# Product scope: Blender + FreeCAD for Android

## Non-negotiable boundaries

This repository is for an Android adaptation of exactly two desktop applications:

1. **Blender** — 3D content creation: scene/object workflow, mesh modeling, transforms, viewport navigation, materials, lighting, cameras, rendering, sculpting, animation and node-based workflows where they can be implemented on Android.
2. **FreeCAD** — parametric CAD: document/body workflow, sketches, geometric constraints, Part Design, Part, assemblies, technical drawings and other native workbenches as they are implemented.

The Android UI must preserve the recognizable visual language and working concepts of each original application while adapting layout, tool access and gestures for touchscreens.

## Explicit exclusions

- No third application is part of the product.
- No plugins, extensions, add-ons or third-party feature packs.
- No custom renovation, interior-design, construction-estimate, finish-catalogue, room-planning or engineering-point modules unless a feature is demonstrably part of Blender or FreeCAD itself.
- Do not invent capabilities or label a placeholder as a working Blender/FreeCAD tool.

## Current repository status (2026-11-15)

The current Android codebase is still a room-planning prototype. Its room polygon, furniture, finishes, estimates, engineering annotations and room-specific 3D preview are not a faithful implementation of Blender or FreeCAD. The latest CI build is useful as a build-system checkpoint, not as a completed release of the agreed product.

## Migration principles

1. Audit each existing screen, model, exporter and dependency against Blender or FreeCAD. Keep only components that directly support a verified native workflow or reusable app infrastructure.
2. Establish separate Blender and FreeCAD workspaces. Do not merge their tool concepts into a generic custom editor.
3. Implement real operations before presenting controls: state changes must modify the document/scene and survive save/reopen.
4. Keep file formats and interoperability explicit. Never claim native .blend, FreeCAD .FCStd, mesh or CAD format support until round-trip tests pass.
5. Build and run unit tests after each coherent change. A successful GitHub build is not proof of correct behavior on the user's Android device.
6. Do not remove legacy code until its replacement is implemented and verified; do not expose legacy room-planning features as part of the agreed product.

## Release gates

- Separate, recognizable Blender and FreeCAD workspaces.
- Real editable 3D scene/model data, not only a rendered room preview.
- Undo/redo and reliable project persistence.
- Touch-friendly viewport navigation and selection.
- Native workflows implemented incrementally with tests.
- Verified import/export for every format advertised.
- Debug and release APK builds, followed by real-device testing.