/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Data model representing a Prosumer's energy trading reservation.
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.Api.Models
{
    public class EnergyReservation
    {
        [BsonId]
        [BsonRepresentation(BsonType.ObjectId)]
        public string? Id { get; set; }

        public string ProsumerId { get; set; } = null!;

        [BsonRepresentation(BsonType.ObjectId)]
        public string StationId { get; set; } = null!;

        [BsonRepresentation(BsonType.ObjectId)]
        public string BookingSlotId { get; set; } = null!;

        [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
        public DateTime ScheduledDateTime { get; set; }

        public string Status { get; set; } = "Pending"; // Pending, Approved, Cancelled, Completed

        public string? QrCode { get; set; }

        [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
        public DateTime RequestedAt { get; set; } = DateTime.UtcNow;

        [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

        [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
        public DateTime? CancelledAt { get; set; }

        public string? CancelReason { get; set; }

        [BsonRepresentation(BsonType.ObjectId)]
        public string? FinalizedBy { get; set; }

        [BsonDateTimeOptions(Kind = DateTimeKind.Utc)]
        public DateTime? FinalizedAt { get; set; }
    }
}
