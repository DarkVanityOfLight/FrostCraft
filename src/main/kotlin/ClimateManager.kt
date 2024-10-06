import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.Location
import org.bukkit.Material

const val MAX_ENCLOSE_RANGE = 128

data class Coordinate(val x: Int, val y: Int, val z: Int){
    constructor(location: Location) : this(location.blockX, location.blockY, location.blockZ)
    constructor(location3D: SimpleLocation3D) : this(location3D.x.toInt(), location3D.y.toInt(), location3D.z.toInt())
}


// TODO: Optimize return fields to shave of a few bytes
// TODO: Somehow this is off by one block, seems like player location x is actually player location x - 1 don't ask why
fun isEnclosed(location3D: SimpleLocation3D, world: World) : Pair<Boolean, List<Material>> {
    val startLocation = Coordinate(location3D)


    val directions = listOf(
        Coordinate(1, 0, 0), Coordinate(-1, 0, 0),  // x-direction
        Coordinate(0, 1, 0), Coordinate(0, -1, 0),  // y-direction
        Coordinate(0, 0, 1), Coordinate(0, 0, -1)   // z-direction
    )

    val visited = mutableSetOf<Coordinate>()
    val stack = mutableListOf(Coordinate(location3D))
    val enclosingBlocks = mutableListOf<Material>()

    while (stack.isNotEmpty()){
        val current = stack.removeAt(stack.size - 1)

        if (current !in visited){
            visited.add(current)

            // If we reach the boundary and still find air, it's not enclosed
            if (
                current.x == startLocation.x - MAX_ENCLOSE_RANGE || current.x == startLocation.x + MAX_ENCLOSE_RANGE ||
                current.y == startLocation.y - MAX_ENCLOSE_RANGE || current.y == startLocation.y + MAX_ENCLOSE_RANGE ||
                current.z == startLocation.z - MAX_ENCLOSE_RANGE || current.z == startLocation.z + MAX_ENCLOSE_RANGE){
                return Pair(false, enclosingBlocks)
            }

            for (dir in directions){
                val newX = current.x + dir.x
                val newY = current.y + dir.y
                val newZ = current.z + dir.z

                val newLocation = Coordinate(newX, newY, newZ)

                val block = world.getBlockAt(newX.toInt(), newY.toInt(), newZ.toInt())
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

fun calculateInsulation(enclosingBlock: List<Material>) : Float {
    var insulation = 0.0f

    if (enclosingBlock.isEmpty()){
        return insulation
    }

    for (block in enclosingBlock){
        insulation += when(block){
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

fun calculateHeatSources(heatSources: Set<Material>) : Float {
    var heat = 0.0f

    for (block in heatSources){
        when(block){
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

data class ClimateZone(val world : World, val center: Coordinate, val radius: Float, val tempFunction: (Float) -> Float){
    private var temperature = 0.0f


    fun contains(point: Coordinate): Boolean {
        val (cx, cy, cz) = center
        val (x, y, z) = point

        // Calculate squared distance from the point to the center
        val distanceSquared = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz)

        // Compare with the squared radius
        return distanceSquared <= radius * radius
    }

    fun updateTemperature(temperature: Float){
        this.temperature = tempFunction(temperature)
    }

    fun getTemperature() : Float {
        return temperature
    }
}

class ClimateManager {
    var globalTemperature = 0.0f
    private val climateZones = mutableSetOf<ClimateZone>()

    fun addClimateZone(climateZone: ClimateZone) {
        climateZones.add(climateZone)
    }

    fun createClimateZone(world: World, center: Coordinate, radius: Float, tempFunc: (Float) -> Float) {
        addClimateZone(ClimateZone(world, center, radius, tempFunc))
    }

    fun createClimateZone(world: World, center: Location, radius: Float, tempFunc: (Float) -> Float) {
        createClimateZone(world, Coordinate(center), radius, tempFunc)
    }

    fun createClimateZone(world: World, center: SimpleLocation3D, radius: Float, tempFunc: (Float) -> Float) {
        createClimateZone(world, Coordinate(center), radius, tempFunc)
    }

    fun updateGlobalTemperature(temperature: Float){
        globalTemperature = temperature
        climateZones.forEach {
            it.updateTemperature(temperature)
        }
    }

    fun getTemperature(location: Location) : Float {
        val point = Coordinate(location)
        var temperature = globalTemperature

        for (zone in climateZones){
            if (zone.contains(point)){
                temperature = zone.getTemperature()
            }
        }

        return temperature
    }
}

