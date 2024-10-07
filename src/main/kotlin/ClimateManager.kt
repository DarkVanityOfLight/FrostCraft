import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

const val MAX_ENCLOSE_RANGE = 128

/**
 * Represents a coordinate in a 3D space.
 *
 * @property x The x-coordinate.
 * @property y The y-coordinate.
 * @property z The z-coordinate.
 */
data class Coordinate(val x: Int, val y: Int, val z: Int) {

    /**
     * Constructs a Coordinate from a Bukkit Location.
     *
     * @param location The Bukkit Location.
     */
    constructor(location: Location) : this(location.blockX, location.blockY, location.blockZ)

    /**
     * Constructs a Coordinate from a SimpleLocation3D.
     *
     * @param location3D The SimpleLocation3D.
     */
    constructor(location3D: SimpleLocation3D) : this(location3D.x.toInt(), location3D.y.toInt(), location3D.z.toInt())
}


// TODO: Optimize return fields to shave of a few bytes
// TODO: Somehow this is off by one block, seems like player location x is actually player location x - 1 don't ask why
/**
 * Determines if a location is enclosed and returns the enclosing materials.
 *
 * @param location3D The location to check.
 * @param world The world the location is in.
 * @return A pair where the first value is true if the location is enclosed, and the second value is a list of enclosing materials.
 */
fun isEnclosed(location3D: SimpleLocation3D, world: World): Pair<Boolean, List<Material>> {
    val startLocation = Coordinate(location3D)


    val directions = listOf(
        Coordinate(1, 0, 0), Coordinate(-1, 0, 0),  // x-direction
        Coordinate(0, 1, 0), Coordinate(0, -1, 0),  // y-direction
        Coordinate(0, 0, 1), Coordinate(0, 0, -1)   // z-direction
    )

    val visited = mutableSetOf<Coordinate>()
    val stack = mutableListOf(Coordinate(location3D))
    val enclosingBlocks = mutableListOf<Material>()

    while (stack.isNotEmpty()) {
        val current = stack.removeAt(stack.size - 1)

        if (current !in visited) {
            visited.add(current)

            // If we reach the boundary and still find air, it's not enclosed
            if (
                current.x == startLocation.x - MAX_ENCLOSE_RANGE || current.x == startLocation.x + MAX_ENCLOSE_RANGE ||
                current.y == startLocation.y - MAX_ENCLOSE_RANGE || current.y == startLocation.y + MAX_ENCLOSE_RANGE ||
                current.z == startLocation.z - MAX_ENCLOSE_RANGE || current.z == startLocation.z + MAX_ENCLOSE_RANGE
            ) {
                return Pair(false, enclosingBlocks)
            }

            for (dir in directions) {
                val newX = current.x + dir.x
                val newY = current.y + dir.y
                val newZ = current.z + dir.z

                val newLocation = Coordinate(newX, newY, newZ)

                val block = world.getBlockAt(newX, newY, newZ)
                if (newLocation !in visited && block.type.isAir) {
                    stack.add(newLocation)
                } else if (newLocation !in visited) {
                    enclosingBlocks.add(block.type)
                }

            }

        }
    }

    return Pair(true, enclosingBlocks)
}

/**
 * Calculates the insulation value based on the enclosing materials.
 *
 * @param enclosingBlock The list of enclosing materials.
 * @return The calculated insulation value.
 */
fun calculateInsulation(enclosingBlock: List<Material>): Float {
    var insulation = 0.0f

    if (enclosingBlock.isEmpty()) {
        return insulation
    }

    for (block in enclosingBlock) {
        insulation += when (block) {
            Material.GLASS -> 1f
            Material.STONE -> 1.5f
            Material.IRON_BLOCK -> 2f
            Material.DIAMOND_BLOCK -> 3f
            Material.EMERALD_BLOCK -> 3f
            Material.GOLD_BLOCK -> 0.5f
            Material.NETHERITE_BLOCK -> 4f
            else -> 0.0f
        }
    }

    return insulation / enclosingBlock.size
}


/**
 * Calculates the heat value based on the heat sources.
 *
 * @param heatSources The set of heat source materials.
 * @return The calculated heat value.
 */
fun calculateHeatSources(heatSources: Set<Material>): Float {
    var heat = 0.0f

    for (block in heatSources) {
        when (block) {
            Material.LAVA -> heat += 0.5f
            Material.TORCH -> heat += 0.1f
            Material.CAMPFIRE -> heat += 0.3f
            Material.SOUL_CAMPFIRE -> heat -= 0.3f
            Material.FIRE -> heat += 0.7f
            else -> heat += 0.0f
        }
    }

    return heat
}


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
    val center: Coordinate,
    val radius: Float,
    val tempFunction: (Float) -> Float
) {
    private var temperature = 0.0f


    /**
     * Checks if a point is within the climate zone.
     *
     * @param point The coordinate to check.
     * @return True if the point is within the climate zone, false otherwise.
     */
    fun contains(point: Coordinate): Boolean {
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
     * @param temperature The new temperature.
     */
    fun updateTemperature(temperature: Float) {
        this.temperature = tempFunction(temperature)
    }


    /**
     * Gets the current temperature of the climate zone.
     *
     * @return The current temperature.
     */
    fun getTemperature(): Float {
        return temperature
    }
}

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
     * @param center The center coordinate of the climate zone.
     * @param radius The radius of the climate zone.
     * @param tempFunc The function to calculate the temperature.
     */
    fun createClimateZone(world: World, center: Coordinate, radius: Float, tempFunc: (Float) -> Float) {
        addClimateZone(ClimateZone(world, center, radius, tempFunc))
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
        createClimateZone(world, Coordinate(center), radius, tempFunc)
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
        createClimateZone(world, Coordinate(center), radius, tempFunc)
    }

    /**
     * Removes a climate zone at the specified coordinate.
     *
     * @param world The world the climate zone is in.
     * @param center The center coordinate of the climate zone.
     */
    fun removeClimateZoneAt(world: World, center: Coordinate) {
        climateZones.removeIf { it.world == world && it.center == center }
    }

    /**
     * Removes a climate zone at the specified SimpleLocation3D.
     *
     * @param world The world the climate zone is in.
     * @param center The center SimpleLocation3D of the climate zone.
     */
    fun removeClimateZoneAt(world: World, center: SimpleLocation3D) {
        removeClimateZoneAt(world, Coordinate(center))
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
        val point = Coordinate(location)

        var maxTemp: Float = globalTemperature

        for (zone in climateZones) {
            if (zone.contains(point)) {
                if (zone.getTemperature() > maxTemp) {
                    maxTemp = zone.getTemperature()
                }
            }
        }

        return maxTemp
    }
}

