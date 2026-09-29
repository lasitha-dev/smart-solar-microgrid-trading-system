/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: DTO for updating an existing energy reservation.
 */

using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.Api.DTOs
{
    public class UpdateReservationDto
    {
        [Required]
        public string BookingSlotId { get; set; } = null!;

        [Required]
        public DateTime ScheduledDateTime { get; set; }
    }
}
