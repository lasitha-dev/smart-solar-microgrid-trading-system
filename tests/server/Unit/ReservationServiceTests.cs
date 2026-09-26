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
            _mockRepo.Verify(r => r.UpdateSlotStatusAsync(It.IsAny<string>(), "Open"), Times.Once);
        }

        [Fact]
        public async Task ApproveReservation_MarksStationBayUnavailable()
        {
            // Arrange
            var reservationId = "RES-APP-1";
            var reservation = new EnergyReservation
            {
                Id = reservationId,
                StationId = "STAT-1",
                AllocatedBayId = "BAY-01",
                BookingSlotId = "SLOT-1",
                Status = "Pending",
                ScheduledDateTime = DateTime.UtcNow.AddDays(2)
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId)).ReturnsAsync(reservation);
            _mockRepo.Setup(r => r.UpdateAsync(It.IsAny<EnergyReservation>())).Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-01", false)).Returns(Task.CompletedTask);

            // Act
            var result = await _service.ApproveReservationAsync(reservationId, "OP-01");

            // Assert
            Assert.Equal("Approved", result.Status);
            _mockRepo.Verify(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-01", false), Times.Once);
        }

        [Fact]
        public async Task CancelReservation_ReleasesStationBayAndSlot()
        {
            // Arrange
            var reservationId = "RES-CAN-1";
            var reservation = new EnergyReservation
            {
                Id = reservationId,
                StationId = "STAT-1",
                AllocatedBayId = "BAY-02",
                BookingSlotId = "SLOT-2",
                Status = "Pending",
                ScheduledDateTime = DateTime.UtcNow.AddHours(20)
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId)).ReturnsAsync(reservation);
            _mockRepo.Setup(r => r.UpdateAsync(It.IsAny<EnergyReservation>())).Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateSlotStatusAsync("SLOT-2", "Open")).Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-02", true)).Returns(Task.CompletedTask);

            // Act
            await _service.CancelReservationAsync(reservationId, "Customer cancelled");

            // Assert
            _mockRepo.Verify(r => r.UpdateSlotStatusAsync("SLOT-2", "Open"), Times.Once);
            _mockRepo.Verify(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-02", true), Times.Once);
        }

        [Fact]
        public async Task RejectReservation_CancelsAndReleasesSlotAndBay()
        {
            // Arrange
            var reservationId = "RES-REJ-1";
            var reservation = new EnergyReservation
            {
                Id = reservationId,
                StationId = "STAT-1",
                AllocatedBayId = "BAY-03",
                BookingSlotId = "SLOT-3",
                Status = "Pending",
                ScheduledDateTime = DateTime.UtcNow.AddDays(1)
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId)).ReturnsAsync(reservation);
            _mockRepo.Setup(r => r.UpdateAsync(It.IsAny<EnergyReservation>())).Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateSlotStatusAsync("SLOT-3", "Open")).Returns(Task.CompletedTask);
            _mockRepo.Setup(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-03", true)).Returns(Task.CompletedTask);

            // Act
            var result = await _service.RejectReservationAsync(reservationId, "Station maintenance scheduled", "OP-01");

            // Assert
            Assert.Equal("Cancelled", result.Status);
            Assert.Equal("Station maintenance scheduled", result.CancelReason);
            _mockRepo.Verify(r => r.UpdateSlotStatusAsync("SLOT-3", "Open"), Times.Once);
            _mockRepo.Verify(r => r.UpdateStationBayAvailabilityAsync("STAT-1", "BAY-03", true), Times.Once);
        }

        [Fact]
        public async Task RejectReservation_Throws_WhenCompleted()
        {
            // Arrange
            var reservationId = "RES-COMP-1";
            var reservation = new EnergyReservation
            {
                Id = reservationId,
                Status = "Completed"
            };

            _mockRepo.Setup(r => r.GetByIdAsync(reservationId)).ReturnsAsync(reservation);

            // Act & Assert
            var ex = await Assert.ThrowsAsync<BusinessRuleException>(() => 
                _service.RejectReservationAsync(reservationId, "Cannot reject", "OP-01"));
            Assert.Equal("INVALID_STATE", ex.Code);
        }
    }
}
