<p align="center">
  <img src="https://img.icons8.com/?size=512&id=vBwH7l0Yq7O-&format=png" alt="Obsidian Ledger Logo" width="150" height="150" />
</p>

<h1 align="center">Obsidian Ledger</h1>

<p align="center">
  <b>A highly designed, offline-first personal finance manager built with Kotlin Multiplatform.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20|%20iOS-green.svg?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.3.20-blue.svg?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose_Multiplatform-UI-purple.svg?style=flat-square&logo=jetpackcompose" alt="Compose">
  <img src="https://img.shields.io/badge/Backend-Supabase-3ECF8E.svg?style=flat-square&logo=supabase" alt="Supabase">
  <img src="https://img.shields.io/badge/Database-SQLDelight-blue?style=flat-square" alt="SQLDelight">
</p>

---

## ⚡️ Overview

**Obsidian Ledger** is a premium, fully reactive money manager designed for blazing-fast local interactions with seamless cloud backups. The app allows users to track expenses, establish dynamic budgets, and analyze spending habits inside a beautiful unified interface seamlessly ported across native Android and iOS operating environments using **Kotlin Multiplatform**.

## 📱 Screenshots

> *Tip: Drop your physical device or emulator screenshots into the `assets/` folder to populate this grid!*

<p align="center">
  <table>
    <tr>
      <td align="center">
        <b>Dark Dashboard</b><br>
        <img src="assets/dashboard.png" width="220" alt="Dashboard Screen"/>
      </td>
      <td align="center">
        <b>Analytics & Sparklines</b><br>
        <img src="assets/analytics.png" width="220" alt="Analytics Screen"/>
      </td>
      <td align="center">
        <b>Budget Tracker</b><br>
        <img src="assets/budgets.png" width="220" alt="Budgets Screen"/>
      </td>
      <td align="center">
        <b>Profile & Settings</b><br>
        <img src="assets/profile.png" width="220" alt="Profile Screen"/>
      </td>
    </tr>
  </table>
</p>


## ✨ Features

- **Offline-First Synchronization**: Utilizes a robust SQLite persistence layer (`SQLDelight`) locally. Any changes map efficiently to a `dirty` flag schema, securely upserting to **Supabase** dynamically in the background via Ktor and Android's `WorkManager`.
- **Dynamic Charting**: Fully bespoke Canvas-rendering logic utilizing `KoalaPlot` to construct elegant sparklines with localized timeframe analytics (tracking out 6 rolling months).
- **Custom Design System**: Adopts an aggressive "monochromatic dark obsidian" aesthetic complemented by an **Emerald Green `(#00C896)`** focal tint constraint. Features `tnum` (tabular-numerals) layouts avoiding native Android strict-borders.
- **Micro-Animations**: Transitions are deeply layered with tonal `AnimatedVisibility` structures built out explicitly in Compose Multiplatform tracking hero balances and status gradients dynamically.
- **Decompose Navigation**: Stack handling and DI injects directly mapped statically via `<RootComponent>` guaranteeing stable application lifecycles and robust back-handler interception regardless of hardware mappings.

## 🛠 Tech Stack

| Layer | Technology | Description |
| :--- | :--- | :--- |
| **User Interface** | Compose Multiplatform (1.10.x) | Shared composables driving UI declarations for Android/iOS natively. |
| **Navigation** | Decompose (3.x) | Lifecycle-aware routing pushing strict component patterns ensuring scalable hierarchy layers. |
| **Architecture** | MVI + Koin (4.x) | Single source of truth state machines (`State -> Intent -> Effect`) injected dynamically. |
| **Database** | SQLDelight (2.x) | Cross-platform SQLite code generation with native Coroutines Flow mappings binding directly to UI collectors. |
| **Cloud Backing** | Supabase-kt (3.4) | Wrapping PostgREST endpoint calls globally and authenticated bindings. |


## 🚀 Getting Started

### 1. Prerequisites
- **JDK 17/21** or higher
- Android Studio / IntelliJ IDEA
- Supabase Project configurations

### 2. Configure Backend 

In the root of the project, edit the `local.properties` file with your specific Subapase reference IDs and publishable keys:

```properties
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_KEY=<your-anon-publishable-key>
```

### 3. Build & Run

**For Android:**
Simply execute Gradle wrapper functions targeting AAPT deployment:
```bash
./gradlew clean
./gradlew :androidApp:installDebug
```

**For iOS:**
Generate expected native Xcode mappings bridging Obj-C headers, then run utilizing Xcode:
```bash
./gradlew :sharedUI:compileKotlinIosSimulatorArm64
```
*Open `iosApp/iosApp.xcworkspace`, target a Simulator, and press Run.*

---
<p align="center">
  <i>Designed and Built via KMP Magic.</i>
</p>
