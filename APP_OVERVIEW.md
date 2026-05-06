# HopeHealth — App Review Document

> **Last updated:** 2026-05-06
> **Platform:** Android 14+ (Google Pixel 10 Pro)
> **Min SDK:** 34 | **Target SDK:** 35
> **Language:** Kotlin | **UI:** Jetpack Compose + Material 3
> **AI Engine:** Gemini Nano (on-device via ML Kit)
> **DI:** Dagger Hilt | **Database:** Room (v3)

---

## What Is HopeHealth?

HopeHealth is a **privacy-first, offline-capable health tracking app**
built exclusively for the Google Pixel 10 Pro. It combines daily
calorie and macronutrient tracking, step counting, fertility and
menstrual cycle tracking, and on-device AI-powered food recognition
into a single, self-contained application.

### Why Does It Exist?

Most health tracking apps require cloud accounts, sync personal data
to remote servers, and rely on internet connectivity. HopeHealth takes
the opposite approach:

- **Zero cloud dependency** — all data stored locally via Room
- **On-device AI** — Gemini Nano runs on the Pixel 10 Pro's
  TPU/NPU; food parsing, nutrition label scanning, cycle insights,
  and coaching tips never leave the phone
- **No accounts or sign-ups** — open the app and start immediately
- **Single-device focus** — optimized for Pixel 10 Pro hardware

### Who Is It For?

Anyone who wants a simple, private, AI-enhanced health tracker
without the overhead of subscription services, cloud sync, or
social features.

---

## Core Features

### 1. Home Dashboard

The main screen provides an at-a-glance view of daily progress:

- **Animated Calorie Ring** — circular progress showing calories
  consumed vs. goal, with an inner ring for step progress
- **AI Coach Tip** — Gemini Nano generates a personalized daily
  insight based on your food log, step count, and goals
- **Macro Summary** — progress bars for protein, carbs, fat, and
  sugar against configurable daily targets
- **Quick Actions** — add steps or navigate to food logging
- **Celebration Animation** — confetti burst when you hit your goal

### 2. Food Logging (4 Modes)

The app offers four distinct ways to log food:

- **Search** — web search via USDA / OpenFoodFacts / DuckDuckGo
  with AI-powered nutrition extraction
- **AI Parse** — describe what you ate in natural language and
  Gemini Nano extracts individual items with macros
- **Scan** — photograph a nutrition label for AI extraction
- **Manual** — enter food name, calories, and macros directly

Entries appear in a **Food Diary** with edit/delete, inline quantity
editing with auto-scaling macros, and unit conversion (g, oz, cups,
ml, tbsp, tsp).

### 3. Step Tracking

Steps are tracked via a 3-tier fallback system:

1. **Health Connect** (preferred)
2. **Hardware Pedometer** (built-in sensor)
3. **Manual Entry**

The Steps screen shows today's count with progress bar, derived
metrics (distance, calories, active minutes), and a 7-day animated
line chart with performance-colored fill.

### 4. Family — Fertility & Cycle Tracking

A full-featured menstrual cycle and fertility tracker built as an
**app-within-an-app** with its own Flo-inspired warm pink/teal
color palette. Enabled by default; toggleable in Settings.

#### Cycle Logging & Calendar

- **Period Logging** — start/end dates, daily flow intensity
- **Cycle Calendar** — monthly view with color-coded phase dots
- **Cycle History** — avg/shortest/longest cycle, regularity score
- **Anomaly Alerts** — flags cycles under 21d or over 35d

#### 3-Layer Prediction Engine

- **Calendar method** — avg cycle length − 14 = ovulation.
  Always available, even with minimal data.
- **Temperature shift** — detects a biphasic BBT rise
  (0.2°F sustained 3 days) to confirm ovulation.
  Requires 8+ days of temperature readings.
- **Symptom cross-reference** — correlates egg-white
  cervical mucus, sex drive peaks, breast tenderness,
  and fatigue with predicted phases.

Confidence scale: LOW → MEDIUM → HIGH → VERY HIGH.
Each additional signal upgrades confidence by one tier.

#### Temperature Integration

- **Manual BBT Entry** — user enters oral temp each morning
- **Pixel Watch (overnight)** — reads `BasalBodyTemperatureRecord`
  from Health Connect during sleep sessions
- **Pixel Watch (daytime fallback)** — reads
  `BodyTemperatureRecord` if available; applies a +1°F
  correction offset, outlier rejection, and 3-day moving
  average smoothing to extract a usable signal from
  noisier daytime wrist data

