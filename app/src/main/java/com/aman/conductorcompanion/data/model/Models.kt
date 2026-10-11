package com.aman.conductorcompanion.data.model

data class RouteData(
    val routeNumber: String,
    val direction: String,
    val stops: List<Stop>
)

data class Stop(
    val id: Int,
    val nameEn: String,
    val nameLoc: String,
    val stageFare: Int,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class PassengerType {
    ADULT, CHILD, SENIOR, PASS
}
