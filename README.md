# FROST OS 26 — Native Edition

An Android 12-compatible, iOS-inspired native experience for the itel A60s.

## Project direction

FROST is an Android customization app, not iOS firmware. It will use Android-supported APIs and explicit user-granted permissions. The original Android lock screen, authentication and recovery remain in control.

## Planned modules

- Native full-screen home experience
- iOS-inspired lock-screen presentation within Android security limits
- Custom Control Centre with supported system settings shortcuts
- Notification Listener integration (user opt-in)
- Dynamic Island-style overlay (draw-over-apps permission, user opt-in)
- App discovery and launching

## Build

The repository will include a GitHub Actions workflow to build a debug APK. The initial target is Android 12 / API 31 and Java-based Android development for broad tool compatibility.

## Device

Target test device: itel A60s (A662LM), Android 12, ARMv7 userspace.
