package generators

import Manager
import net.axay.kspigot.event.listen
import net.axay.kspigot.extensions.geometry.toSimple
import net.axay.kspigot.gui.GUIType
import net.axay.kspigot.gui.Slots
import net.axay.kspigot.gui.kSpigotGUI
import net.axay.kspigot.gui.openGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.data.type.Furnace
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import zones.HeatZone
import kotlin.math.pow


val HEAT_BLOCKS = setOf(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER) // Increases Heat, increase fuel consumption
val CONTROL_BLOCKS = setOf(Material.REDSTONE_BLOCK) // Decreases stress
val STRUCTURE_BLOCKS = setOf(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK) // Increases durability
val DISSIPATION_BLOCKS = setOf(Material.DISPENSER, Material.DROPPER) // Increases range, increases stress
val EXHAUST_BLOCKS = setOf(Material.CAMPFIRE) // Decrease stress, increases heat

const val HEAT_BLOCK_HEAT = 50.0f
const val EXHAUST_HEAT = 25.0f
const val DISSIPATION_BLOCK_RANGE = 5.0f
const val DISSIPATION_BLOCK_STRESS = 10.0f
const val CONTROL_BLOCKS_STRESS_DECREASE = 5.0f
const val BASE_HEAT_RANGE = 10.0f
const val STRUCTURE_BLOCK_DURABILITY = 10.0f

/**
 * Represents the state of the generator.
 */
enum class GeneratorState {
    ON, OFF, POWERING_ON, POWERING_OFF
}

fun getNeighbors(block: Block): List<Block> {
    val neighbors = mutableListOf<Block>()

    val x = block.x
    val y = block.y
    val z = block.z

    neighbors.add(block.world.getBlockAt(x + 1, y, z))
    neighbors.add(block.world.getBlockAt(x - 1, y, z))
    neighbors.add(block.world.getBlockAt(x, y + 1, z))
    neighbors.add(block.world.getBlockAt(x, y - 1, z))
    neighbors.add(block.world.getBlockAt(x, y, z + 1))
    neighbors.add(block.world.getBlockAt(x, y, z - 1))

    return neighbors
}

/**
 * Represents a generator in the game.
 *
 * @property radius The radius of the generator's effect.
 * @property origin The origin location of the generator.
 * @property controller The block that controls the generator.
 * @property consumption The fuel consumption rate of the generator.
 * @property heat The heat generated by the generator.
 * @property durability The durability of the generator.
 * @property world The world the generator is in.
 */
