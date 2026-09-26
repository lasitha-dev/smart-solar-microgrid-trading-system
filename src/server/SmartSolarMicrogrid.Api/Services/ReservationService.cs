/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Service layer implementing 7-day and 12-hour business rules.
 */

using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Exceptions;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Repositories;

namespace SmartSolarMicrogrid.Api.Services
{
    public class ReservationService : IReservationService
    {
        private readonly IReservationRepository _repository;
        private readonly IQrSignatureService? _qrSignatureService;

        public ReservationService(IReservationRepository repository, IQrSignatureService? qrSignatureService = null)
        {
            _repository = repository;
            _qrSignatureService = qrSignatureService;
        }

        public async Task<EnergyReservation> CreateReservationAsync(CreateReservationDto dto)
        {
            // Rule 1: Scheduled date must be within 7 days
            var now = DateTime.UtcNow;
            if (dto.ScheduledDateTime.Date < now.Date || dto.ScheduledDateTime.Date > now.Date.AddDays(7))
            {
                throw new BusinessRuleException("RESERVATION_WINDOW_INVALID", "Reservation must be within 7 days from today.");
            }

            // Rule 2: Slot must exist and be Open
            var slot = await _repository.GetSlotByIdAsync(dto.BookingSlotId);
            if (slot == null)
            {
                throw new NotFoundException("Booking slot not found.");
            }
            
            if (slot.Status != "Open")
            {
                throw new BusinessRuleException("SLOT_UNAVAILABLE", $"Slot is currently {slot.Status}.");
            }

            // Atomically reserve the slot to prevent race conditions / double bookings
            var reserved = await _repository.TryReserveSlotAsync(dto.BookingSlotId);
            if (!reserved)
            {
                throw new BusinessRuleException("SLOT_UNAVAILABLE", "Slot was just taken by another user. Please select another slot.");
            }

            // Lookup station to assign operator and station name
            var station = await _repository.GetStationByIdAsync(dto.StationId);

            // Create reservation
            var reservation = new EnergyReservation
            {
                ProsumerId = dto.ProsumerId,
                ProsumerNic = dto.ProsumerId,
                StationId = dto.StationId,
                StationName = station?.StationName ?? dto.StationId,
                AssignedOperatorId = station?.AssignedOperatorId,
                AssignedOperatorNic = station?.AssignedOperatorNic,
                BookingSlotId = dto.BookingSlotId,
                AllocatedBayId = !string.IsNullOrWhiteSpace(slot.BatterySlotId) ? slot.BatterySlotId : "Bay-01",
                ScheduledDateTime = dto.ScheduledDateTime,
                Status = "Pending",
                RequestedAt = now,
                UpdatedAt = now
            };

            try
            {
                await _repository.CreateAsync(reservation);
                return reservation;
            }
            catch
            {
                // Revert reserved slot status to Open if reservation creation fails
                await _repository.UpdateSlotStatusAsync(dto.BookingSlotId, "Open");
                throw;
            }
        }

        public async Task<EnergyReservation> UpdateReservationAsync(string id, UpdateReservationDto dto)
        {
            var reservation = await _repository.GetByIdAsync(id);
            if (reservation == null)
            {
                throw new NotFoundException("Reservation not found.");
            }

            var now = DateTime.UtcNow;

            // Rule 3: Updates require at least 12 hours before scheduled time
            var timeUntilScheduled = reservation.ScheduledDateTime - now;
            if (timeUntilScheduled.TotalHours < 12)
            {
                throw new BusinessRuleException("MODIFICATION_WINDOW_CLOSED", "Modifications are not allowed within 12 hours of the scheduled time.");
            }

            // Enforce 7-day rule on new time
            if (dto.ScheduledDateTime.Date < now.Date || dto.ScheduledDateTime.Date > now.Date.AddDays(7))
            {
                throw new BusinessRuleException("RESERVATION_WINDOW_INVALID", "New reservation date must be within 7 days from today.");
            }

            // If date changed, automatically find an Open slot on the new date and assign it
            if (reservation.ScheduledDateTime.Date != dto.ScheduledDateTime.Date)
            {
                var newDateSlots = await _repository.GetAvailableSlotsAsync(reservation.StationId, dto.ScheduledDateTime);
                var openSlot = newDateSlots.FirstOrDefault(s => s.Status == "Open");
                
                if (openSlot != null)
                {
                    // Release old slot
                    await _repository.UpdateSlotStatusAsync(reservation.BookingSlotId, "Open");
                    
                    // Reserve new slot
                    await _repository.UpdateSlotStatusAsync(openSlot.Id!, "Reserved");
                    
                    // Update reservation references
                    reservation.BookingSlotId = openSlot.Id!;
                }
                else
                {
                    throw new BusinessRuleException("SLOT_UNAVAILABLE", "No open slots available on the newly selected date.");
                }
            }
            else if (reservation.BookingSlotId != dto.BookingSlotId)
            {
                var newSlot = await _repository.GetSlotByIdAsync(dto.BookingSlotId);
                if (newSlot == null || newSlot.Status != "Open")
                {
                    throw new BusinessRuleException("SLOT_UNAVAILABLE", "The new slot is not available.");
                }

                await _repository.UpdateSlotStatusAsync(reservation.BookingSlotId, "Open");
                await _repository.UpdateSlotStatusAsync(dto.BookingSlotId, "Reserved");
                
                reservation.BookingSlotId = dto.BookingSlotId;
            }

            reservation.ScheduledDateTime = dto.ScheduledDateTime;
            reservation.UpdatedAt = now;

            await _repository.UpdateAsync(reservation);

            return reservation;
        }

