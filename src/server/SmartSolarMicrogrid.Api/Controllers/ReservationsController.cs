/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: REST API Controller for managing energy reservations.
 */

using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Exceptions;
using SmartSolarMicrogrid.Api.Helpers;
using SmartSolarMicrogrid.Api.Services;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.Repositories;
using MongoDB.Driver;

namespace SmartSolarMicrogrid.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class ReservationsController : ControllerBase
    {
        private readonly IReservationService _reservationService;
        private readonly IReservationRepository _repository;

        public ReservationsController(IReservationService reservationService, IReservationRepository repository)
        {
            _reservationService = reservationService;
            _repository = repository;
        }

        [HttpGet("slots")]
        public async Task<IActionResult> GetAvailableSlots([FromQuery] string stationId, [FromQuery] DateTime date)
        {
            var slots = await _reservationService.GetAvailableSlotsAsync(stationId, date);
            var dtos = slots.Select(s => new AvailableSlotDto
            {
                Id = s.Id!,
                StationId = s.StationId,
                SlotDate = s.SlotDate,
                StartTime = s.StartTime,
                EndTime = s.EndTime,
                BatterySlotId = s.BatterySlotId,
                Status = s.Status
            });
            return Ok(ApiResponse<IEnumerable<AvailableSlotDto>>.SuccessResponse(dtos));
        }

        [HttpPost]
        public async Task<IActionResult> CreateReservation([FromBody] CreateReservationDto dto)
        {
            try
            {
                var reservation = await _reservationService.CreateReservationAsync(dto);
                return CreatedAtAction(nameof(GetReservation), new { id = reservation.Id }, 
                    ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation), "Reservation created successfully"));
            }
            catch (BusinessRuleException ex)
            {
                return BadRequest(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
            }
            catch (NotFoundException ex)
            {
                return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
            }
        }

        [HttpGet("{id}")]
        public async Task<IActionResult> GetReservation(string id)
        {
            var reservation = await _reservationService.GetReservationByIdAsync(id);
            if (reservation == null)
            {
                return NotFound(ApiResponse<object>.ErrorResponse("Reservation not found", "NOT_FOUND"));
            }

            return Ok(ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation)));
        }

        [HttpGet]
        public async Task<IActionResult> GetReservations([FromQuery] string? prosumerId, [FromQuery] string? status)
        {
            var reservations = await _reservationService.GetProsumerReservationsAsync(prosumerId ?? string.Empty, status);
            var dtos = reservations.Select(MapToDto);
            return Ok(ApiResponse<IEnumerable<ReservationResponseDto>>.SuccessResponse(dtos));
        }

        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateReservation(string id, [FromBody] UpdateReservationDto dto)
        {
            try
            {
                var reservation = await _reservationService.UpdateReservationAsync(id, dto);
                return Ok(ApiResponse<ReservationResponseDto>.SuccessResponse(MapToDto(reservation), "Reservation updated successfully"));
            }
            catch (BusinessRuleException ex)
            {
                return Conflict(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
            }
            catch (NotFoundException ex)
            {
                return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
            }
        }

        [HttpDelete("{id}")]
        public async Task<IActionResult> CancelReservation(string id, [FromQuery] string? reason)
        {
            try
            {
                await _reservationService.CancelReservationAsync(id, reason);
                return Ok(ApiResponse<object>.SuccessResponse(new {}, "Reservation cancelled successfully"));
            }
            catch (BusinessRuleException ex)
            {
                return Conflict(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
            }
            catch (NotFoundException ex)
            {
                return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
            }
        }

        [HttpPatch("{id}/approve")]
        public async Task<IActionResult> ApproveReservation(string id, [FromQuery] string operatorId)
        {
            try
            {
                await _reservationService.ApproveReservationAsync(id, operatorId);
                return Ok(ApiResponse<object>.SuccessResponse(new {}, "Reservation approved successfully"));
            }
            catch (BusinessRuleException ex)
            {
                return BadRequest(ApiResponse<object>.ErrorResponse(ex.Message, ex.Code));
            }
            catch (NotFoundException ex)
            {
                return NotFound(ApiResponse<object>.ErrorResponse(ex.Message, "NOT_FOUND"));
            }
        }

        [HttpPost("seed")]
        public async Task<IActionResult> SeedSlots()
        {
            var stationId = "60d5ec49f1b2c42d8c3b4a59"; // Dummy Station A
            await _repository.SeedSlotsAsync(stationId);
            return Ok(ApiResponse<object>.SuccessResponse(new { stationId, count = 33 }, "Seeded 3 slots per day for next 10 days"));
        }

        private static ReservationResponseDto MapToDto(Models.EnergyReservation r)
        {
            return new ReservationResponseDto
            {
                Id = r.Id!,
                ProsumerId = r.ProsumerId,
                StationId = r.StationId,
                BookingSlotId = r.BookingSlotId,
                ScheduledDateTime = r.ScheduledDateTime,
                Status = r.Status,
                QrCode = r.QrCode,
                RequestedAt = r.RequestedAt,
                UpdatedAt = r.UpdatedAt,
                CancelledAt = r.CancelledAt,
                CancelReason = r.CancelReason,
                FinalizedBy = r.FinalizedBy,
                FinalizedAt = r.FinalizedAt
            };
        }
    }
}
