import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.Location
import org.bukkit.Material

const val MAX_ENCLOSE_RANGE = 128

data class ClimateZone(val origin: Coordinate, val world: World, var temperature: Float) {}

data class Coordinate(val x: Int, val y: Int, val z: Int){
    constructor(location: Location) : this(location.blockX, location.blockY, location.blockZ)
    constructor(location3D: SimpleLocation3D) : this(location3D.x.toInt(), location3D.y.toInt(), location3D.z.toInt())
}


fun isEnclosed(location3D: SimpleLocation3D, world: World) : Pair<Boolean, Set<Material>> {
    val startLocation = Coordinate(location3D)


    val directions = listOf(
        Coordinate(1, 0, 0), Coordinate(-1, 0, 0),  // x-direction
        Coordinate(0, 1, 0), Coordinate(0, -1, 0),  // y-direction
        Coordinate(0, 0, 1), Coordinate(0, 0, -1)   // z-direction
    )

    val visited = mutableSetOf<Coordinate>()
    val stack = mutableListOf(Coordinate(location3D))
    val enclosingBlocks = mutableSetOf<Material>()
    //val enclosedBlocks = mutableSetOf<Coordinate>()
    var isEnclosed = true

    //enclosedBlocks.add(startLocation)

    while (stack.isNotEmpty()){
        //Bukkit.getLogger().info("Stack size: ${stack.size}")
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

    return Pair(isEnclosed, enclosingBlocks)
}

fun calculateInsulation(enclosingBlock: Set<Material>) : Float {
    var insulation = 0.0f

    for (block in enclosingBlock){
        when(block){
            Material.GLASS -> insulation += 0.1f
            //Material.WOOD -> insulation += 0.2f
            Material.STONE -> insulation += 0.3f
            Material.IRON_BLOCK -> insulation += 0.4f
            Material.DIAMOND_BLOCK -> insulation += 0.5f
            Material.EMERALD_BLOCK -> insulation += 0.6f
            Material.GOLD_BLOCK -> insulation += 0.7f
            Material.NETHERITE_BLOCK -> insulation += 0.8f
            else -> insulation += 0.0f
        }
    }

    return insulation / enclosingBlock.size
}

class ClimateManager {
    private var globaleTemperature = 0.0f
    private val climateZones = mutableSetOf<ClimateZone>()

    fun addClimateZone(climateZone: ClimateZone) {
        climateZones.add(climateZone)
    }

    fun createClimateZone(location3D: SimpleLocation3D, world: World) : Boolean{
        val (isEnclosed, enclosingBlocks) = isEnclosed(location3D, world)
        if (isEnclosed){
            val insulation = calculateInsulation(enclosingBlocks)
            val temperature = globaleTemperature + insulation

            val climateZone = ClimateZone(Coordinate(location3D), world, temperature)
            addClimateZone(climateZone)
            return true
        }else{
            return false
        }
    }
}
