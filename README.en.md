English · [Русский](README.md)

# Task Organizer

An Android app: tabs with task lists and notes that can be dictated by voice. Speech is
recognised by the phone's built-in service — the same one the keyboard's voice input uses.

## Features

- Tabs at the top. One at first, "Tasks". Add as many as you like, with any names, colour and type:
  tasks with checkboxes or notes.
- Entries: add, edit, drag, move to another tab.
  - Entry actions via the "⋮" button: importance, edit, due date, move, delete. Tapping and
    holding the entry itself does nothing; tapping an empty area dismisses the cursor.
  - Swipe right — done, swipe left — delete, with undo.
- Importance: gray (normal), yellow, red (highest). By default red go on top, then yellow,
  then gray: a marked entry moves up immediately. Sorting by date added is available in the settings.
- Due date: a date and, optionally, a time.
- Reminders as notifications. In the settings:
  - how far in advance to remind, several times and custom intervals;
  - repeats after the due time until the task is done;
  - snooze duration;
  - when to remind about tasks with no time;
  - for which levels;
  - missed reminders after the phone was off;
  - alarm-clock mode;
  - separate sound and vibration for each level.
- Voice input through the Android system recogniser. You can choose the recognition service
  (Google, Samsung and others), the language and the "offline only" mode.
- Dictionary. English names that the recogniser writes in Russian are replaced with the right
  spelling, taking cases into account: "докера" → "Docker".
- Looks: 8 styles — Standard, Minimal, Soft, Sharp, Terminal, Paper, Compact, Neon. Each has its own
  font, icon set, shapes of buttons, switches and fields, look of entries, tabs, microphone,
  importance flags and background. Plus 10 colours including wallpaper colours, and light, dark and black themes.
- The fonts Manrope, Nunito, Oswald, Roboto Condensed, JetBrains Mono, Caveat and Exo 2 are
  bundled (SIL Open Font License, licence texts in `app/src/main/assets/licenses`).
- Search across all tabs, a 30-day trash, daily backups, JSON export and import.

## Install

Download `TaskOrganizer-*.apk` from the [Releases](https://github.com/Keavors/TaskOrginizer/releases)
page, open it on the phone and allow installation from this source.

## Build

Needs JDK 21 and the Android SDK (platform 37).

```bash
./gradlew testDebugUnitTest   # logic unit tests
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
./gradlew installRelease      # straight onto the connected phone (USB debugging)
```

## Documents

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — how the app is built and how to extend it.
