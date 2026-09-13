# Smart Solar Microgrid Trading System

An enterprise client-server Smart Solar Microgrid Trading System for managing solar prosumers, microgrid nodes, energy-slot reservations, operator verification, and energy-transfer transactions.

## Architecture

- **Web Application:** Responsive UI for Backoffice and Grid Operator workflows.
- **Mobile Application:** Pure native Android application for Prosumer and Grid Operator workflows.
- **Web Service:** C# Web API hosted on IIS.
- **Database:** MongoDB server-side database.
- **Local Android Persistence:** SQLite.
- **External Integration:** Google Maps API and QR-code scanning.
- **Business Logic:** Centralized in the Web API following the FAT Service pattern.

## Main Features

- Role-based authentication and authorization
- Backoffice user management
- Prosumer registration and profile management
- Prosumer account activation/deactivation
- Microgrid node management
- GPS and nearby-node mapping
- Energy-slot management
- Reservation creation, modification and cancellation
- Seven-day reservation scheduling rule
- Twelve-hour modification/cancellation rule
- Reservation approval workflow
- Transaction QR generation
- QR scanning and server verification
- Energy-transfer finalization
- Current and pending booking views
- Booking history
- Search and filtering
- Operational dashboard
- SQLite local persistence

## Repository Structure

```text
.github/
├── ISSUE_TEMPLATE/
│   └── feature.md
├── workflows/
└── pull_request_template.md

assets/
└── screenshots/

config/

docs/
├── api/
│   └── API_DOCUMENTATION.md
├── architecture/
│   ├── high-level-architecture.drawio
│   ├── use-case-diagram.drawio
│   └── dfd.drawio
├── database/
│   ├── database-design.md
│   └── sample-data/
├── srs/
│   └── SRS.pdf
└── testing/
    ├── test-plan.md
    └── test-results.md

scripts/

src/
├── server/
│   ├── SmartSolarMicrogrid.Api/
│   └── README.md
├── web/
│   ├── smart-solar-microgrid-web/
│   └── README.md
└── mobile-android/
    ├── SmartSolarMicrogridAndroid/
    └── README.md

tests/
├── server/
└── web/

.gitignore
BRANCH_PLAN.md
README.md
```

## Development Architecture

```text
                    Smart Solar Microgrid
                              │
             ┌────────────────┴────────────────┐
             │                                 │
             ▼                                 ▼
      Web Application                  Native Android
             │                                 │
             │ REST                            │ REST
             │                                 │
             └────────────────┬────────────────┘
                              ▼
                       C# Web API
                              │
                ┌─────────────┴─────────────┐
                │                           │
                ▼                           ▼
         Business Logic                Data Access
                                            │
                                            ▼
                                         MongoDB
```

### Client Responsibilities

The Web Application and Native Android Application act as user-interface clients.

They must communicate with the central Web API through REST API calls.

### Server Responsibilities

The C# Web API is responsible for:

* Business logic
* Validation
* Authorization
* Reservation rules
* Account management
* Microgrid management
* Data processing
* Database access
* QR verification
* Energy-transfer finalization

### Database Access Rule

Clients must **not** access MongoDB directly.

```text
Web Application ────────┐
                        │
                        ▼
                    C# Web API
                        │
                        ▼
                     MongoDB
                        ▲
                        │
Native Android ─────────┘
```

## Technology Stack

### Web Application

* React.js
* Bootstrap 5 or Tailwind CSS
* REST API integration
* Responsive UI

### Mobile Application

* Native Android
* Java or Kotlin
* SQLite
* Google Maps API
* QR-code scanning

### Web Service

* C#
* ASP.NET Web API
* IIS
* RESTful APIs
* FAT Service architecture

### Database

* MongoDB

## User Roles

The system contains the following main roles:

### Backoffice

Responsible for:

* Managing web application users
* Managing prosumer accounts
* Activating/deactivating accounts
* Managing microgrid nodes
* Managing operational information

### Grid Operator

Responsible for:

* Monitoring bookings
* Managing operational activities
* Accessing the mobile application
* Scanning transaction QR codes
* Verifying transactions with the server
* Finalizing energy transfers

### Solar Prosumer

Responsible for:

* Registering an account
* Maintaining profile information
* Requesting account deactivation
* Viewing available microgrid nodes
* Making reservations
* Updating reservations
* Cancelling reservations
* Viewing booking history
* Receiving and presenting transaction QR codes

## Main Business Rules

### Reservation Scheduling

Reservations must be scheduled within **7 days**.

### Reservation Modification

Reservation updates require at least **12 hours' notice**.

### Reservation Cancellation

Reservation cancellations require at least **12 hours' notice**.

### Microgrid Node Deactivation

A microgrid node cannot be deactivated while it has active energy reservations.

### Prosumer Account Deactivation

Prosumer deactivation requests must follow the defined activation/deactivation workflow.

### Transaction Finalization

A Grid Operator must verify the prosumer's transaction QR code against the server before finalizing the energy transfer.

## Repository Branches

The project uses four primary feature branches.

### Member 1 — Authentication & User Management

```text
feature/member-1-auth-user-management
```

Responsibilities:

* Authentication
* Authorization
* Role management
* Backoffice user management
* Prosumer registration
* Prosumer profile management
* Account activation
* Account deactivation
* Pending activation workflow

### Member 2 — Microgrid Node Management

```text
feature/member-2-microgrid-node-management
```