Manual BBT is preferred (highest accuracy). Watch sleep
data is next best. Daytime watch data still contributes
to shift detection but with relaxed thresholds and
reduced confidence boost.

Includes a temperature chart with coverline and shift markers.

#### Symptom Tracking (5 Categories)

- **Cervical Mucus** — Dry, Sticky, Creamy, Watery, Egg White
  (with inline how-to tooltip)
- **Physical** — Cramps, Bloating, Headache, Breast Tenderness,
  Fatigue (multi-select)
- **Mood** — emoji row (Good, Neutral, Low, Irritable, Energetic)
- **Sex Drive** — Low, Normal, High
- **Sexual Activity** — Protected, Unprotected, None

#### Modes

- **Cycle Tracking** (default) — calendar, symptoms, phase display
- **Trying to Conceive (TTC)** — adds conception probability
  (clinical data by day relative to ovulation) and daily TTC tips

#### AI Insights

Gemini Nano generates phase/TTC-aware daily insights based on
cycle day, symptoms, temperature, and mode. Deterministic
phase-specific fallback tips when AI is unavailable.

### 5. Settings & Configuration

- **Profile** — nickname used in greeting and AI prompts
- **Health Connect** — connect/disconnect with permission management
- **Theme Mode** — 3-way picker: Light / Dark / System
- **Family Planning** — toggle (default: ON) + TTC mode switch
- **Daily Goals** — calorie and step targets
- **Body Measurements** — weight, height, age
- **Macro Goals** — protein, carbs, fat, sugar targets
  (AI-auto-generatable from profile)
- **Database Explorer** — developer tool for raw data inspection
- **Nuclear Reset** — clear all data and restart fresh

---

## Architecture

### Key Design Decisions

1. **Room database** — persistent relational storage with
   compile-time query verification, migration support, and
   Flow-based reactive queries. Schema at version 3.
2. **Dagger Hilt** — constructor-injected ViewModels and singleton
   services for clean dependency management and testability.
3. **Single-Activity architecture** — one `MainActivity` with a
   `NavHost` managing five composable destinations via bottom nav
   (Home, Diary, Steps, Family, Settings).
4. **Type-safe navigation** — `@Serializable` route objects
   (Navigation 2.8+) for compile-time route safety.
5. **Gemini Nano with graceful degradation** — every AI feature has
   a non-AI fallback. The app is fully usable without Gemini Nano.
6. **Family as app-within-an-app** — uses its own color palette
   (`FamilyColors`) while sharing Material 3 structure, typography,
   and interaction patterns with the rest of HopeHealth.

### Data Storage (Room v3)

| Table                 | Purpose                                  |
| --------------------- | ---------------------------------------- |
| `diary_item`          | Daily food diary entries with macros     |
| `food_item`           | Saved foods with usage tracking          |
| `user_settings`       | Singleton: goals, metrics, theme, family |
| `steps_record`        | Daily step counts                        |
| `cycle_record`        | Menstrual cycle records                  |
| `daily_cycle_log`     | Per-day symptom/flow/mood/temp entries   |
| `temperature_reading` | BBT and Health Connect temp readings     |

### Data Models

- **DiaryItem** — food log entry: id, name, calories, macros,
  timestamp, quantity, meal type
- **FoodItem** — saved food with nutrition data and usage tracking
- **NutritionResult** — search/scan result with per-serving data
- **ParsedFoodItem** — AI-parsed food item with confidence rating
- **MealType** — enum: BREAKFAST, LUNCH, DINNER, SNACK, OTHER
- **CycleRecordEntity** — cycle: start/end, length, ovulation
- **DailyCycleLogEntity** — per-day: flow, CM, symptoms, mood,
  sex drive, sexual activity, temperature
- **TemperatureReadingEntity** — temp with source
  (MANUAL, HEALTH_CONNECT_BBT, HEALTH_CONNECT_SKIN)
- **Cycle enums** — CyclePhase, FlowIntensity, CervicalMucusType,
  MoodType, SexDriveLevel, SexualActivityType,
  PredictionConfidence, FamilyMode, Symptom

---

## AI Integration

### Gemini Nano on Pixel 10 Pro

The app uses **Gemini Nano** — an on-device SLM running via the
ML Kit GenAI (Prompt) API. On the Pixel 10 Pro (Tensor G5),
inference is accelerated by the NPU/TPU.

