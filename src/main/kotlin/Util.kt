import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import org.bukkit.Material
import org.bukkit.World

const val MAX_ENCLOSE_RANGE = 128

/**
 * Represents a coordinate in a 3D space.
 * This class only exists for efficiency reasons.
 *
 * @property x The x-coordinate.
 * @property y The y-coordinate.
 * @property z The z-coordinate.
 */
private data class Coordinate(val x: Int, val y: Int, val z: Int) {

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
    var insulation = 1.0f

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