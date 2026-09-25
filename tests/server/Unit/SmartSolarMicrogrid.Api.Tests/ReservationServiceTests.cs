/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Unit tests for ReservationService business rules (7-day rule, 12-hour rule, slot availability).
 */

using System;
using System.Threading.Tasks;
using Moq;
using Xunit;
using SmartSolarMicrogrid.Api.Services;
using SmartSolarMicrogrid.Api.Repositories;
using SmartSolarMicrogrid.Api.Models;
using SmartSolarMicrogrid.Api.DTOs;
using SmartSolarMicrogrid.Api.Exceptions;

namespace SmartSolarMicrogrid.Api.Tests
{
    public class ReservationServiceTests
    {
        private readonly Mock<IReservationRepository> _mockRepo;
        private readonly ReservationService _service;

        public ReservationServiceTests()
        {
            _mockRepo = new Mock<IReservationRepository>();
            _service = new ReservationService(_mockRepo.Object);
        }

        [Fact]
        public async Task CreateReservation_Fails_WhenDateIsMoreThan7DaysAhead()
        {
            // Arrange
            var invalidDate = DateTime.UtcNow.AddDays(8);
            var dto = new CreateReservationDto
            {
                ProsumerId = "P1",
                StationId = "S1",
                BookingSlotId = "B1",
                ScheduledDateTime = invalidDate
            };

            // Act & Assert
            var ex = await Assert.ThrowsAsync<BusinessRuleException>(() => _service.CreateReservationAsync(dto));
            Assert.Contains("within 7 days", ex.Message);
        }

        [Fact]
        public async Task CreateReservation_Succeeds_WhenDateIsWithin7Days()
        {
            // Arrange
            var validDate = DateTime.UtcNow.AddDays(3);
            var dto = new CreateReservationDto
            {
                ProsumerId = "P1",
                StationId = "S1",
                BookingSlotId = "B1",
                ScheduledDateTime = validDate
            };

            var slot = new EnergyBookingSlot
            {
                Id = "B1",
                StationId = "S1",
                Status = "Open"
            };

            _mockRepo.Setup(r => r.GetSlotByIdAsync("B1")).ReturnsAsync(slot);
            _mockRepo.Setup(r => r.TryReserveSlotAsync("B1")).ReturnsAsync(true);
            _mockRepo.Setup(r => r.CreateAsync(It.IsAny<EnergyReservation>()))
                     .Returns(Task.CompletedTask);

            // Act
            var result = await _service.CreateReservationAsync(dto);

            // Assert
            Assert.NotNull(result);
            Assert.Equal("Pending", result.Status);
            _mockRepo.Verify(r => r.CreateAsync(It.IsAny<EnergyReservation>()), Times.Once);
        }

        [Fact]
        public async Task CreateReservation_Fails_WhenSlotIsAlreadyReserved()
        {
            // Arrange
            var validDate = DateTime.UtcNow.AddDays(2);
            var dto = new CreateReservationDto
            {
                ProsumerId = "P1",
                StationId = "S1",
                BookingSlotId = "B1",
                ScheduledDateTime = validDate
            };

            var slot = new EnergyBookingSlot
            {
                Id = "B1",
                StationId = "S1",
                Status = "Reserved" // Already reserved
            };

            _mockRepo.Setup(r => r.GetSlotByIdAsync("B1")).ReturnsAsync(slot);

            // Act & Assert
            var ex = await Assert.ThrowsAsync<BusinessRuleException>(() => _service.CreateReservationAsync(dto));
            Assert.Equal("SLOT_UNAVAILABLE", ex.Code);
        }

        [Fact]
        public async Task CancelReservation_Fails_WhenWithin12HoursOfScheduledTime()
        {
            // Arrange
            var reservationId = "RES-123";
            var scheduledTime = DateTime.UtcNow.AddHours(5); // Less than 12 hours
            
            var existingReservation = new EnergyReservation
            {
                Id = reservationId,
                ScheduledDateTime = scheduledTime,
                Status = "Pending"
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId))
                     .ReturnsAsync(existingReservation);

            // Act & Assert
            var ex = await Assert.ThrowsAsync<BusinessRuleException>(() => _service.CancelReservationAsync(reservationId, "No longer needed"));
            Assert.Contains("12 hours", ex.Message);
        }

        [Fact]
        public async Task CancelReservation_Succeeds_WhenMoreThan12HoursRemaining()
        {
            // Arrange
            var reservationId = "RES-123";
            var scheduledTime = DateTime.UtcNow.AddHours(24); // More than 12 hours
            
            var existingReservation = new EnergyReservation
            {
                Id = reservationId,
                ScheduledDateTime = scheduledTime,
                Status = "Pending"
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId))
                     .ReturnsAsync(existingReservation);
            _mockRepo.Setup(r => r.UpdateAsync(It.IsAny<EnergyReservation>()))
                     .Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateSlotStatusAsync(It.IsAny<string>(), It.IsAny<string>()))
                     .Returns(Task.CompletedTask);

            // Act
            await _service.CancelReservationAsync(reservationId, "Change of plans");

            // Assert
            _mockRepo.Verify(r => r.UpdateAsync(It.Is<EnergyReservation>(e => e.Status == "Cancelled" && e.CancelReason == "Change of plans")), Times.Once);
        }
    }
}
