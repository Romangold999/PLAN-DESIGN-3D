package com.romgold.plandesign3d.data

import com.romgold.plandesign3d.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProjectCodec {
    fun encode(project: ProjectDocument): String {
        val walls = JSONArray().apply {
            project.room.walls.forEach { w -> put(JSONObject()
                .put("sx", w.start.xMm).put("sy", w.start.yMm)
                .put("ex", w.end.xMm).put("ey", w.end.yMm)
                .put("thickness", w.thicknessMm).put("height", w.heightMm)) }
        }
        val openings = JSONArray().apply {
            project.room.openings.forEach { o -> put(JSONObject()
                .put("wall", o.wallIndex).put("offset", o.offsetMm).put("width", o.widthMm)
                .put("height", o.heightMm).put("sill", o.sillHeightMm).put("type", o.type.name)) }
        }
        val furniture = JSONArray().apply {
            project.furniture.forEach { f -> put(JSONObject()
                .put("id", f.id).put("kind", f.kind.name).put("x", f.xMm).put("y", f.yMm)
                .put("width", f.widthMm).put("depth", f.depthMm).put("rotation", f.rotationDegrees)) }
        }
        fun material(m: FinishMaterial) = JSONObject().put("id", m.id).put("name", m.name)
            .put("category", m.category.name).put("color", m.colorArgb).put("price", m.pricePerSquareMetre)
        return JSONObject().put("schemaVersion", 3).put("id", project.id).put("name", project.name)
            .put("walls", walls).put("openings", openings).put("furniture", furniture)
            .put("floorFinish", material(project.floorFinish)).put("wallFinish", material(project.wallFinish))
            .put("floorWaste", project.floorWastePercent).put("wallWaste", project.wallWastePercent)
            .put("surfaceLayout", JSONObject()
                .put("tileWidth", project.surfaceLayout.tileWidthMm)
                .put("tileHeight", project.surfaceLayout.tileHeightMm)
                .put("grout", project.surfaceLayout.groutMm)
                .put("rotation", project.surfaceLayout.rotationDegrees)
                .put("waste", project.surfaceLayout.wastePercent)
                .put("pattern", project.surfaceLayout.pattern.name)).toString()
    }

    fun decode(json: String): ProjectDocument {
        val root = JSONObject(json)
        val wallsJson = root.getJSONArray("walls")
        val walls = (0 until wallsJson.length()).map { i ->
            val w = wallsJson.getJSONObject(i)
            WallSegment(Point2(w.getInt("sx"), w.getInt("sy")), Point2(w.getInt("ex"), w.getInt("ey")),
                w.optInt("thickness", 120), w.optInt("height", 2700))
        }
        val openingJson = root.optJSONArray("openings") ?: JSONArray()
        val openings = (0 until openingJson.length()).map { i ->
            val o = openingJson.getJSONObject(i)
            Opening(o.getInt("wall"), o.getInt("offset"), o.getInt("width"), o.getInt("height"),
                o.optInt("sill", 0), OpeningType.valueOf(o.optString("type", "DOOR")))
        }
        val furnitureJson = root.optJSONArray("furniture") ?: JSONArray()
        val furniture = (0 until furnitureJson.length()).map { i ->
            val f = furnitureJson.getJSONObject(i)
            val kind = FurnitureKind.valueOf(f.getString("kind"))
            FurnitureItem(f.optString("id", UUID.randomUUID().toString()), kind, f.getInt("x"), f.getInt("y"),
                f.optInt("width", kind.widthMm), f.optInt("depth", kind.depthMm), f.optInt("rotation", 0))
        }
        fun readMaterial(key: String, fallback: FinishMaterial): FinishMaterial {
            val m = root.optJSONObject(key) ?: return fallback
            return FinishMaterial(m.optString("id", fallback.id), m.optString("name", fallback.name),
                FinishCategory.valueOf(m.optString("category", fallback.category.name)),
                m.optLong("color", fallback.colorArgb), m.optDouble("price", fallback.pricePerSquareMetre))
        }
        val layoutJson = root.optJSONObject("surfaceLayout") ?: JSONObject()
        val layout = SurfaceLayout(
            layoutJson.optInt("tileWidth", 600), layoutJson.optInt("tileHeight", 600),
            layoutJson.optInt("grout", 2), layoutJson.optInt("rotation", 0),
            layoutJson.optDouble("waste", 10.0),
            SurfacePattern.valueOf(layoutJson.optString("pattern", SurfacePattern.STRAIGHT.name))
        )
        return ProjectDocument(root.optString("id", UUID.randomUUID().toString()), root.optString("name", "Мой проект"),
            RoomPlan(walls, openings), furniture, readMaterial("floorFinish", defaultFloorMaterials.first()),
            readMaterial("wallFinish", defaultWallMaterials.first()), root.optDouble("floorWaste", 10.0),
            root.optDouble("wallWaste", 10.0), layout)
    }
}
