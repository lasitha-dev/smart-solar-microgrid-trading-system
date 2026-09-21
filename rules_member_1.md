---
trigger: always_on
---

# AGENT RULES: SMART SOLAR MICROGRID TRADING SYSTEM (SSMTS)
## Component: Identity, Authentication & Account Lifecycle (Member 1)

- **Target Branch:** `feature/member-1-auth-user-management`
- **Module:** SE4040 Enterprise Application Development (2026)
- **Student Name:** SILVA M N U
- **Student ID:** IT22169112

---

### 1. Scope & Responsibility Boundaries

You are an expert Enterprise Full-Stack & Android Systems Engineer building the dedicated slice for **Member 1**. You must operate strictly within these core functional boundaries:

1. **Role-Based Authentication:** Authenticate Backoffice, Grid Operator, and Prosumer accounts through the central C# Web API (`POST /api/auth/login`).
2. **Role-Based Routing:** Return role and session token on login, correctly routing users to Backoffice Home, Operator Home, or Prosumer Home.
3. **Prosumer Self-Registration:** Enable Prosumers to register via the mobile application using NIC as the unique primary business key, defaulting their status to `PendingActivation` (`POST /api/prosumers`).
4. **Pending Activation Review (Web):** Provide Backoffice UI and endpoints to review and activate/reject pending Prosumer registrations (`GET /api/users/pending`, `PATCH /api/users/{id}/activate`).
5. **Backoffice User Management (Web & API):** Allow Backoffice administrators to create and manage Backoffice and Grid Operator accounts (`POST /api/users`, `GET /api/users`).
6. **Prosumer Profile Management (Mobile):** Enable Prosumers to view and update permitted profile fields via Android (`PUT /api/prosumers/{nic}`).
7. **Prosumer Self-Deactivation (Mobile):** Allow Prosumers to request account deactivation from the Android client (`PATCH /api/prosumers/{nic}/request-deactivation`).
8. **Account Lifecycle Administration:** Backoffice administration of account deactivation and reactivation workflows (`PATCH /api/users/{id}/deactivate`).
9. **Password Security:** Never persist plaintext passwords. Implement secure hashing (BCrypt or PBKDF2).
10. **Android Local Session Persistence:** Cache active login state, Bearer token, user role, and cached user details locally in SQLite/Room (`ssmts_local.db`).

**Strict Architectural Invariants:**
- **FAT Service Invariant:** The client contains zero authoritative business rules. All unique constraints, role checks, password hashing, and lifecycle validations reside strictly in the C# Web API hosted on Windows IIS.
- **Pure Native Android Only:** Use Kotlin targeting SDK 34 (Android 14) and Java 17 toolchain. Absolutely NO cross-platform frameworks (Flutter, React Native, KMM).
- **Direct Database Restriction:** Web and Android clients must NEVER connect directly to MongoDB. All data flows exclusively through RESTful JSON endpoints.

---

### 2. Architecture & File Placement Guidelines

Inspect the project directory before generating code. Place every file strictly inside the repository's configured clean structure:

#### Backend Web API (`src/server/SmartSolarMicrogrid.Api`)
```text
Controllers/
├── AuthController.cs               # /api/auth/login endpoint
├── UsersController.cs              # /api/users endpoints (Backoffice user admin, pending reviews)
└── ProsumersController.cs          # /api/prosumers endpoints (registration, profile, self-deactivation)
Models/
├── User.cs                         # MongoDB entity mapping to collection "User's Detail"
└── Enums/
    ├── UserRole.cs                 # Backoffice, GridOperator, Prosumer
    └── AccountStatus.cs            # Active, Deactivated, PendingActivation
DTOs/
├── LoginRequestDto.cs
├── LoginResponseDto.cs
├── UserCreateDto.cs
├── ProsumerRegisterDto.cs
├── ProsumerUpdateDto.cs
└── UserResponseDto.cs
Services/
├── IUserService.cs
├── UserService.cs                  # Lifecycle transitions, hashing, validation
├── ITokenService.cs
└── TokenService.cs                 # JWT token generation with role and NIC claims
Data/
└── MongoDbContext.cs               # MongoDB Driver 3.1.0 context for "User's Detail"

````

#### Web Application (`src/web/smart-solar-microgrid-web`)

Plaintext

```
src/
├── authentication/
│   ├── LoginPage.jsx               # Role-based login screen
│   ├── ProtectedRoute.jsx          # Route guard for Backoffice/Operator roles
│   └── useAuth.js                  # Auth state hook
├── users/
│   ├── UserManagementPage.jsx      # Backoffice UI: create & list web users
│   └── UserFormModal.jsx
├── prosumers/
│   ├── PendingActivationsPage.jsx  # Backoffice UI: review pending prosumers
│   └── PendingProsumerTable.jsx    # Table with Activate/Reject actions
└── services/
    └── authService.js              # Axios/Fetch client injecting Authorization header

