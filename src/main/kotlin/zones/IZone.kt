package zones

import net.axay.kspigot.extensions.geometry.SimpleLocation3D

interface IZone {
    val world: World
    val center: SimpleLocation3D

    fun contains(point: SimpleLocation3D): Boolean
    fun getTemperatureAt(point: SimpleLocation3D): Float
    fun updateTemperature(globalTemperature: Float)
}