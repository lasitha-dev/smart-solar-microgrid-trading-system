/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for returning reservation details to clients.
 */

namespace SmartSolarMicrogrid.Api.DTOs
{
    public class ReservationResponseDto
    {
        public string Id { get; set; } = null!;
        public string ProsumerId { get; set; } = null!;
        public string StationId { get; set; } = null!;
        public string BookingSlotId { get; set; } = null!;
        public DateTime ScheduledDateTime { get; set; }
        public string Status { get; set; } = null!;
        public string? QrCode { get; set; }
        public DateTime RequestedAt { get; set; }
        public DateTime UpdatedAt { get; set; }
        public DateTime? CancelledAt { get; set; }
        public string? CancelReason { get; set; }
        public string? FinalizedBy { get; set; }
        public DateTime? FinalizedAt { get; set; }
    }
}
