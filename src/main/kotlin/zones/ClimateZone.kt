package zones

import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import org.bukkit.World

/**
 * Represents a climate zone in the world.
 *
 * @property world The world the climate zone is in.
 * @property center The center coordinate of the climate zone.
 * @property radius The radius of the climate zone.
 * @property tempFunction The function to calculate the temperature.
 */
data class ClimateZone(
    val world: World,
    val center: SimpleLocation3D,
    val radius: Float,
    val tempFunction: (Float) -> Float
) : IZone {
    private var temperature = 0.0f


    /**
     * Checks if a point is within the climate zone.
     *
     * @param point The coordinate to check.
     * @return True if the point is within the climate zone, false otherwise.
     */
    override fun contains(point: SimpleLocation3D): Boolean {
        val (cx, cy, cz) = center
        val (x, y, z) = point

        // Calculate squared distance from the point to the center
        val distanceSquared = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz)

        // Compare with the squared radius
        return distanceSquared <= radius * radius
    }

    /**
     * Updates the temperature of the climate zone.
     *
     * @param globalTemperature The new temperature.
     */
    override fun updateTemperature(globalTemperature: Float) {
        this.temperature = tempFunction(globalTemperature)
    }


    /**
     * Gets the current temperature of the climate zone.
     *
     * @return The current temperature.
     */
    override fun getTemperatureAt(point: SimpleLocation3D): Float {
        return temperature
    }
}
