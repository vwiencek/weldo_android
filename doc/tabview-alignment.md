# Android alignment with web/iOS: bottom-nav shell + agenda Home

Brought the Android app in line with the web app (and the iOS TabView work): the
single combined home became a **tabbed shell with an agenda Home**, matching the
web left rail (Home / All items / Statistics + always-present capture).

**Status: implemented (2026-07-11).** `:app:assembleDebug` is green (Temurin 21).
Not run on a device/emulator (no AVD present; backend also needed to reach the
signed-in shell).

## What changed

- **`ui/MainShell.kt`** (new) — the signed-in shell: a Material 3 bottom
  `NavigationBar` (Home / Items / Stats) + the `CaptureBar` as a compact
  `＋ Capture` accessory floating above it + a Settings gear in the top bar. This
  is the web left rail translated to the Android-idiomatic bottom bar (a literal
  side rail isn't a phone pattern). `WeldoNavHost` HOME route now renders it.
- **`ui/home/HomeScreen.kt`** — split into two content-only composables:
  - `AgendaScreen` (Home tab) — open task/project/reminder items bucketed by due
    date: Overdue → Today → Tomorrow → This week → This month → Later, empty
    buckets dropped, undated → Later. Greeting shows the web overdue/today copy.
    No stats strip, no capture bar (both moved out).
  - `ItemsScreen` (All-items tab) — the previous search + filter chips + mixed
    list.
- **`ui/home/HomeViewModel.kt`** — `DueBucket` / `AgendaBucket` + `agenda()`,
  porting web `core/due.ts` `bucketFor` (Monday-based weeks) via
  `WeldoTime.epochMillis`. One shared VM drives Home + Items.
- **`ui/capture/CaptureBar.kt`** — added `compact: Boolean`. Compact = a single
  `＋ Capture` button whose `DropdownMenu` offers Add item / Use AI. The full
  two-button row stays for the in-list use (project screen).
- **`ui/stats/StatsScreen.kt`** — extracted `StatsPane(contentPadding)` (the tab
  body); `StatsScreen(onBack)` still wraps it for the Settings → Statistics link.

## Build / run

Xcode-free; Android needs a JDK 17–21 (repo builds with Temurin 21):

```sh
cd android
JAVA_HOME=~/.sdkman/candidates/java/21.0.9-tem ./gradlew :app:assembleDebug
```

To screenshot the shell you need an emulator/AVD (none currently created) and a
signed-in session (dev token via Login → Advanced, which needs the backend up).