Responsibilities:

* Microgrid node CRUD
* GPS coordinates
* Capacity management
* Battery-slot management
* Operational schedules
* Node availability
* Node deactivation rules
* Nearby-node functionality
* Google Maps integration

### Member 3 — Reservation Workflow

```text
feature/member-3-reservation-workflow
```

Responsibilities:

* Energy-slot booking
* Reservation creation
* Reservation modification
* Reservation cancellation
* Seven-day scheduling rule
* Twelve-hour modification rule
* Twelve-hour cancellation rule
* Reservation approval
* Booking summary

### Member 4 — Operator Verification & Dashboard

```text
feature/member-4-operator-verification-dashboard
```

Responsibilities:

* QR generation
* QR scanning
* QR server verification
* Energy-transfer finalization
* Grid Operator workflow
* Current bookings
* Pending bookings
* Booking history
* Search and filtering
* Operational dashboard

## Equal Contribution Principle

Each member owns a complete vertical feature slice.

Every feature owner should contribute to the relevant:

* Web API functionality
* Business logic
* MongoDB operations
* Web UI
* Android UI where applicable
* Validation
* Testing
* Documentation

The objective is to maintain approximately equal technical workload among all four members.

Shared activities such as:

* Integration
* Deployment
* API testing
* Code review
* Documentation
* Final testing

should be distributed among all members rather than assigned permanently to one person.

## Git Workflow

The `main` branch contains the stable integrated version of the project.

Development is performed using feature branches.

```text
main
 │
 ├── feature/member-1-auth-user-management
 │
 ├── feature/member-2-microgrid-node-management
 │
 ├── feature/member-3-reservation-workflow
 │
 └── feature/member-4-operator-verification-dashboard
```

### Recommended Workflow

Before starting work:

```bash
git checkout main
git pull origin main
git checkout <your-feature-branch>
git pull origin <your-feature-branch>
```

After completing a change:

```bash
git add .
git commit -m "feat: describe the change"
git push origin <your-feature-branch>
```

Then create a Pull Request:

```text
<feature-branch> → main
```

Another team member should review the changes before merging.

## Commit Convention

Use descriptive commit messages.

### New Feature

```text
feat: implement prosumer registration
```

### Bug Fix

```text
fix: enforce reservation cancellation rule
```

### Database Change

```text
db: add energy reservation collection
```

### Documentation

```text
docs: update reservation API documentation
```

### Testing

```text
test: add reservation validation tests
```

### Configuration

```text
chore: configure MongoDB connection settings
```

Avoid vague commit messages such as:

```text
update
changes
final
test
working
```

## Development Rules

1. All business logic must remain in the central Web API.
2. Web and Android clients must communicate with the Web API through REST APIs.
3. The Android application must remain a pure native Android application.
4. SQLite must be used for the required Android local persistence.
5. Clients must not connect directly to MongoDB.
6. API keys, passwords, secrets and production connection strings must never be committed.
7. Use meaningful and descriptive Git commits.
8. Develop features using feature branches.
9. Changes to `main` should be made through Pull Requests.
10. Every member must document their individual contribution.
11. Every member should understand the work they submit for the viva.
12. Code obtained from external sources must be properly referenced according to the assignment requirements.

## Documentation

The `docs/` directory contains the project's documentation.

### Software Requirements Specification

```text
docs/srs/
```

Contains the approved SRS.

### Architecture

```text
docs/architecture/
```

Contains:

* High-level architecture diagram
* Use case diagram
* Data Flow Diagram

### API Documentation

```text
docs/api/
```

Contains API endpoints, request/response formats and authentication requirements.

### Database Design

```text
docs/database/
```

Contains:

* MongoDB collection design
* Field definitions
* Relationships/references
* Sample data

### Testing

```text
docs/testing/
```

Contains:

* Test plan
* Test cases
* Test results

## Required MongoDB Collections

The system should maintain the required collections identified in the project specification:

```text
User's Detail
SolarStationInfo
EnergyBookingSlots
Energy Reservation
```

Additional collections may be introduced where required by the implementation, provided they remain consistent with the system architecture and assignment requirements.

## Deployment

The Web API is intended to be hosted on:

```text
Windows IIS Server
```

The server uses:

```text
C# Web API
      │
      ▼
   MongoDB
```

Both clients communicate with the hosted API.

## Project Deliverables

The final project should include:

* Web application
* Native Android application
* C# Web API
* MongoDB database
* SQLite local persistence
* Google Maps integration
* QR-code generation/scanning
* API documentation
* Database design
* Architecture diagrams
* Use case diagram
* DFD
* UI screenshots
* Testing documentation
* GitHub repository
* Individual contribution documentation
* Challenges and reflections
* README
* Demonstration video

## Team Members

| Member   | IT Number    | Responsibility                    |
| -------- | ------------ | --------------------------------- |
| Member 1 | `ITXXXXXXXX` | Authentication & User Management  |
| Member 2 | `ITXXXXXXXX` | Microgrid Node Management         |
| Member 3 | `ITXXXXXXXX` | Reservation Workflow              |
| Member 4 | `ITXXXXXXXX` | Operator Verification & Dashboard |

Replace the placeholders with the actual member names and IT numbers.

## Project Status

```text
Planning / Initial Development
```

Update this section as the project progresses.

## License

This project is developed as an academic project for the SE4040 Enterprise Application Development module.

The project must not be redistributed or submitted as another student's work.
