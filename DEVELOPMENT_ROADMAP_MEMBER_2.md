### 2. `DEVELOPMENT_ROADMAP_MEMBER_2.md` (ඔයාගේ පියවරෙන් පියවර සැලැස්ම)

```markdown
# Development Phases & Sub-Phases: Microgrid Nodes, Schedules & Maps (Member 2)

## Component Overview
- **Target Branch:** `feature/member-2-microgrid-node-management`
- **Module:** SE4040 Enterprise Application Development (2026)
- **Target Score Allocation:** Covers Station CRUD, Rule validations, Web UI, and Android Maps integration.

---

## Phase 1: Central C# Web API & MongoDB Backend (Tier 1)
- **Sub-phase 1.1: Data Models & Database Context**
  - Create `SolarStationInfo.cs` (Id, Name, Location, Capacity, Slots, Schedule, Status).
  - Create `EnergyBookingSlot.cs`.
  - Register `IMongoCollection<SolarStationInfo>` in `MongoDbContext.cs`.
- **Sub-phase 1.2: DTOs & Validation**
  - Implement `StationCreateDto`, `StationUpdateDto`, and `NearbyStationDto`.
- **Sub-phase 1.3: Service Layer & Business Rules**
  - Implement `StationService.cs`.
  - **CRITICAL RULE:** Implement `CheckDeactivationEligibility()` to block deactivation if active reservations exist.
- **Sub-phase 1.4: Stations Controller**
  - Implement `GET /api/stations`, `POST /api/stations`, `PUT /api/stations/{id}`, `DELETE /api/stations/{id}` (Soft delete).
  - Implement `GET /api/stations/nearby` for Android Maps.

---

## Phase 2: React Web Admin UI (Tier 2)
- **Sub-phase 2.1: API Integration**
  - Create `stationService.js` using configured Axios interceptors.
- **Sub-phase 2.2: Station Management Views**
  - Build `StationListPage.jsx` (Data table with Active/Inactive badges).
  - Add to `AdminLayout.jsx` sidebar navigation.
- **Sub-phase 2.3: Creation & Deactivation Modals**
  - Build `StationFormModal.jsx` (Form for Capacity, GPS, Schedule).
  - Build `StationDeactivateModal.jsx` (Shows 409 Conflict error clearly if blocking rule fails).

---

## Phase 3: Android Domain & Data Layer (Tier 3)
- **Sub-phase 3.1: Network & Repository Setup**
  - Define `StationApi.kt` Retrofit interface.
  - Implement `StationRepositoryImpl.kt` mapping network responses to `MicrogridStation.kt` domain models.
- **Sub-phase 3.2: Map Dependencies**
  - Add Google Play Services Maps & Location to `build.gradle.kts`.

---

## Phase 4: Android Maps UI & Location Integration (Tier 3)
- **Sub-phase 4.1: Permissions & View Setup**
  - Implement `NearbyStationsActivity.kt` with `SupportMapFragment`.
  - Handle `ACCESS_FINE_LOCATION` runtime permissions.
- **Sub-phase 4.2: Plotting Nodes on Map**
  - Observe `StationMapViewModel.kt`.
  - Plot markers using `lat`/`lng` from the backend.
  - Show a BottomSheet or InfoWindow displaying Station Name, Distance, and Available Slots when a marker is tapped.

---

## Phase 5: Testing & Academic Compliance
- **Sub-phase 5.1: Unit & Integration Testing**
  - C# xUnit tests to verify the Deactivation Business Rule.
  - Android tests for Map ViewModel states.
- **Sub-phase 5.2: Code Cleanliness & Viva Prep**
  - Ensure all `.cs`, `.jsx`, and `.kt` files have the mandatory Academic Header Comments.
  - Ensure ViewBinding is nulled in Android Fragments.
  - Prepare Viva script demonstrating: 1. Creating a station, 2. Failing to deactivate it due to rules, 3. Plotting it on the Android map.