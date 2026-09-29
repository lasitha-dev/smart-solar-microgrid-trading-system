const { MongoClient, ObjectId } = require('mongodb');

const uri = "mongodb+srv://sathnarakumarasinghe_db_user:poGE90v3aWflFkmC@solarmicrogrid.eu70nfe.mongodb.net";
const client = new MongoClient(uri);

async function run() {
  try {
    await client.connect();
    const db = client.db("SmartSolarMicrogridDb");
    const slotsCollection = db.collection("EnergyBookingSlots");
    
    // Clear old slots
    await slotsCollection.deleteMany({});
    
    const stationA = new ObjectId("60d5ec49f1b2c42d8c3b4a59");
    const stationB = new ObjectId("60d5ec49f1b2c42d8c3b4a60");
    const stations = [stationA, stationB];

    const slotTimes = [
      { start: "08:00:00", end: "09:00:00", bay: "Bay-1" },
      { start: "10:00:00", end: "11:00:00", bay: "Bay-2" },
      { start: "13:00:00", end: "14:00:00", bay: "Bay-3" },
      { start: "15:00:00", end: "16:00:00", bay: "Bay-4" }
    ];

    const docs = [];
    const now = new Date();

    // Generate slots for today and the next 7 days (0 to 7)
    for (let dayOffset = 0; dayOffset <= 7; dayOffset++) {
      const slotDate = new Date();
      slotDate.setUTCDate(slotDate.getUTCDate() + dayOffset);
      slotDate.setUTCHours(0, 0, 0, 0);

      for (const st of stations) {
        for (const time of slotTimes) {
          docs.push({
            StationId: st,
            SlotDate: slotDate,
            StartTime: time.start,
            EndTime: time.end,
            BatterySlotId: time.bay,
            Status: "Open",
            CreatedAt: now,
            UpdatedAt: now
          });
        }
      }
    }
    
    await slotsCollection.insertMany(docs);
    console.log(`Successfully seeded ${docs.length} energy slots across Station A and Station B for the next 7 days!`);
  } finally {
    await client.close();
  }
}

run().catch(console.error);
