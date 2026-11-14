# PLAN DESIGN 3D

Android room-planning and interior-layout editor. Kotlin, Jetpack Compose and SceneView/Filament.

## Current implementation (0.4.0)

- Room geometry in integer millimetres; rectangular and coordinate-defined polygon rooms.
- Closed-contour, self-intersection and opening-overlap validation.
- 2D plan with pinch zoom and pan.
- Doors and windows with offsets, widths, heights and sill levels.
- Procedural 3D room preview with openings, furniture, selected floor/wall colours and orbit camera controls.
- Furniture and sanitary fixtures: add, move by coordinates, rotate in 90-degree steps and remove.
- Finish catalogue, editable ARGB colours and editable material prices.
- Surface layout parameters: tile format, grout, pattern and waste; approximate piece count.
- Wall elevations showing openings.
- Engineering plan points for sockets, switches, lights, water, drainage and ventilation.
- Preliminary floor/wall material quantity and cost estimate.
- Multiple local projects, autosave, undo/redo.
- JSON project import/export and SVG plan export.
- Offline-first storage; no account or paid API required.

## Build

Open with Android Studio (JDK 17, Android SDK 36, Gradle 8.13, Kotlin 2.2.20) or use the GitHub Actions workflow.

The CI workflow runs `:app:assembleDebug`, `:app:testDebugUnitTest` and publishes a debug APK artifact.

## Current limitations / next milestones

This is an actively developed MVP, not yet a full professional CAD/BIM package. Detailed arbitrary wall editing, wall thickness/height per segment, niches and recesses, texture-image materials, lighting controls, ceiling plans, electrical circuit routing, plumbing pipe routing, collision detection, labour-rate estimates, PDF/print sheets and DWG/DXF exchange still need dedicated implementation and device validation. Tile counts are area-based estimates and do not yet simulate every cut or pattern boundary.
