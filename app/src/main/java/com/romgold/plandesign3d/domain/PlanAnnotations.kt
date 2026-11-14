package com.romgold.plandesign3d.domain

enum class AnnotationKind(val title: String) {
    SOCKET("Розетка"), SWITCH("Выключатель"), LIGHT("Светильник"),
    WATER("Вода"), DRAIN("Канализация"), VENTILATION("Вентиляция")
}

data class PlanAnnotation(
    val id: String,
    val kind: AnnotationKind,
    val xMm: Int,
    val yMm: Int,
    val label: String = ""
) {
    init { require(xMm >= 0 && yMm >= 0) }
}
