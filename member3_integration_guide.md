# 🔌 Member 3 – Integration Guide
### Reservation Workflow Integration into Smart Solar Microgrid Trading System
**Author:** Kumarasinghe S.S | IT22221414  
**Branch:** `feature/member-3-reservation-workflow`  
**Module:** Energy Reservation Workflow (SE4040 EAD 2026)

---

## 📋 Table of Contents
1. [Overview](#overview)
2. [What Member 3 Provides](#what-member-3-provides)
3. [Dependencies on Other Members](#dependencies-on-other-members)
4. [Step-by-Step Integration Guide](#step-by-step-integration-guide)
5. [API Contract Reference](#api-contract-reference)
6. [Database Schema Reference](#database-schema-reference)
7. [Business Rules (Do Not Break!)](#business-rules)
8. [Station ID Replacement Guide](#station-id-replacement-guide)
9. [Testing After Integration](#testing-after-integration)
10. [Common Integration Issues](#common-integration-issues)

---

## Overview

Member 3 built the **Energy Reservation Workflow** — the full flow for Prosumers to:
- Browse available energy booking slots by Station & Date
- Book a slot (with 7-day window constraint)
- View/Cancel bookings (with 12-hour lockout rule)
- View Booking History with QR codes

This module spans **3 layers**: C# Backend API, Android Mobile App, and React Web Portal.

---

## What Member 3 Provides

### 🖥️ Backend (C# ASP.NET Core API)
**Location:** `src/server/SmartSolarMicrogrid.Api/`

| File | Purpose |
|---|---|
| `Controllers/ReservationsController.cs` | REST API endpoints for reservations |
| `Services/ReservationService.cs` | Business logic + validation rules |
| `Repositories/ReservationRepository.cs` | MongoDB data access layer |
| `Models/EnergyReservation.cs` | Reservation data model |
| `Models/EnergyBookingSlot.cs` | Slot data model |
| `DTOs/CreateReservationDto.cs` | Request DTO for creating a booking |
| `DTOs/ReservationResponseDto.cs` | Response DTO returned to clients |

### 📱 Mobile (Android Kotlin)
**Location:** `src/mobile-android/SmartSolarMicrogridAndroid/`

| File | Purpose |
|---|---|
| `ui/booking/SlotSelectionActivity.kt` | Slot browsing screen |
| `ui/booking/BookingSummaryActivity.kt` | Booking receipt + QR Code screen |
| `ui/history/BookingHistoryActivity.kt` | All bookings list screen |
| `data/repository/ReservationRepositoryImpl.kt` | API ↔ Room DB sync |
| `data/local/dao/ReservationDao.kt` | Room DB queries |
| `data/remote/ReservationApi.kt` | Retrofit API interface |

### 🌐 Web (React + Vite)
**Location:** `src/web/smart-solar-microgrid-web/`

| File | Purpose |
|---|---|
| `src/pages/SlotBookingPage.jsx` | Slot booking page |
| `src/pages/ReservationListPage.jsx` | Reservation history page |
| `src/services/reservationService.js` | API call functions |

---

## Dependencies on Other Members

> [!IMPORTANT]
> Member 3's module uses **hardcoded placeholder** Station IDs. These must be replaced with **real Station IDs** from the shared production database during integration.

### From Member 2 (Microgrid Node / Station Management):

| What We Need | Why | Where to Replace |
|---|---|---|
| Real `StationId` (MongoDB ObjectId) for **Station A** | Currently hardcoded as `60d5ec49f1b2c42d8c3b4a59` | See [Station ID Replacement Guide](#station-id-replacement-guide) |
| Real `StationId` for **Station B** | Currently hardcoded as `60d5ec49f1b2c42d8c3b4a60` | See [Station ID Replacement Guide](#station-id-replacement-guide) |
| Station Name (String) for display | Currently shows raw ID in UI | Lookup by ID from Member 2's API |
| Optional: `GET /api/stations` endpoint | To dynamically load station list | Replaces hardcoded list in App & Web |

### From Member 1 (Auth / User Management):
| What We Need | Why | Where to Apply |
|---|---|---|
| JWT Token from login flow | `ProsumerId` must match authenticated user | Pass token in `Authorization: Bearer <token>` header |
| Authenticated `userId` / `prosumerId` | All reservations are scoped per user | Replace placeholder `prosumerId` in booking screens |

---

## Step-by-Step Integration Guide

### STEP 1 – Merge Branches

```bash
# On the integration branch (e.g., main or develop):
git merge feature/member-3-reservation-workflow
```

> [!NOTE]
> If there are conflicts in `Program.cs` or `appsettings.json`, manually keep both members' service registrations. Do NOT remove Member 3's `ReservationService` or `ReservationRepository` DI registrations.

---

### STEP 2 – Update the Shared Database Connection

Member 3 used a **test MongoDB Atlas cluster** during development. For production integration, update the connection string in `appsettings.json`:

```json
// src/server/SmartSolarMicrogrid.Api/appsettings.json
"DatabaseSettings": {
    "ConnectionString": "<SHARED_PRODUCTION_MONGODB_CONNECTION_STRING>",
    "DatabaseName": "SmartSolarMicrogridDb"
}
```

> [!WARNING]
> Do NOT commit production credentials to git. Use environment variables or `appsettings.Production.json` (excluded via `.gitignore`).

---

### STEP 3 – Replace Hardcoded Station IDs

#### 3a. Backend – `seed.js`
```js
// seed.js  (Lines 15-16)
// BEFORE:
const stationA = new ObjectId("60d5ec49f1b2c42d8c3b4a59");
const stationB = new ObjectId("60d5ec49f1b2c42d8c3b4a60");

// AFTER (replace with real IDs from Member 2's station collection):
const stationA = new ObjectId("<REAL_STATION_A_ID_FROM_MEMBER2>");
const stationB = new ObjectId("<REAL_STATION_B_ID_FROM_MEMBER2>");
```

Then re-run the seed script to populate real slots:
```bash
node seed.js
```

#### 3b. Mobile App – `SlotSelectionActivity.kt`
```kotlin
// Find this block in SlotSelectionActivity.kt:
stations = listOf(
    Station("60d5ec49f1b2c42d8c3b4a59", "Station A – Solar Bay (Main Microgrid)"),
    Station("60d5ec49f1b2c42d8c3b4a60", "Station B – North Grid (Substation)")
)

// AFTER (replace IDs with real ones, or fetch dynamically from API):
stations = listOf(
    Station("<REAL_STATION_A_ID>", "Station A – Solar Bay (Main Microgrid)"),
    Station("<REAL_STATION_B_ID>", "Station B – North Grid (Substation)")
)
```

**OR (Preferred):** Fetch dynamically from Member 2's Station API:
```kotlin
// Call GET /api/stations and populate the spinner dynamically
val response = stationApi.getAllStations()
stations = response.data.map { Station(it.id, it.name) }
```

#### 3c. Web Portal – `SlotBookingPage.jsx`
```jsx
// Find the STATIONS constant in SlotBookingPage.jsx:
const STATIONS = [
  { id: '60d5ec49f1b2c42d8c3b4a59', name: 'Station A – Solar Bay' },
  { id: '60d5ec49f1b2c42d8c3b4a60', name: 'Station B – North Grid' },
];

// AFTER:
const STATIONS = [
  { id: '<REAL_STATION_A_ID>', name: 'Station A – Solar Bay' },
  { id: '<REAL_STATION_B_ID>', name: 'Station B – North Grid' },
];
```

---

### STEP 4 – Connect Authentication (Member 1)

Currently, `ProsumerId` is passed as a plain string. After Member 1's Auth is integrated:

#### Mobile App:
```kotlin
// In SlotSelectionActivity.kt, replace:
val prosumerId = "user_demo_001"  // <-- placeholder

// With (read from shared auth session/token):
val prosumerId = AuthSessionManager.getCurrentUserId(this)
// OR from SharedPreferences/DataStore where login stores the user ID
```

#### Web Portal:
```jsx
// In SlotBookingPage.jsx, replace:
prosumerId: "web_user_demo_001"  // <-- placeholder

// With authenticated user's ID from Member 1's auth context:
prosumerId: authContext.user.id
// OR from localStorage/sessionStorage where Member 1's login stores it
```

#### Backend (Optional – add JWT middleware):
If Member 1 implements JWT auth middleware, protect Member 3's endpoints:
```csharp
// In ReservationsController.cs, add [Authorize] attribute:
[ApiController]
[Route("api/[controller]")]
[Authorize]  // <-- Add this line
public class ReservationsController : ControllerBase
```

---

### STEP 5 – Seed Real Slots for Integration Testing

After replacing Station IDs, seed fresh slots into the shared database:
```bash
# From project root:
node seed.js
```

This creates **64 slots** (8 per day × 2 stations × ~8 days) with `Status: "Open"`.

> [!NOTE]
> The seed script **clears all existing slots** first (`deleteMany({})`). Only run it once at integration start to avoid losing real booking data.

---

### STEP 6 – Verify API Endpoints Are Accessible

Run the backend and confirm these endpoints respond correctly:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/reservations/slots?stationId={id}&date={yyyy-MM-dd}` | Fetch available slots |
| `POST` | `/api/reservations` | Create a new reservation |
| `GET` | `/api/reservations?prosumerId={id}` | Get reservations for a user |
| `GET` | `/api/reservations/{id}` | Get single reservation |
| `DELETE` | `/api/reservations/{id}?reason={text}` | Cancel a reservation |
| `PATCH` | `/api/reservations/{id}/approve?operatorId={id}` | Approve reservation (admin) |

Quick test using curl:
```bash
curl http://localhost:5000/api/reservations/slots?stationId=<REAL_ID>&date=2026-09-26
```

---

### STEP 7 – Mobile App – ADB Reverse (For Local Testing)

If testing Mobile App against a locally-running API:
```bash
adb reverse tcp:5000 tcp:5000
```

For production, update `BASE_URL` in `RetrofitClient.kt`:
```kotlin
// Change from:
private const val BASE_URL = "http://10.0.2.2:5000/"
// To production URL:
private const val BASE_URL = "https://api.smartsolarmicrogrid.com/"
```

---

## API Contract Reference

### Create Reservation — `POST /api/reservations`

**Request Body:**
```json
{
  "prosumerId": "string (MongoDB ObjectId or user ID)",
  "stationId": "string (MongoDB ObjectId)",
  "bookingSlotId": "string (MongoDB ObjectId)",
  "scheduledDateTime": "2026-09-27T10:00:00Z"
}
```

**Success Response (201):**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "prosumerId": "...",
    "stationId": "...",
    "bookingSlotId": "...",
    "scheduledDateTime": "2026-09-27T10:00:00Z",
    "status": "Pending",
    "qrCode": null,
    "requestedAt": "...",
    "updatedAt": "..."
  },
  "message": "Reservation created successfully"
}
```

**Error Response (400) – Business Rule Violation:**
```json
{
  "success": false,
  "message": "Reservation must be within 7 days from today.",
  "code": "RESERVATION_WINDOW_INVALID"
}
```

---

## Database Schema Reference

### Collection: `EnergyReservations`
```json
{
  "_id": "ObjectId",
  "ProsumerId": "string",
  "StationId": "ObjectId (→ ref: Member 2's station)",
  "BookingSlotId": "ObjectId (→ ref: EnergyBookingSlots)",
  "ScheduledDateTime": "ISODate",
  "Status": "Pending | Approved | Cancelled | Completed",
  "QrCode": "string | null",
  "RequestedAt": "ISODate",
  "UpdatedAt": "ISODate",
  "CancelledAt": "ISODate | null",
  "CancelReason": "string | null",
  "FinalizedBy": "ObjectId | null",
  "FinalizedAt": "ISODate | null"
}
```

### Collection: `EnergyBookingSlots`
```json
{
  "_id": "ObjectId",
  "StationId": "ObjectId (→ ref: Member 2's station)",
  "SlotDate": "ISODate",
  "StartTime": "string (HH:mm:ss)",
  "EndTime": "string (HH:mm:ss)",
  "BatterySlotId": "string (Bay-1, Bay-2, etc.)",
  "Status": "Open | Reserved | Completed",
  "CreatedAt": "ISODate",
  "UpdatedAt": "ISODate"
}
```

---

## Business Rules

> [!CAUTION]
> These rules are **enforced in the backend service layer** and must NOT be bypassed during integration. They are core academic requirements for SE4040.

### Rule 1 – 7-Day Booking Window
- Prosumers can only book energy slots **within the next 7 days** from today.
- Bookings in the past or beyond 7 days will be **rejected** with `RESERVATION_WINDOW_INVALID`.
- **Implemented in:** `ReservationService.cs` → `CreateReservationAsync()` (Line 28)

### Rule 2 – 12-Hour Cancellation / Modification Lockout
- Prosumers **cannot cancel or modify** a reservation if it is scheduled **within 12 hours from now**.
- Any such attempt will be **rejected** with `MODIFICATION_WINDOW_CLOSED`.
- **Implemented in:** `ReservationService.cs` → `CancelReservationAsync()` (Line 148) and `UpdateReservationAsync()` (Line 81)

### Rule 3 – No Double Booking (Atomic Slot Reservation)
- Slots are atomically set from `Open → Reserved` using a MongoDB `findOneAndUpdate` with a filter.
- If two users simultaneously try to book the same slot, only one succeeds.
- **Implemented in:** `ReservationRepository.cs` → `TryReserveSlotAsync()`

---

## Station ID Replacement Guide

During development, Member 3 used these **placeholder IDs** that were inserted manually into MongoDB for testing:

| Placeholder | Used As |
|---|---|
| `60d5ec49f1b2c42d8c3b4a59` | Station A – Solar Bay |
| `60d5ec49f1b2c42d8c3b4a60` | Station B – North Grid |

### Files to Update with Real IDs:
| File | Location | What to Change |
|---|---|---|
| `seed.js` | Lines 15-16 | `stationA`, `stationB` ObjectId values |
| `SlotSelectionActivity.kt` | `stations = listOf(...)` block | Station ID strings in the list |
| `SlotBookingPage.jsx` | `const STATIONS = [...]` | ID values in the array |
| `ReservationsController.cs` | Line 145 (SeedSlots endpoint) | `stationId` string for the dev seed endpoint |

---

## Testing After Integration

Run through these test scenarios after integration to confirm everything works:

| # | Test Scenario | Expected Result |
|---|---|---|
| 1 | Book a slot for tomorrow | ✅ Reservation created, Status = `Pending` |
| 2 | Book a slot for 8 days later | ❌ Error: `RESERVATION_WINDOW_INVALID` |
| 3 | Book an already-booked slot | ❌ Error: `SLOT_UNAVAILABLE` |
| 4 | Cancel a booking scheduled 24h from now | ✅ Cancelled, slot returns to `Open` |
| 5 | Cancel a booking scheduled in 6 hours | ❌ Error: `MODIFICATION_WINDOW_CLOSED` |
| 6 | Approve a `Pending` reservation | ✅ Status = `Approved`, QR code generated |
| 7 | View Booking History | ✅ All reservations listed with status badges |
| 8 | Station Name shows readable name (not raw ID) | ✅ After Step 3 integration |

---

## Common Integration Issues

| Problem | Cause | Fix |
|---|---|---|
| `404 Not Found` on `/api/reservations` | Controller not registered | Check `Program.cs` has `AddControllers()` and `MapControllers()` |
| `Connection refused` from Mobile App | API not running or ADB reverse not set | Run API, then `adb reverse tcp:5000 tcp:5000` |
| Slots not appearing for a date | seed.js used old Station IDs | Re-run `node seed.js` after updating IDs |
| `SLOT_UNAVAILABLE` on all slots | Slots still marked `Reserved` from dev testing | Manually set `Status: "Open"` in MongoDB or re-run seed |
| `ProsumerId` mismatch in history | Placeholder ID used instead of real auth ID | See Step 4 – Connect Authentication |
| `MODIFICATION_WINDOW_CLOSED` on test cancel | Test booking is within 12 hours | Create a booking for 2+ days later to test cancel |
| Station name shows raw ObjectId | Member 2's station API not integrated yet | This is expected; will be fixed in Step 3 |

---

*Generated: 2026-09-25 | Member 3 – Reservation Workflow | SE4040 EAD 2026*
