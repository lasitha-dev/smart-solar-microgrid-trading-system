const { MongoClient, ObjectId } = require('mongodb');

const uri = "mongodb+srv://sathnarakumarasinghe_db_user:poGE90v3aWflFkmC@solarmicrogrid.eu70nfe.mongodb.net";
const client = new MongoClient(uri);

async function run() {
  try {
    await client.connect();
    const db = client.db("SmartSolarMicrogridDb");
    const slots = db.collection("EnergyBookingSlots");
    
    const all = await slots.find({}).toArray();
    console.log(JSON.stringify(all, null, 2));
  } finally {
    await client.close();
  }
}
run().catch(console.dir);
