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

// Constants
val HEAT_BLOCKS = setOf(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER)
val CONTROL_BLOCKS = setOf(Material.REDSTONE_BLOCK)
val STRUCTURE_BLOCKS = setOf(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK)
val DISSIPATION_BLOCKS = setOf(Material.DISPENSER, Material.DROPPER)
val EXHAUST_BLOCKS = setOf(Material.CAMPFIRE)

const val HEAT_BLOCK_HEAT = 50.0f
const val EXHAUST_HEAT = 25.0f
const val DISSIPATION_BLOCK_RANGE = 5.0f
const val DISSIPATION_BLOCK_STRESS = 10.0f
const val CONTROL_BLOCKS_STRESS_DECREASE = 5.0f
const val BASE_HEAT_RANGE = 10.0f
const val STRUCTURE_BLOCK_DURABILITY = 10.0f

// Enums
enum class GeneratorState {
    ON, OFF, POWERING_ON, POWERING_OFF
}

// Utility functions
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

// Generator class
class Generator(private val origin: Block) {
    private var stress: Float = 0.0f
    private var heat: Float = 0.0f
    private var state: GeneratorState = GeneratorState.OFF
    private var durability: Float = 0.0f
    private var consumption: Int = 1
    private var range = 10.0f
    private var consumerId: Int? = null

    private val structure: MutableSet<Block> = mutableSetOf()
    private val intakes: MutableSet<Block> = mutableSetOf()
    private val heatBlocks: MutableSet<Block> = mutableSetOf()
    private val controlBlocks: MutableSet<Block> = mutableSetOf()
    private val structureBlocks: MutableSet<Block> = mutableSetOf()
    private val dissipationBlocks: MutableSet<Block> = mutableSetOf()
    private val exhaustBlocks: MutableSet<Block> = mutableSetOf()

    init {
        discoverStructure()
        listenForPlayerInteraction()
    }

    private fun discoverStructure() {
        val stack = mutableListOf(origin)
        val visited = mutableSetOf<Block>()

        while (stack.isNotEmpty()) {
            val current = stack.removeAt(stack.size - 1)
            visited.add(current)

            if (addBlockToStructure(current)) {
                stack.addAll(getNeighbors(current).filter { it !in visited })
            }
        }
    }

    private fun addBlockToStructure(block: Block): Boolean {
        val added = when (block.type) {
            in HEAT_BLOCKS -> heatBlocks.add(block)
            in CONTROL_BLOCKS -> controlBlocks.add(block)
            in STRUCTURE_BLOCKS -> structureBlocks.add(block)
            in DISSIPATION_BLOCKS -> dissipationBlocks.add(block)
            in EXHAUST_BLOCKS -> exhaustBlocks.add(block)
            Material.CHEST -> intakes.add(block)
            else -> false
        }
        if (added) structure.add(block)
        return added
    }

    private fun listenForPlayerInteraction() {
        listen<PlayerInteractEvent> {
            if (controlBlocks.contains(it.clickedBlock)) {
                openControlPanel(it)
                it.isCancelled = true
            }
        }
    }

    private fun openControlPanel(event: PlayerInteractEvent) {
        val gui = kSpigotGUI(GUIType.ONE_BY_FIVE) {
            defaultPage = 1
            title = Component.text("Generator control panel")

            page(1) {
                placeholder(Slots.Border, ItemStack(Material.BLACK_STAINED_GLASS_PANE))

                val stack = ItemStack(if (state == GeneratorState.OFF) Material.GREEN_WOOL else Material.RED_WOOL)
                stack.itemMeta = stack.itemMeta.apply {
                    displayName(Component.text(if (state == GeneratorState.OFF) "Power on" else "Power off"))
                }
                button(Slots.RowOneSlotThree, stack) {
                    if (state == GeneratorState.OFF) {
                        findIntakes()
                        powerOn()
                    } else {
                        powerOff()
                    }
                    it.player.closeInventory()
                }
            }
        }
        event.player.openGUI(gui)
    }

    private fun run() {
        calculateHeat()
        calculateStress()
        calculateRange()
        calculateDurability()
        updateZone()
        consumeFuel()
    }

    private fun calculateHeat() {
        heat = heatBlocks.size * HEAT_BLOCK_HEAT + exhaustBlocks.size * EXHAUST_HEAT
    }

    private fun calculateStress() {
        stress = dissipationBlocks.size * DISSIPATION_BLOCK_STRESS - controlBlocks.size * CONTROL_BLOCKS_STRESS_DECREASE
    }

    private fun calculateRange() {
        range = BASE_HEAT_RANGE + dissipationBlocks.size * DISSIPATION_BLOCK_RANGE
    }

    private fun calculateDurability() {
        durability = structureBlocks.size * STRUCTURE_BLOCK_DURABILITY
    }

    private fun updateZone() {
        removeZone()
        generateZone()
    }

    private fun generateZone() {
        val heatZone = HeatZone(origin.world, origin.location.toSimple(), range) { globalTemp, distance ->
            globalTemp + (heat / distance.toDouble().pow(2.0).toFloat())
        }
        Manager.climateManager.addClimateZone(heatZone)
    }

    private fun removeZone() {
        Manager.climateManager.removeClimateZoneAt(origin.world, origin.location.toSimple())
    }

    private fun consumeFuel() {
        if (!hasFuel()) {
            powerOff()
            return
        }

        for (intake in intakes) {
            val chest = intake.state as Chest
            if (chest.blockInventory.contains(Material.COAL)) {
                chest.blockInventory.removeItem(ItemStack(Material.COAL, consumption))
                break
            }
        }
    }

    private fun hasFuel(): Boolean {
        return intakes.any { (it.state as Chest).blockInventory.contains(Material.COAL) }
    }

    fun powerOn() {
        if (!hasFuel()) {
            Bukkit.getLogger().info("No fuel")
            return
        }

        Bukkit.getLogger().info("Powering on generator")
        setFurnacesLit(true)
        generateZone()
        consumerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, this::run, 5, 20)
        if (consumerId == -1) {
            Bukkit.getLogger().severe("Failed to start generator consumer")
        }
        state = GeneratorState.ON
    }

    fun powerOff() {
        consumerId?.let { Bukkit.getScheduler().cancelTask(it) }
        consumerId = null
        setFurnacesLit(false)
        removeZone()
        state = GeneratorState.OFF
    }

    private fun setFurnacesLit(lit: Boolean) {
        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val furnace = block.state.blockData as Furnace
                furnace.isLit = lit
                block.blockData = furnace
                block.state.update()
            }
        }
    }

    fun findIntakes() {
        structure.forEach { block ->
            if (block.type == Material.CHEST) {
                intakes.add(block)
            }
        }
    }
}