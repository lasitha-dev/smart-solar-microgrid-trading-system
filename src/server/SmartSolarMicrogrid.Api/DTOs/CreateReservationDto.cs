/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for creating a new energy reservation.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs
{
    public class CreateReservationDto
    {
        [Required]
        public string ProsumerId { get; set; } = null!;

        [Required]
        public string StationId { get; set; } = null!;

        [Required]
        public string BookingSlotId { get; set; } = null!;

        [Required]
        public DateTime ScheduledDateTime { get; set; }
    }
}
