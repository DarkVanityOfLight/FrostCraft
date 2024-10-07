import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import net.axay.kspigot.extensions.geometry.toSimple
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import zones.ClimateZone



/**
 * Manages the climate zones and global temperature.
 */
class ClimateManager {
    var globalTemperature = 0.0f
    private val climateZones = mutableSetOf<ClimateZone>()

    /**
     * Adds a climate zone to the manager.
     *
     * @param climateZone The climate zone to add.
     */
    fun addClimateZone(climateZone: ClimateZone) {
        climateZone.updateTemperature(globalTemperature)
        climateZones.add(climateZone)
    }


    /**
     * Creates and adds a climate zone to the manager.
     *
     * @param world The world the climate zone is in.
     * @param center The center location of the climate zone.
     * @param radius The radius of the climate zone.
     * @param tempFunc The function to calculate the temperature.
     */
    fun createClimateZone(world: World, center: Location, radius: Float, tempFunc: (Float) -> Float) {
        createClimateZone(world, center.toSimple(), radius, tempFunc)
    }

    /**
     * Creates and adds a climate zone to the manager.
     *
     * @param world The world the climate zone is in.
     * @param center The center SimpleLocation3D of the climate zone.
     * @param radius The radius of the climate zone.
     * @param tempFunc The function to calculate the temperature.
     */
    fun createClimateZone(world: World, center: SimpleLocation3D, radius: Float, tempFunc: (Float) -> Float) {
        addClimateZone(ClimateZone(world, center, radius, tempFunc))
    }


    /**
     * Removes a climate zone at the specified SimpleLocation3D.
     *
     * @param world The world the climate zone is in.
     * @param center The center SimpleLocation3D of the climate zone.
     */
    fun removeClimateZoneAt(world: World, center: SimpleLocation3D) {
        climateZones.removeIf { it.world == world && it.center == center }
    }

    /**
     * Updates the global temperature and all climate zones.
     *
     * @param temperature The new global temperature.
     */
    fun updateGlobalTemperature(temperature: Float) {
        globalTemperature = temperature
        climateZones.forEach {
            it.updateTemperature(temperature)
        }
    }

    /**
     * Gets the temperature at a specific location.
     *
     * @param location The location to get the temperature at.
     * @return The temperature at the location.
     */
    fun getTemperature(location: Location): Float {
        var maxTemp: Float = globalTemperature
        val simpleLocation = location.toSimple()

        for (zone in climateZones) {
            if (zone.contains(simpleLocation)) {
                val tmp = zone.getTemperatureAt(simpleLocation)
                if (tmp > maxTemp) {
                    maxTemp = tmp
                }
            }
        }

        return maxTemp
    }
}

