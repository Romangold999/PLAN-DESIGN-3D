package com.romgold.plandesign3d.data

import com.romgold.plandesign3d.domain.ProjectDocument

object ProjectSvgExporter {
    fun encode(project: ProjectDocument): String {
        val minX = project.room.walls.minOf { minOf(it.start.xMm, it.end.xMm) }
        val minY = project.room.walls.minOf { minOf(it.start.yMm, it.end.yMm) }
        val width = project.room.boundsWidthMm.coerceAtLeast(1)
        val height = project.room.boundsDepthMm.coerceAtLeast(1)
        val margin = 300
        val out = StringBuilder()
        out.append("""<svg xmlns="http://www.w3.org/2000/svg" width="${width + margin * 2}mm" height="${height + margin * 2}mm" viewBox="0 0 ${width + margin * 2} ${height + margin * 2}">""")
        out.append("""<rect width="100%" height="100%" fill="#ffffff"/>""")
        out.append("""<g transform="translate(${margin - minX},${margin - minY})" fill="none" stroke-linecap="square" stroke-linejoin="miter">""")
        out.append("""<polygon points="${project.room.walls.joinToString(" ") { "${it.start.xMm},${it.start.yMm}" }}" fill="#f1eee7" stroke="none"/>""")
        project.room.walls.forEachIndexed { index, wall ->
            out.append("""<line x1="${wall.start.xMm}" y1="${wall.start.yMm}" x2="${wall.end.xMm}" y2="${wall.end.yMm}" stroke="#315c78" stroke-width="${wall.thicknessMm}"/>""")
            project.room.openings.filter { it.wallIndex == index }.forEach { opening ->
                val dx = (wall.end.xMm - wall.start.xMm).toDouble() / wall.lengthMm
                val dy = (wall.end.yMm - wall.start.yMm).toDouble() / wall.lengthMm
                val x1 = wall.start.xMm + dx * opening.offsetMm
                val y1 = wall.start.yMm + dy * opening.offsetMm
                val x2 = wall.start.xMm + dx * (opening.offsetMm + opening.widthMm)
                val y2 = wall.start.yMm + dy * (opening.offsetMm + opening.widthMm)
                val color = if (opening.type.name == "WINDOW") "#35a6c8" else "#ffffff"
                out.append("""<line x1="$x1" y1="$y1" x2="$x2" y2="$y2" stroke="$color" stroke-width="${wall.thicknessMm + 20}"/>""")
            }
        }
        project.annotations.forEach { annotation ->
            val color = when (annotation.kind) {
                com.romgold.plandesign3d.domain.AnnotationKind.SOCKET -> "#2d8a57"
                com.romgold.plandesign3d.domain.AnnotationKind.SWITCH -> "#da9a27"
                com.romgold.plandesign3d.domain.AnnotationKind.LIGHT -> "#e8c547"
                com.romgold.plandesign3d.domain.AnnotationKind.WATER -> "#2d9ccb"
                com.romgold.plandesign3d.domain.AnnotationKind.DRAIN -> "#536b9b"
                com.romgold.plandesign3d.domain.AnnotationKind.VENTILATION -> "#8b65a8"
            }
            out.append("""<circle cx="${annotation.xMm}" cy="${annotation.yMm}" r="70" fill="$color" stroke="#263238" stroke-width="18"/>""")
            val label = escapeXml(annotation.label.ifBlank { annotation.kind.title })
            out.append("""<text x="${annotation.xMm + 90}" y="${annotation.yMm}" fill="#263238" stroke="none" font-size="120">$label</text>""")
        }
        project.furniture.forEach { item ->
            val rotated = item.rotationDegrees % 180 != 0
            val footprintWidth = if (rotated) item.depthMm else item.widthMm
            val footprintDepth = if (rotated) item.widthMm else item.depthMm
            val centerX = item.xMm + footprintWidth / 2.0
            val centerY = item.yMm + footprintDepth / 2.0
            out.append("""<g transform="translate($centerX,$centerY) rotate(${item.rotationDegrees})">""")
            out.append("""<rect x="${-item.widthMm / 2.0}" y="${-item.depthMm / 2.0}" width="${item.widthMm}" height="${item.depthMm}" fill="#c18a64" stroke="#704d3a" stroke-width="20"/>""")
            val label = escapeXml(item.kind.title)
            out.append("""<text x="0" y="0" fill="#2f251e" stroke="none" text-anchor="middle" dominant-baseline="middle" font-size="140">$label</text>""")
            out.append("</g>")
        }
        out.append("</g></svg>")
        return out.toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