Key characteristics:

- **Context window:** ~4,096 tokens (input + output)
- **Model management:** handled by Android AICore
- **Model download:** 1-3 hours over WiFi after device setup
- **Thermal awareness:** AICore manages throttling

Initialization flow:

1. `checkStatus()` — verify model availability
2. If downloadable, trigger `download()` and poll
3. Once available, `warmup()` for faster first inference
4. If unavailable, fall back to non-AI features

### Prompt Engineering

All prompts use Google's best practices for on-device SLMs:

- **Structured delimiters** — XML-style tags (`<instruction>`,
  `<user_data>`, `<examples>`, `<input>`)
- **Concise few-shot examples** — 1-2 per prompt
- **Explicit output format** — "Return ONLY JSON"
- **Edge case handling** — `NOT_FOUND` sentinel for label scans
- **Context-driven behavior** — deterministic tone selection
- **Conditional guidelines** — scenario-specific instructions

### AI Feature Matrix

| Feature          | Service               | Fallback            |
| ---------------- | --------------------- | ------------------- |
| Coach Tips       | `CoachTipUseCase`     | Rule-based tips     |
| Food Parsing     | `AiFoodParserService` | Feature unavailable |
| Nutrition Lookup | `AiFoodParserService` | Feature unavailable |
| Label Scanning   | `AiFoodParserService` | Feature unavailable |
| Macro Goal Gen   | `SettingsViewModel`   | Manual entry only   |
| Cycle Insights   | `FamilyViewModel`     | Phase-specific tips |

### Nutrition Search Pipeline

Food search uses a 3-tier fallback chain:

1. **USDA FoodData Central API** (primary) — structured data,
   `DEMO_KEY` rate-limited
2. **OpenFoodFacts** (secondary) — community product database
3. **DuckDuckGo scraping** (tertiary) — AI or regex extraction

### AI Reliability Notes

- SLM responses can be inconsistent for unusual foods
- JSON extraction uses regex to handle SLM quirks
- All numeric fields use `toDoubleOrNull()` with fallbacks
- `safeDouble()` strips unit suffixes from model output
- Coach tip format depends on `**bold**` markers

---

## Build & Deploy

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Google Pixel 10 Pro with USB debugging enabled
- ADB installed and in PATH

### Commands

```bash
# Build debug APK
make build

# Install on connected device
make phone

# Build + install + launch on running device
make run

# Build, sign, and deploy signed production APK
make deploy

# Run unit tests
make test

# Run UI navigation tests (emulator required)
make ui-test

# Run exhaustive interaction tests
make exhaustive-test

# Clean build artifacts
make clean
```

### Key Dependencies

| Dependency            | Version       | Purpose                |
| --------------------- | ------------- | ---------------------- |
| Jetpack Compose BOM   | 2026.04.01    | UI framework           |
| Material 3            | (via BOM)     | Design system          |
| Room                  | 2.8.4         | Local database         |
| Dagger Hilt           | 2.59.2        | Dependency injection   |
| Health Connect        | 1.1.0         | Steps, cals, temp      |
| ML Kit GenAI (Prompt) | 1.0.0-beta2   | On-device AI inference |
| kotlinx.serialization | 1.8.1         | JSON serialization     |
| OkHttp                | 5.0.0         | HTTP client for search |
| Google Fonts          | (via Compose) | Inter font family      |

---

## Known Limitations

1. **Pixel 10 Pro only** — not tested on other devices
2. **No data backup/export** — uninstall = data loss
3. **USDA `DEMO_KEY`** — rate-limited to 40 req/hour
4. **Web scraping fragility** — DDG HTML changes may break
   the 3rd-tier fallback
5. **No accessibility support** — content descriptions and
   screen reader optimizations are incomplete
6. **Pedometer resets on restart** — step counter baseline
   lost when service recreated (mitigated by Health Connect)
7. **No data sync** — single-device only
8. **Pixel IR thermometer** — inaccessible to third-party
   apps; temperature comes from manual BBT or Pixel Watch
   via Health Connect only
9. **Family predictions need data** — temperature-based and
   symptom-based predictions require 8+ days of logged
   readings to activate; the calendar method works
   immediately with no prior data
10. **Pixel Watch daytime temp is noisy** — the watch
    primarily records during sleep; daytime readings are
    usable but less accurate (smoothing and correction
    offsets are applied automatically)