class Generator(
    private val origin: Block,
) {
    private var stress: Float = 0.0f
    private var heat: Float = 0.0f
    private var state: GeneratorState = GeneratorState.OFF
    private var durability: Float = 0.0f
    private var consumption: Int = 1
    private var range = 10.0f

    private var consumerId: Int? = null


    // Structure
    private val structure: MutableSet<Block> = mutableSetOf()
    private val intakes: MutableSet<Block> = mutableSetOf()
    private var heatBlocks: MutableSet<Block> = mutableSetOf()
    private var controlBLocks: MutableSet<Block> = mutableSetOf()
    private var structureBlocks : MutableSet<Block> = mutableSetOf()
    private var dissipationBlocks : MutableSet<Block> = mutableSetOf()
    private var exhaustBlocks : MutableSet<Block> = mutableSetOf()


    private fun discoverStructure() {
        // DFS to find all blocks in the structure
        val stack = mutableListOf(origin)
        val visited = mutableSetOf<Block>()

        while (stack.isNotEmpty()) {
            val current = stack.removeAt(stack.size - 1)
            visited.add(current)

            if (addBlockToStructure(current)) {
                for (neighbor in getNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        stack.add(neighbor)
                    }
                }
            }
        }
    }

    /**
     * Adds a block to the appropriate structure set based on its type.
     *
     * @param block The block to add.
     * @return True if the block was added to any set, false otherwise.
     */
    private fun addBlockToStructure(block: Block): Boolean {
        return when (block.type) {
            in HEAT_BLOCKS -> heatBlocks.add(block)
            in CONTROL_BLOCKS -> controlBLocks.add(block)
            in STRUCTURE_BLOCKS -> structureBlocks.add(block)
            in DISSIPATION_BLOCKS -> dissipationBlocks.add(block)
            in EXHAUST_BLOCKS -> exhaustBlocks.add(block)
            Material.CHEST -> intakes.add(block)
            else -> false
        }.also { added ->
            if (added) structure.add(block)
        }
    }


    init {

        discoverStructure()

        // TODO Listen for block placing events to add blocks to the generator's structure


        // Listen for player interaction events to open the generator control panel
        listen<PlayerInteractEvent> {
            if (controlBLocks.contains(it.clickedBlock)) {
                val gui = kSpigotGUI(GUIType.ONE_BY_FIVE) {
                    defaultPage = 1
                    title = Component.text("Generator control panel")

                    page(1) {
                        placeholder(Slots.Border, ItemStack(Material.BLACK_STAINED_GLASS_PANE))

                        if (state == GeneratorState.OFF) {
                            val stack = ItemStack(Material.GREEN_WOOL)
                            stack.itemMeta = stack.itemMeta.apply {
                                displayName(Component.text("Power on"))
                            }
                            button(Slots.RowOneSlotThree, stack) {
                                findIntakes()
                                powerOn()
                                it.player.closeInventory()
                            }
                        } else {
                            val stack = ItemStack(Material.RED_WOOL)
                            stack.itemMeta = stack.itemMeta.apply {
                                displayName(Component.text("Power off"))
                            }
                            button(Slots.RowOneSlotThree, stack) {
                                powerOff()
                                it.player.closeInventory()
                            }
                        }
                    }
                }

                it.player.openGUI(gui)
                it.isCancelled = true
            }
        }
    }

    // Called every second
    private fun run() {

        // Calculate heat, // TODO do this dynamically for startup/shutdown
        this.heat = heatBlocks.size * HEAT_BLOCK_HEAT + exhaustBlocks.size * EXHAUST_HEAT

        // Calculate stress
        this.stress = dissipationBlocks.size * DISSIPATION_BLOCK_STRESS - controlBLocks.size * CONTROL_BLOCKS_STRESS_DECREASE

        // Calculate range
        this.range = BASE_HEAT_RANGE + dissipationBlocks.size * DISSIPATION_BLOCK_RANGE

        // Calculate durability
        this.durability = structureBlocks.size * STRUCTURE_BLOCK_DURABILITY

        // Update zone
        updateZone()

        // Consume fuel
        consumeFuel()

    }

    private fun updateZone() {
        removeZone()
        generateZone()
    }

    /**
     * Generates a climate zone around the generator.
     */
    private fun generateZone() {
        // TODO calculate this by range
        val heatZone = HeatZone(origin.world, origin.location.toSimple(), range) { globalTemp, distance ->
            return@HeatZone globalTemp + (heat / distance.toDouble().pow(2.0).toFloat())
        }

        Manager.climateManager.addClimateZone(heatZone)

    }

    /**
     * Removes the climate zone around the generator.
     */
    private fun removeZone() {
        Manager.climateManager.removeClimateZoneAt(origin.world, origin.location.toSimple())
    }

    /**
     * Consumes fuel from the generator's intakes.
     * If no fuel is available, the generator is powered off.
     */
    private fun consumeFuel() {
        if (!hasFuel()) {
            powerOff()
            return
        }

        for (intake in intakes) {
            if ((intake.state as Chest).blockInventory.contains(Material.COAL)) {
                (intake.state as Chest).blockInventory.removeItem(ItemStack(Material.COAL, consumption))
                break
            }
        }
    }

    /**
     * Checks if the generator has fuel in its intakes.
     *
     * @return True if fuel is available, false otherwise.
     */
    private fun hasFuel(): Boolean {
        for (intake in intakes) {
            if ((intake.state as Chest).blockInventory.contains(Material.COAL)) {
                return true
            }
        }
        return false
    }

    /**
     * Powers on the generator.
     * Turns on all furnaces in the structure and starts fuel consumption.
     */
    fun powerOn() {
        if (!hasFuel()) {
            Bukkit.getLogger().info("No fuel")
            return
        }

        Bukkit.getLogger().info("Powering on generator")

        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val f: Furnace = (block.state.blockData as Furnace)
                f.isLit = true
                block.blockData = f
                block.state.update()
            }
        }

        // Calculate heat, range, stress

        generateZone()

        // TODO Change this to not only consuming fuel but doing everything around the generator, eg increasing stress, heat, etc
        consumerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, this::run, 5, 20)

        if (consumerId == -1) {
            Bukkit.getLogger().severe("Failed to start generator consumer")
        }

        // TODO Move this up into the repeating task
        this.state = GeneratorState.ON
    }

    /**
     * Powers off the generator.
     * Turns off all furnaces in the structure and stops fuel consumption.
     */
    fun powerOff() {
        Bukkit.getScheduler().cancelTask(consumerId!!)
        consumerId = null

        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val f: Furnace = (block.state.blockData as Furnace)
                f.isLit = false
                block.blockData = f
                block.state.update()
            }
        }

        removeZone()

        this.state = GeneratorState.OFF
    }

    /**
     * Finds and adds all chest blocks in the structure to the intakes set.
     */
    fun findIntakes() {
        structure.forEach { block ->
            if (block.type == Material.CHEST) {
                intakes.add(block)
            }
        }
    }
}