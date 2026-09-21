# Development Phases & Sub-Phases: Identity, Auth & User Lifecycle (Member 1)

Based on an in-depth analysis of **`SRS.pdf`**, **`EAD_SE4040_Assignment_2026.pdf`**, and **`reference.md`**, here is the comprehensive breakdown of all Phases and Sub-Phases required for developing Member 1's vertical slice.

---

## Component Overview & Target Context

- **Target Branch:** `feature/member-1-auth-user-management`
- **Student Name:** SILVA M N U
- **Student ID:** IT22169112
- **Module:** SE4040 Enterprise Application Development (2026)
- **Target Score Allocation:** 22 / 65 Individual Marks (9 marks Mobile Auth & Account, 8 marks Web User Management & Login, 5 marks Service Integration & Local SQLite Persistence)

---

## Development Phases & Sub-Phases Roadmap

```mermaid
flowchart TD
    P1[Phase 1: Environment & Foundation Setup] --> P2[Phase 2: Central C# Web API & MongoDB Backend]
    P2 --> P3[Phase 3: Web Application - Auth & User Admin UI]
    P2 --> P4[Phase 4: Android App - Session SQLite & Network Layer]
    P4 --> P5[Phase 5: Android App - Prosumer Mobile UI]
    P3 & P5 --> P6[Phase 6: Mandatory Automated Testing Suite]
    P6 --> P7[Phase 7: Academic Compliance, IIS Deployment & Viva Prep]

````

## Phase 1: Environment & Foundation Setup

- **Sub-phase 1.1: Shared Data Model Definition**
  - Define `User.cs` mapping to MongoDB collection `"User's Detail"`.
  - Required fields: `Id` (ObjectId), `Role` (Backoffice, GridOperator, Prosumer), `NIC` (unique business key), `Username`, `PasswordHash`, `FullName`, `Phone`, `Status` (Active, Deactivated, PendingActivation), `CreatedAt`, `UpdatedAt`.
- **Sub-phase 1.2: DTOs & Validation Contracts**
  - Implement serialization models: `LoginRequestDto`, `LoginResponseDto`, `UserCreateDto`, `ProsumerRegisterDto`, `ProsumerUpdateDto`, and `UserResponseDto`.
- **Sub-phase 1.3: Package Scaffolding**
  - Organize C# API controllers, models, and services under `src/server/SmartSolarMicrogrid.Api/`.
  - Scaffold React feature folders under `src/web/smart-solar-microgrid-web/src/` (`authentication`, `users`, `prosumers`).
  - Scaffold Kotlin Android packages under `com.sliit.ssmts` (`ui/auth`, `ui/profile`, `data/local`, `data/remote`).

## Phase 2: Central C# Web API & MongoDB Backend (FAT Service Invariant)

- **Sub-phase 2.1: Password Security & Token Generation Service**
  - Implement BCrypt/PBKDF2 hashing helper to ensure zero plaintext storage.
  - Implement JWT service issuing bearer tokens containing `userId`, `role`, and `nic` claims.
- **Sub-phase 2.2: Authentication & Registration Endpoints**
  - `POST /api/auth/login`: Authenticates username/password or NIC; blocks users with status `PendingActivation` (403) or `Deactivated` (403).
  - `POST /api/prosumers`: Registers Prosumer with NIC uniqueness check; enforces initial status as `PendingActivation`.
- **Sub-phase 2.3: User Management & Lifecycle Endpoints**
  - `POST /api/users`: Allows Backoffice officers to create Backoffice and Grid Operator accounts.
  - `GET /api/users/pending`: Lists all Prosumers currently in `PendingActivation` status.
  - `PATCH /api/users/{id}/activate`: Backoffice transitions pending Prosumer to `Active`.
  - `PUT /api/prosumers/{nic}`: Allows Prosumer to update permitted profile fields (e.g., fullName, phone).
  - `PATCH /api/prosumers/{nic}/request-deactivation`: Initiates Prosumer self-deactivation workflow.
  - `PATCH /api/users/{id}/deactivate`: Backoffice executes account deactivation/reactivation.

## Phase 3: Web Application - Auth & User Admin UI (React.js + Bootstrap 5 / Tailwind)

- **Sub-phase 3.1: Auth Context & Protected Routing**
  - Implement `AuthContext` and Axios request interceptor injecting `Authorization: Bearer <token>`.
  - Implement `ProtectedRoute` blocking non-Backoffice users from system administration screens.
- **Sub-phase 3.2: Web Login Screen**
  - Responsive login view with client-side validation, error banners, and role-based redirection.
- **Sub-phase 3.3: Backoffice User Management Page**
  - UI view listing Backoffice and Grid Operator users.
  - Modal form to create new administrative accounts.
- **Sub-phase 3.4: Pending Activations Review Page**
  - Table showing pending prosumers with NIC, Full Name, and Phone.
  - Action buttons to "Activate" or "Reject" registrations with live API refresh.

## Phase 4: Android App - Session SQLite & Network Layer

- **Sub-phase 4.1: SQLite Local Persistence (****`ssmts_local.db`****)**
  - Implement Room database `SsmtsDatabase.kt` and `SessionEntity.kt` (`token`, `userId`, `role`, `nic`, `fullName`).
  - Implement `SessionDao.kt` supporting `saveSession()`, `getActiveSession()`, and `clearSession()`.
- **Sub-phase 4.2: Retrofit Network Service & Interceptors**
  - Define `AuthApi.kt` covering login, prosumer registration, and profile endpoints.
  - Implement `AuthHeaderInterceptor.kt` dynamically appending the stored token to requests.
- **Sub-phase 4.3: Repository Implementation**
  - Build `AuthRepositoryImpl.kt` orchestrating remote API calls, caching active credentials in SQLite, and providing offline session retrieval.

## Phase 5: Android App - Prosumer Mobile UI (Kotlin Native)

- **Sub-phase 5.1: Mobile Login Screen (****`LoginActivity`****)**
  - Clean Material 3 login layout supporting Prosumer and Grid Operator credentials.
  - Surfaces explicit error states when account is in `PendingActivation` or `Deactivated`.
  - Stores session in SQLite on success and routes to role-specific Home screen.
- **Sub-phase 5.2: Prosumer Registration Screen (****`RegisterActivity`****)**
  - Form capturing NIC, Full Name, Phone, and Password.
  - Pre-validates NIC syntax before submission.
  - Success screen informing user that account activation is pending Backoffice review.
- **Sub-phase 5.3: Profile & Self-Deactivation Screen (****`ProfileActivity`****)**
  - Displays cached profile details from SQLite with live update capability.
  - "Request Account Deactivation" button triggering confirmation alert and server workflow.

## Phase 6: Mandatory Automated Testing Suite

- **Sub-phase 6.1: Backend xUnit Test Suite (****`tests/server`****)**
  - `AuthServiceTests.cs`: Asserts valid credential login, rejects bad passwords, and verifies password hashing.
  - `UserLifecycleTests.cs`: Asserts duplicate NIC conflict (409), pending activation filtering, and activation transitions.
- **Sub-phase 6.2: Android Unit & SQLite DAO Tests (****`app/src/test`****)**
  - `SessionDaoTest.kt`: Tests Room in-memory database operations (session insert, query, update, purge).
  - `AuthViewModelTest.kt`: Uses Coroutine Test Dispatchers to verify `Loading` -> `Success` / `Error` state flows.

## Phase 7: Academic Compliance, IIS Deployment & Viva Prep

- **Sub-phase 7.1: SE4040 Academic Header & Comment Audit**
  - Verify every single `.cs`, `.kt`, and `.jsx` file contains the standardized header with student details (`SILVA M N U`, `IT22169112`).
  - Ensure all public methods include Javadoc/XML documentation.
- **Sub-phase 7.2: IIS Deployment Verification**
  - Publish API to Windows IIS and confirm Web and Android clients communicate seamlessly over local IP.
- **Sub-phase 7.3: Viva Demonstration Walkthrough Verification**
  - Verify end-to-end flow: Mobile Prosumer Registration -> Shows as Pending on Web -> Backoffice Activates on Web -> Mobile Login succeeds -> Profile edit -> Deactivation request.

```

---
