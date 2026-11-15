# PLAN DESIGN 3D

Android project for adapting **Blender and FreeCAD** workflows to a touchscreen device.

## Agreed product scope

- Blender for 3D content creation and modeling.
- FreeCAD for parametric CAD and technical design.
- Two distinct workspaces with recognizable original-app interaction patterns, adapted for Android touch input.

There are **no third applications, plugins, extensions, renovation modules, room-planning features, estimates or custom feature packs** in the agreed product scope.

## Current state

The repository currently contains an earlier room-planning prototype. It is **not yet** a Blender/FreeCAD mobile implementation. The existing room geometry, furniture, finishes, estimates, engineering annotations and room preview are legacy prototype functionality and must not be treated as completed target features.

The latest CI build is a technical checkpoint only. Work is proceeding by auditing and replacing the prototype with verified Blender and FreeCAD workflows. Existing code should be removed only after a replacement is implemented and tested.

## Development rules

- Do not present placeholder controls as working tools.
- Do not claim native .blend, FreeCAD .FCStd, mesh or CAD format support until import/export round-trip tests pass.
- Keep Blender and FreeCAD workflows distinct.
- Run unit tests and Android builds after each coherent change.
- A successful CI build does not replace testing on the target Android device.

See [PRODUCT_SCOPE.md](docs/PRODUCT_SCOPE.md) for migration boundaries and release gates.

## Build

The Android debug APK is built by GitHub Actions. Open the latest successful workflow run and download the `plan-design-3d-debug-apk` artifact.