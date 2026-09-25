/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Interface for the business rule service handling reservations.
 */

using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Services
{
    public interface IReservationService
    {
        Task<EnergyReservation> CreateReservationAsync(CreateReservationDto dto);
        Task<EnergyReservation> UpdateReservationAsync(string id, UpdateReservationDto dto);
        Task CancelReservationAsync(string id, string? reason);
        Task ApproveReservationAsync(string id, string operatorId);
        Task<EnergyReservation?> GetReservationByIdAsync(string id);
        Task<IEnumerable<EnergyReservation>> GetProsumerReservationsAsync(string prosumerId, string? status = null);
        Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date);
    }
}