        public async Task CancelReservationAsync(string id, string? reason)
        {
            var reservation = await _repository.GetByIdAsync(id);
            if (reservation == null)
            {
                throw new NotFoundException("Reservation not found.");
            }

            var now = DateTime.UtcNow;

            // Rule 4: Cancellations require at least 12 hours before scheduled time
            var timeUntilScheduled = reservation.ScheduledDateTime - now;
            if (timeUntilScheduled.TotalHours < 12)
            {
                throw new BusinessRuleException("MODIFICATION_WINDOW_CLOSED", "Cancellations are not allowed within 12 hours of the scheduled time.");
            }

            reservation.Status = "Cancelled";
            reservation.CancelReason = reason;
            reservation.CancelledAt = now;
            reservation.UpdatedAt = now;

            await _repository.UpdateAsync(reservation);

            // Release slot back to Open
            await _repository.UpdateSlotStatusAsync(reservation.BookingSlotId, "Open");
        }

        public async Task<EnergyReservation> ApproveReservationAsync(string id, string operatorId)
        {
            var reservation = await _repository.GetByIdAsync(id);
            if (reservation == null)
            {
                throw new NotFoundException("Reservation not found.");
            }

            // If already approved, return idempotently with existing QR code
            if (string.Equals(reservation.Status, "Approved", StringComparison.OrdinalIgnoreCase))
            {
                return reservation;
            }

            if (!string.Equals(reservation.Status, "Pending", StringComparison.OrdinalIgnoreCase))
            {
                throw new BusinessRuleException("INVALID_STATE", "Only Pending reservations can be approved.");
            }

            reservation.Status = "Approved";
            reservation.UpdatedAt = DateTime.UtcNow;
            
            // Generate cryptographically signed QR payload matching Member 4 specification
            if (_qrSignatureService != null)
            {
                var prosumerNic = !string.IsNullOrWhiteSpace(reservation.ProsumerNic) ? reservation.ProsumerNic : reservation.ProsumerId;
                reservation.QrCode = _qrSignatureService.GenerateFullPayload(
                    reservation.Id!,
                    prosumerNic,
                    reservation.StationId,
                    reservation.ScheduledDateTime
                );
            }
            else
            {
                reservation.QrCode = $"SSMTS-QR|{reservation.Id}|{reservation.ProsumerId}|{reservation.StationId}|{reservation.ScheduledDateTime:yyyy-MM-ddTHH:mm:ssZ}|default_sig";
            }

            await _repository.UpdateAsync(reservation);
            return reservation;
        }

        public async Task<EnergyReservation?> GetReservationByIdAsync(string id)
        {
            return await _repository.GetByIdAsync(id);
        }

        public async Task<IEnumerable<EnergyReservation>> GetProsumerReservationsAsync(string prosumerId, string? status = null)
        {
            return await _repository.GetAllAsync(prosumerId, status);
        }

        public async Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date)
        {
            return await _repository.GetAvailableSlotsAsync(stationId, date);
        }
    }
}
