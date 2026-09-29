/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for representing available slots to clients.
 */

namespace SmartSolarMicrogrid.Api.DTOs
{
    public class AvailableSlotDto
    {
        public string Id { get; set; } = null!;
        public string StationId { get; set; } = null!;
        public DateTime SlotDate { get; set; }
        public TimeSpan StartTime { get; set; }
        public TimeSpan EndTime { get; set; }
        public string BatterySlotId { get; set; } = null!;
        public string Status { get; set; } = null!;
    }
}
