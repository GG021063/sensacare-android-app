# SensaCare Android – Clean Architecture Guide

Welcome to the SensaCare code-base!  
This document explains **how the project is organised, why it was designed this way, and what you need to know to start contributing confidently.**

---

## 1. Architectural Overview

SensaCare follows Robert C. Martin’s _“Clean Architecture”_ adapted for modern Android/Kotlin:

```
┌────────────────┐
│   Presentation │  ← Jetpack Compose / ViewModels
├────────────────┤
│     Domain     │  ← Pure Kotlin business rules
├────────────────┤
│      Data      │  ← Repositories, DTOs, DAOs
└────────────────┘
```

* Dependency flow always points **inwards** (Presentation ➜ Domain ➜ Core).
* Each outer layer can be replaced without touching the inner ones – ideal for testing, multi-platform or backend reuse.

---

## 2. Layer Structure & Responsibilities

| Layer | Android module | Responsibilities |
|-------|----------------|------------------|
| Presentation | `app` | UI (Compose), `ViewModel`s, navigation, state reduction, error mapping |
| Domain | `domain` | Entities, value objects, use cases (aka interactors), interfaces (repositories, gateways) |
| Data | `data` | Concrete repository impls, network & database sources, mapping DTO ↔ entity |
| Core / Utilities | `core` | Logging, coroutine dispatchers, util classes (e.g., `SingleLiveEvent`) |

---

## 3. Data Flow & Dependency Direction

1. UI event ➜ Presentation `ViewModel`
2. `ViewModel` invokes a **UseCase** (domain interface)
3. UseCase talks to a **Repository** (interface)
4. Repository implementation (data layer) decides:
   * Remote source (REST, BLE)
   * Local cache (Room)
5. Result bubbles back as `Flow<Result<T>>`
6. `ViewModel` reduces it into `UiState` for Compose.

At compile time the arrows are one-way: Presentation depends on Domain (interfaces only), Domain depends on nothing, Data depends on Domain.

---

## 4. Key Components

### Presentation
* `DeviceManagementViewModel`, `HealthDashboardViewModel`, `GoalsManagementViewModel`, `AlertsViewModel`, …
* `UiState` sealed classes – single source of truth for every screen
* Navigation events via `SingleLiveEvent` to avoid multiple deliveries

### Domain
* Entities: `HealthGoal`, `HealthAlert`, `HBandDevice`, …
* Value objects: `YearMonth`, `GoalCalendarEvent`
* UseCases: `ConnectDeviceUseCase`, `ManageHealthGoalsUseCase`, `MonitorHealthAlertsUseCase`, …

### Data
* Retrofit services, Room DAOs, BLE drivers
* Mappers in `*.Mapper.kt`
* Repository implementations: `HealthGoalRepositoryImpl`, `AlertRepositoryImpl`

---

## 5. Domain Entities & Models

Characteristics:
* **Pure Kotlin, no Android imports**
* Immutability by default (`data class` + copy())
* Business invariants encoded in constructors / factory methods

Example: `HealthGoal`
```kotlin
data class HealthGoal(
    val id: Long,
    val userId: Long,
    val type: GoalType,
    val title: String,
    ...
)
```

---

## 6. Use Cases & Business Logic

Single-responsibility interactors encapsulate **what** the app does, not **how**.

* `SyncDeviceDataUseCase`: orchestrates fetching raw BLE packets, mapping, persisting.
* `ManageHealthGoalsUseCase`: CRUD, progress computation, smart suggestions.
* `ProcessEmergencyAlertsUseCase`: end-to-end pipeline from threshold breach to contacting EMS.

Guidelines:
1. Synchronous to caller (suspend/Flow)
2. Stateless; if state needed → inject Repository
3. No Android or framework code

---

## 7. Repository Pattern

```text
Presentation ──▶ Domain (Repository interface) ──▶ Data (RepositoryImpl)
```

* Domain defines capability (`interface AlertRepository`)
* Data implements it, wiring local/remote sources
* Test doubles mock the interface without touching Data layer

---

## 8. Presentation Layer – MVVM

* **Compose** renders `UiState`
* `ViewModel` owns coroutine scope, exposes `StateFlow`
* One-way data flow:
  UI → Intent → ViewModel → State → UI
* Side-effects (navigation, snackbar) via **single-shot** events.

---

## 9. Dependency Injection

* **Hilt** bootstraps graphs per layer.
  * Module groups: `domainModule`, `dataModule`, `bluetoothModule`, `networkModule`
* Constructor injection everywhere ➜ zero boilerplate in tests (`@TestInstallIn`).

---

## 10. Testing Strategy

1. **Unit tests (fast)**
   * Domain use cases with fake repositories
   * Pure functions / mappers
2. **Instrumentation tests**
   * DAO with in-memory Room
   * Repository integration
3. **UI tests**
   * Compose testing rules driving ViewModels
4. **Contract tests**
   * JSON contract between app ↔ backend using WireMock

Continuous Integration runs lint, detekt, unit + instrumentation test suites.

---

## 11. Key Architectural Decisions

* **Flow everywhere** for reactive streams & back-pressure
* **Results wrapper** (`Loading/Success/Error`) instead of exceptions across layers
* **Protobuf over BLE** for binary efficiency
* **Room first, Network second** – offline-first UX

---

## 12. Performance Considerations

* Paging for historic data
* BLE sync throttling & opportunistic batching
* Database indices on timestamp/userId
* ViewModel caches & `distinctUntilChanged()` to minimise recompositions
* Background jobs scheduled with WorkManager, respecting battery constraints

---

## 13. Error Handling Strategies

| Layer | Mechanism |
|-------|-----------|
| Domain | `sealed class Result<out T>` – errors stay typed |
| Data | Retrofit `CallAdapter` maps HTTP codes; Room ops wrapped in `runCatching` |
| Presentation | Centralised `errorEvents` `SharedFlow` per screen; mapped to snackbars/dialogs |
| Global | UncaughtExceptionHandler logs to Crashlytics; critical alerts escalate via `AlertsViewModel` |

---

## 14. Future Extensibility

* **Wear OS** module – reuse Domain & Data untouched
* Plug-in new sensors by adding a `DeviceDriver` + binding in DI
* Multi-user support – swap userId provider implementation
* GraphQL backend – new remote source and mapper, repositories untouched
* Feature modules can live in separate Gradle modules relying only on Domain contracts

---

Happy coding! If anything is unclear, open an issue or ping the #sensa-dev channel.  
_Keep the arrows pointing inwards and the tests green._ 🎉
