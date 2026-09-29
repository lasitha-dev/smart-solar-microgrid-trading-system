---
trigger: always_on
---

# AGENT RULES: SMART SOLAR MICROGRID TRADING SYSTEM (SSMTS)
## Component: Microgrid Nodes, Schedules & Maps (Member 2)

- **Target Branch:** `feature/member-2-microgrid-node-management`
- **Module:** SE4040 Enterprise Application Development (2026)
- **Student Role:** Member 2

---

### 1. Scope & Responsibility Boundaries

You are an expert Full-Stack & Android Enterprise Systems Engineer building the dedicated slice for **Member 2**. You must operate strictly within these boundaries:

1. **Station Data Modeling:** Design `SolarStationInfo` and `EnergyBookingSlot` entities for MongoDB.
2. **Station CRUD Operations (Web API):** Implement secure C# endpoints to Create, Read, Update, and soft-delete/deactivate microgrid nodes.
3. **Deactivation Business Rule:** Enforce the strict FAT Service rule: A node CANNOT be deactivated if it has any Pending or Approved reservations.
4. **Slot Generation & Management:** Logic to manage available battery bay slots and operating schedules.
5. **React Admin UI:** Build responsive Web UI for `StationListPage`, `StationFormModal`, and `StationDeactivateModal` using React & Bootstrap 5/Tailwind.
6. **Android Maps Integration:** Implement Google Maps API (or local map fallback) in Android.
7. **Nearby Stations Discovery:** Query and plot active grid nodes on the Android map using GPS coordinates (Latitude/Longitude).
8. **Location Hardware Handling:** Securely request and handle Android `ACCESS_FINE_LOCATION` permissions.

**Strict Technical Constraints:**
- **Pure Native Android Only:** Kotlin/Java targeting SDK 34. NO cross-platform frameworks.
- **FAT Service Invariant:** The React and Android clients contain ZERO authoritative business logic. All validation (especially node deactivation) resides on the C# Web API.
- **API Communication:** Clients must never directly touch MongoDB. All communication is via REST APIs.

---

### 2. Architecture & File Placement Guidelines

Inspect the project directory before generating code. Place files strictly inside the structured directories:

**C# Web API (`src/server/SmartSolarMicrogrid.Api`)**
- `Models/SolarStationInfo.cs`, `Models/EnergyBookingSlot.cs`
- `DTOs/StationCreateDto.cs`, `DTOs/NearbyStationDto.cs`
- `Controllers/StationsController.cs`
- `Services/StationService.cs`, `Services/IStationService.cs`

**React Web App (`src/web/smart-solar-microgrid-web/src`)**
- `services/stationService.js` (Axios API calls)
- `features/microgrid-nodes/pages/StationListPage.jsx`
- `features/microgrid-nodes/components/StationFormModal.jsx`
- `features/microgrid-nodes/components/StationDeactivateModal.jsx`

**Android App (`src/mobile-android/SmartSolarMicrogridAndroid/.../microgrid_nodes`)**
- `data/remote/StationApi.kt`, `data/repository/StationRepositoryImpl.kt`
- `domain/model/MicrogridStation.kt`
- `ui/map/NearbyStationsActivity.kt` (or Fragment)
- `ui/map/StationMapViewModel.kt`

---

### 3. Security & Device Access Standards

- **Map & Location Hardware:**
  - Request `android.permission.ACCESS_FINE_LOCATION`.
  - Handle permission denials gracefully without crashing.
  - Never hardcode Google Maps API keys in the source code (use `local.properties` or `.env`).
- **Deactivation Security:**
  - Deactivation requests must verify the JWT token role (`Backoffice` only).
  - Return HTTP 409 Conflict if deactivation is attempted on a node with active bookings.

---

### 4. SE4040 Academic Documentation Compliance

Every file must include the mandatory header block and Javadocs/XML comments:

```kotlin
/**
 * Description: {Detailed explanation of the class responsibility and architectural layer}
 * Author: Member 2
 */