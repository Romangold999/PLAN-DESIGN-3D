# PLAN DESIGN 3D

Independent Android room-planning and interior-design application. Not part of PLAN MASTER 3D.

## Current milestone
Android foundation, touch-first 2D room prototype, millimetre dimensions, local save, and CI APK build. 3D is not claimed complete; SceneView/Filament compatibility will be tested before integration.

## Build
JDK 17 and Android SDK 35:
`gradle :app:assembleDebug`

GitHub Actions builds a debug APK and uploads it as an artifact.

## Device target
Android 14 / ARM64 (Unihertz Tank 2), touch-first and offline-first.

## Roadmap
1. Android foundation and installable APK.
2. Geometry core, walls, openings, validation and tests.
3. 2D editor: numeric dimensions, pan/zoom, snapping, undo/redo.
4. 3D scene from shared geometry, full orbit camera.
5. Versioned local projects, export and device validation.
