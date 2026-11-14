package com.romgold.plandesign3d.domain

data class WorkRates(
    val floorInstallationPerM2: Double = 900.0,
    val wallPreparationPerM2: Double = 450.0,
    val wallFinishingPerM2: Double = 700.0,
    val ceilingPerM2: Double = 800.0,
    val electricalPoint: Double = 1500.0,
    val plumbingPoint: Double = 3000.0
) {
    init {
        require(listOf(floorInstallationPerM2, wallPreparationPerM2, wallFinishingPerM2,
            ceilingPerM2, electricalPoint, plumbingPoint).all { it >= 0.0 })
    }
}
