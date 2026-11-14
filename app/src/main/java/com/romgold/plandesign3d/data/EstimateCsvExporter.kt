package com.romgold.plandesign3d.data

import com.romgold.plandesign3d.domain.*

object EstimateCsvExporter {
    fun encode(project: ProjectDocument): String {
        val m = project.room.metrics()
        val floorQty = m.floorAreaM2 * (1 + project.floorWastePercent / 100.0)
        val wallQty = m.netWallAreaM2 * (1 + project.wallWastePercent / 100.0)
        val rates = project.workRates
        val electricalCount = project.annotations.count { it.kind == AnnotationKind.SOCKET || it.kind == AnnotationKind.SWITCH || it.kind == AnnotationKind.LIGHT }.toDouble()
        val plumbingCount = project.annotations.count { it.kind == AnnotationKind.WATER || it.kind == AnnotationKind.DRAIN }.toDouble()
        val rows = listOf(
            listOf("Материалы", project.floorFinish.name, "м²", floorQty, project.floorFinish.pricePerSquareMetre),
            listOf("Материалы", project.wallFinish.name, "м²", wallQty, project.wallFinish.pricePerSquareMetre),
            listOf("Работы", "Монтаж пола", "м²", m.floorAreaM2, rates.floorInstallationPerM2),
            listOf("Работы", "Подготовка стен", "м²", m.netWallAreaM2, rates.wallPreparationPerM2),
            listOf("Работы", "Отделка стен", "м²", m.netWallAreaM2, rates.wallFinishingPerM2),
            listOf("Работы", "Устройство потолка", "м²", m.floorAreaM2, rates.ceilingPerM2),
            listOf("Работы", "Электроточки", "шт.", electricalCount, rates.electricalPoint),
            listOf("Работы", "Сантехнические точки", "шт.", plumbingCount, rates.plumbingPoint)
        )
        val out = StringBuilder("\uFEFF")
        out.append("Раздел;Наименование;Ед.;Количество;Цена за единицу, руб.;Сумма, руб.\n")
        rows.forEach { row ->
            val quantity = row[3] as Double
            val price = row[4] as Double
            val sum = quantity * price
            out.append(row[0]).append(';').append(row[1]).append(';').append(row[2]).append(';')
                .append("%.3f".format(java.util.Locale.US, quantity)).append(';')
                .append("%.2f".format(java.util.Locale.US, price)).append(';')
                .append("%.2f".format(java.util.Locale.US, sum)).append('\n')
        }
        val total = rows.sumOf { (it[3] as Double) * (it[4] as Double) }
        out.append("ИТОГО;;;;;").append("%.2f".format(java.util.Locale.US, total)).append('\n')
        out.append("Примечание;Предварительный расчёт, не договорная смета;;;;\n")
        return out.toString()
    }
}
