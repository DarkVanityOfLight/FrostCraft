package zones

import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import net.axay.kspigot.extensions.geometry.toVector
import org.bukkit.World

data class HeatZone(
    override val world: World,
    override val center: SimpleLocation3D,
    val radius: Float,
    val tempFunction: (Float, Float) -> Float
) : IZone {
    private var globalTemperature = 0.0f

    override fun contains(point: SimpleLocation3D): Boolean {
        val (cx, cy, cz) = center
        val (x, y, z) = point

        // Calculate squared distance from the point to the center
        val distanceSquared = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz)

        // Compare with the squared radius
        return distanceSquared <= radius * radius
    }

    override fun getTemperatureAt(point: SimpleLocation3D): Float {
        val distance = center.toVector().distance(point.toVector()).toFloat()

        return tempFunction(globalTemperature, distance)
    }

    override fun updateTemperature(globalTemperature: Float) {
        this.globalTemperature = globalTemperature
    }
}