```

#### Android Native Application (`src/mobile-android/SmartSolarMicrogridAndroid`)

Plaintext

```
app/src/main/java/com/sliit/ssmts/
├── data/
│   ├── local/
│   │   ├── SsmtsDatabase.kt        # Room database (ssmts_local.db)
│   │   ├── dao/
│   │   │   └── SessionDao.kt       # Session CRUD queries
│   │   └── entity/
│   │       └── SessionEntity.kt    # SQLite table: token, nic, role, fullName
│   ├── remote/
│   │   ├── AuthApi.kt              # Retrofit service interface
│   │   ├── dto/
│   │   │   ├── LoginRequest.kt
│   │   │   ├── RegisterRequest.kt
│   │   │   └── ProfileUpdateRequest.kt
│   │   └── interceptor/
│   │       └── AuthHeaderInterceptor.kt
│   └── repository/
│       └── AuthRepositoryImpl.kt   # Syncs Retrofit responses with SessionDao
├── domain/
│   ├── model/
│   │   └── UserSession.kt
│   └── repository/
│       └── IAuthRepository.kt
├── ui/
│   ├── auth/
│   │   ├── LoginActivity.kt        # Prosumer & Operator mobile login
│   │   ├── RegisterActivity.kt     # Prosumer registration form
│   │   └── AuthViewModel.kt        # StateFlow/LiveData for login & registration
│   └── profile/
│       ├── ProfileActivity.kt      # Profile editing & deactivation request
│       └── ProfileViewModel.kt
└── util/
    ├── SessionManager.kt           # Encapsulates SQLite token state
    └── NetworkResult.kt            # Sealed class: Success, Error, Exception

```

### 3. Exact Versions & Dependencies (Aligned to reference.md)

- **Backend Framework:** .NET SDK `10.0.x` (`net10.0`, C# 13).
- **Database Driver:** `MongoDB.Driver` `3.1.0`.
- **Server Tests:** `xunit` `2.9.2`, `xunit.runner.visualstudio` `2.8.2`, `Microsoft.NET.Test.Sdk` `17.11.1`.
- **Mobile Target:** Native Android (Kotlin `2.0.21`, Java 17 / JVM `17`, `compileSdk` 34, `targetSdk` 34, `minSdk` 26).
- **Mobile Build Tools:** Gradle `8.13`, AGP `8.4.2`, KSP `2.0.21-1.0.26`.
- **Android Persistence & Networking:**
  - Room SQLite: `androidx.room:room-runtime:2.6.1`, `androidx.room:room-ktx:2.6.1`.
  - Retrofit: `com.squareup.retrofit2:retrofit:2.11.0` + `converter-gson:2.11.0`.
  - OkHttp: `com.squareup.okhttp3:okhttp:4.12.0`.
- **Web Frontend:** React.js (v18/v19 with Vite), Bootstrap 5 or Tailwind CSS.

### 4. Code Standards & Academic Integrity Compliance

1. **Mandatory File Header Comment:**
   Place this comment block at the very top of every `.cs`, `.kt`, and `.jsx` file:

   Kotlin
   ```
   /**
    * Description: {Class responsibility, layer, and architectural context}
    */

   ```
2. **Javadoc & Inline Method Headers:**
   Every public and internal method must include comprehensive comments specifying business intent, inputs, return data, and exceptions.
3. **Clean Android Architecture:**
   - Always nullify ViewBinding in `onDestroyView()` for Fragments to prevent memory leaks.
   - Zero hardcoded strings or dimensions (`res/values/strings.xml`, `colors.xml`, `dimens.xml`).
4. **Defensive Validation:**
   - Server-side checks must guard against duplicate NICs, duplicate usernames, and invalid account status transitions.
   - Passwords must be hashed before storage; plain passwords must never be logged or returned in DTOs.

### 5. Mandatory Automated Testing Suite

Every sub-feature created must be accompanied by automated unit tests:

1. **`AuthServiceTests.cs`****:** Tests password hashing, JWT claims generation, and rejection of deactivated/pending accounts.
2. **`UserLifecycleTests.cs`****:** Asserts pending activation lists, prosumer activation transitions, and duplicate NIC 409 Conflict handling.
3. **`SessionDaoTest.kt`****:** In-memory Room test validating session insertion, retrieval, and cache eviction.
4. **`AuthViewModelTest.kt`****:** Tests coroutine flows and UI state transitions (`Loading` -> `Success` / `Error`).

````

---

### 2. `DEVELOPMENT_ROADMAP_MEMBER_1.md`

```markdown
