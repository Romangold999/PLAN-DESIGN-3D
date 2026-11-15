# Product scope: Blender + FreeCAD for Android

## Non-negotiable boundaries

This repository adapts exactly two applications for Android touchscreens:

1. **Blender** — 3D content creation and modeling.
2. **FreeCAD** — parametric CAD and technical design.

The two workspaces must retain the recognizable visual language and core concepts of their respective original applications.

## Explicit exclusions

- No third application.
- No plugins, extensions, add-ons or third-party feature packs.
- No renovation, interior-design, construction-estimate, finish-catalogue, room-planning or unrelated custom modules.
- No invented functionality and no placeholder controls presented as completed native tools.

## Current status (2026-11-15)

The old room-planning source modules, exporters, tests and room-specific 3D preview have been removed. The app now launches into two separate prototype workspaces: Blender and FreeCAD.

This is **not full Blender or FreeCAD parity**. Current limitations include:
- No native .blend or .FCStd import/export round trip.
- No complete Blender viewport navigation, edit mode, mesh operators, materials, lighting, camera or render pipeline.
- No FreeCAD geometric constraint solver, full Part Design/Part workbenches, assemblies or technical drawing pipeline.
- No complete undo/redo history.
- Device-level interaction and performance remain to be validated on the target Android device.

## Migration and implementation rules

1. Keep the two applications separate in navigation, visual language and document models.
2. Implement operations against editable scene/CAD data, not decorative controls.
3. Verify persistence after closing and reopening documents.
4. Add unit tests for domain operations and file-format round trips.
5. Do not advertise a format or workbench until verified.
6. Build debug and release APKs and test on a physical Android device.

## Release gates

- Distinct, recognizable Blender and FreeCAD interfaces.
- Functional scene/sketch editing and touch navigation.
- Reliable document persistence and undo/redo.
- Verified import/export for each advertised format.
- Successful CI tests, APK installation and physical-device testing.
