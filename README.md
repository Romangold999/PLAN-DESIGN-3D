# PLAN DESIGN 3D

Android room-planning and interior-layout editor built with Kotlin, Jetpack Compose and SceneView/Filament.

## Current implementation (0.4.1)

- Room geometry in integer millimetres; rectangular and coordinate-defined polygon rooms.
- Closed-contour, self-intersection and opening-overlap validation.
- 2D plan with pinch zoom, pan, true polygon floor fill and surface grid.
- Doors and windows with offsets, widths, heights and sill levels.
- Per-wall thickness and height settings.
- Procedural 3D room preview with openings, furniture, selected floor/wall colours and orbit camera controls.
- Furniture and sanitary fixtures: add, move by coordinates, rotate in 90-degree steps and remove.
- Finish catalogue, editable ARGB colours and editable material prices.
- Surface layout parameters: tile format, grout, pattern and waste; approximate piece count.
- Wall elevations showing openings.
- Engineering plan points for sockets, switches, lights, water, drainage and ventilation.
- Preliminary estimate with editable labour rates and engineering point costs.
- Multiple local projects, autosave, undo/redo.
- JSON project import/export, SVG plan export and CSV estimate export.
- Offline-first storage; no account or paid API required.

## Build

JDK 17, Android SDK 36, Gradle 8.13, Kotlin 2.2.20. GitHub Actions runs `:app:assembleDebug`, `:app:testDebugUnitTest` and publishes the debug APK.

## Remaining work before a professional CAD/BIM release

Niches and recessed wall geometry, texture-image materials, adjustable 3D lighting, ceiling plans, electrical circuit and plumbing route drawing, wall/floor collision and cut simulation, detailed work packages and printable PDF sheets, DWG/DXF exchange, and real-device validation are not yet complete. Tile counts are area-based estimates; they do not yet simulate every cut or pattern boundary.
