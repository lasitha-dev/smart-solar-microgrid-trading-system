/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: MongoDB implementation of the reservation repository.
 */

using MongoDB.Driver;
using SmartSolarMicrogrid.Api.Models;

namespace SmartSolarMicrogrid.Api.Repositories
{
    public class ReservationRepository : IReservationRepository
    {
        private readonly IMongoCollection<EnergyReservation> _reservations;
        private readonly IMongoCollection<EnergyBookingSlot> _slots;

        public ReservationRepository(IMongoDatabase database)
        {
            _reservations = database.GetCollection<EnergyReservation>("EnergyReservations");
            _slots = database.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");
        }

        public async Task<EnergyReservation?> GetByIdAsync(string id)
        {
            return await _reservations.Find(r => r.Id == id).FirstOrDefaultAsync();
        }

        public async Task<IEnumerable<EnergyReservation>> GetAllAsync(string? prosumerId = null, string? status = null)
        {
            var filterBuilder = Builders<EnergyReservation>.Filter;
            var filter = filterBuilder.Empty;

            if (!string.IsNullOrEmpty(prosumerId))
            {
                filter &= filterBuilder.Eq(r => r.ProsumerId, prosumerId);
            }

            if (!string.IsNullOrEmpty(status))
            {
                filter &= filterBuilder.Eq(r => r.Status, status);
            }

            return await _reservations.Find(filter).ToListAsync();
        }

        public async Task CreateAsync(EnergyReservation reservation)
        {
            await _reservations.InsertOneAsync(reservation);
        }

        public async Task UpdateAsync(EnergyReservation reservation)
        {
            await _reservations.ReplaceOneAsync(r => r.Id == reservation.Id, reservation);
        }

        public async Task DeleteAsync(string id)
        {
            await _reservations.DeleteOneAsync(r => r.Id == id);
        }

        public async Task<EnergyBookingSlot?> GetSlotByIdAsync(string slotId)
        {
            return await _slots.Find(s => s.Id == slotId).FirstOrDefaultAsync();
        }

        public async Task<IEnumerable<EnergyBookingSlot>> GetAvailableSlotsAsync(string stationId, DateTime date)
        {
            // Ensure the date being queried is treated as UTC to match MongoDB storage
            var utcDate = DateTime.SpecifyKind(date.Date, DateTimeKind.Utc);

            // Return all slots for a given station on a specific date (Open, Reserved, etc.)
            // The Android app will use the Status to grey out unavailable ones.
            var filter = Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId) &
                         Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotDate, utcDate);

            return await _slots.Find(filter).ToListAsync();
        }

        public async Task UpdateSlotStatusAsync(string slotId, string status)
        {
            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.Status, status)
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            await _slots.UpdateOneAsync(s => s.Id == slotId, update);
        }

        public async Task<bool> TryReserveSlotAsync(string slotId)
        {
            var filter = Builders<EnergyBookingSlot>.Filter.And(
                Builders<EnergyBookingSlot>.Filter.Eq(s => s.Id, slotId),
                Builders<EnergyBookingSlot>.Filter.Eq(s => s.Status, "Open")
            );
            var update = Builders<EnergyBookingSlot>.Update
                .Set(s => s.Status, "Reserved")
                .Set(s => s.UpdatedAt, DateTime.UtcNow);

            var result = await _slots.FindOneAndUpdateAsync(filter, update);
            return result != null;
        }

        public async Task SeedSlotsAsync(string stationId)
        {
            var today = DateTime.UtcNow.Date;
            var slots = new List<EnergyBookingSlot>();

            for (int i = 0; i <= 10; i++)
            {
                var date = today.AddDays(i);

                slots.Add(new EnergyBookingSlot
                {
                    StationId = stationId,
                    SlotDate = date,
                    StartTime = new TimeSpan(8, 0, 0),
                    EndTime = new TimeSpan(9, 0, 0),
                    BatterySlotId = "Bay-1",
                    Status = "Open",
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                });

                slots.Add(new EnergyBookingSlot
                {
                    StationId = stationId,
                    SlotDate = date,
                    StartTime = new TimeSpan(10, 0, 0),
                    EndTime = new TimeSpan(11, 0, 0),
                    BatterySlotId = "Bay-2",
                    Status = "Open",
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                });

                slots.Add(new EnergyBookingSlot
                {
                    StationId = stationId,
                    SlotDate = date,
                    StartTime = new TimeSpan(14, 0, 0),
                    EndTime = new TimeSpan(15, 0, 0),
                    BatterySlotId = "Bay-3",
                    Status = "Open",
                    CreatedAt = DateTime.UtcNow,
                    UpdatedAt = DateTime.UtcNow
                });
            }

            await _slots.DeleteManyAsync(Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, stationId));
            await _slots.InsertManyAsync(slots);
        }
    }
}
