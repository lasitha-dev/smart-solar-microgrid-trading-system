const { MongoClient, ObjectId } = require('mongodb');

const uri = "mongodb+srv://sathnarakumarasinghe_db_user:poGE90v3aWflFkmC@solarmicrogrid.eu70nfe.mongodb.net";
const client = new MongoClient(uri);

async function run() {
  try {
    await client.connect();
    const db = client.db("SmartSolarMicrogridDb");
    const slots = db.collection("EnergyBookingSlots");
    
    // Clear old ones
    await slots.deleteMany({});
    
    // Seed new ones for today
    const stationId = new ObjectId("60d5ec49f1b2c42d8c3b4a59");
    const today = new Date();
    today.setUTCHours(0, 0, 0, 0); // Start of today UTC
    
    const tomorrow = new Date(today);
    tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
    
    const docs = [
        {
            StationId: stationId,
            SlotDate: today,
            StartTime: "08:00:00",
            EndTime: "09:00:00",
            BatterySlotId: "Bay-1",
            Status: "Open",
            CreatedAt: new Date(),
            UpdatedAt: new Date()
        },
        {
            StationId: stationId,
            SlotDate: tomorrow,
            StartTime: "10:00:00",
            EndTime: "11:00:00",
            BatterySlotId: "Bay-2",
            Status: "Open",
            CreatedAt: new Date(),
            UpdatedAt: new Date()
        }
    ];
    
    // Actually, let's look at how C# TimeSpan is serialized in MongoDB. Typically as string "08:00:00".
    // And StationId in C# is:
    // [BsonRepresentation(BsonType.ObjectId)] public string StationId { get; set; } = null!;
    // This means in MongoDB it MUST be an ObjectId!
    docs[0].StationId = stationId;
    docs[1].StationId = stationId;
    
    await slots.insertMany(docs);
    console.log("Seeded 2 slots for today.");
  } finally {
    await client.close();
  }
}
run().catch(console.dir);
