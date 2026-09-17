/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Interface for the MongoDB repository handling energy reservations and slots.
 */

using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories
{
    public interface IReservationRepository
    {
        Task<EnergyReservation?> GetByIdAsync(string id);
        Task<IEnumerable<EnergyReservation>> GetAllAsync(string? prosumerId = null, string? status = null);
        Task CreateAsync(EnergyReservation reservation);
        Task UpdateAsync(EnergyReservation reservation);
        Task DeleteAsync(string id);
        Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId);
        Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date);
        Task UpdateSlotStatusAsync(string slotId, string status);
    }
}
