# Blender + FreeCAD for Android

Public Android project: a touchscreen-oriented workspace for two applications only:
- **Blender** — 3D scene and mesh workflows.
- **FreeCAD** — parametric CAD and sketch workflows.

No third application, plugins, extensions, room planning, renovation tools, construction estimates, finish catalogues or unrelated custom modules are included.

## Current implementation status

The launcher contains two workspaces. The legacy room-planning screens, data models, exporters, tests and the unused SceneView dependency have been removed from the active source tree.

This is an early native Android prototype, **not** a port of the full Blender or FreeCAD desktop applications. Current scene/sketch controls are limited; geometric constraint solving, full 3D transform manipulation, native .blend/.FCStd import/export, undo/redo and broad workbench coverage are not yet implemented. Do not treat visual controls as proof of feature parity.

## Engineering rules

- Keep Blender and FreeCAD workflows visually and functionally distinct.
- Implement real document operations before presenting tools as functional.
- Persist and test edits across app restarts.
- Do not claim native format compatibility until round-trip tests pass.
- Run unit tests and Android builds after each coherent change.
- CI success does not replace installation and testing on the Unihertz Tank 2.

## Build

GitHub Actions builds a debug APK. Open the latest successful workflow run and download the `plan-design-3d-debug-apk` artifact.
