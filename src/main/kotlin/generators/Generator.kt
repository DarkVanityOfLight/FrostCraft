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
import org.bukkit.block.data.type.Campfire
import org.bukkit.block.Chest
import org.bukkit.block.data.type.Furnace
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import zones.HeatZone
import kotlin.math.max
import kotlin.math.pow

// TODO Melt snow in a radius around the generator, maybe do this in zones tho
// TODO Increase stress slowly over time and break generator if stress is too high
// TODO Add particle effects
// TODO Add overdrive

const val BASE_HEAT_RANGE = 10.0f
const val UPDATE_TICK_RATE = 20
const val TEMPERATURE_INCREASE_RATE = 50.0f

// Enums
enum class GeneratorState {
    ON, OFF
}

class InvalidStructureException : Exception("Invalid structure")

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
// TODO Increase stress slowly over time and break generator if stress is too high
class Generator(private val origin: Block) {
    private var stress: Float = 0.0f
    private var heat: Float = 0.0f
    private var state: GeneratorState = GeneratorState.OFF
    private var durability: Float = 0.0f
    private var consumption: Int = 1
    private var range = BASE_HEAT_RANGE
    private var runTaskId: Int? = null
    private var targetHeat: Float = 0.0f

    private val structure: MutableSet<Block> = mutableSetOf()
    private val intakes: MutableSet<Block> = mutableSetOf()
    private val heatBlocks: MutableSet<Block> = mutableSetOf()
    private val controlBlocks: MutableSet<Block> = mutableSetOf()
    private val structureBlocks: MutableSet<Block> = mutableSetOf()
    private val dissipationBlocks: MutableSet<Block> = mutableSetOf()
    private val exhaustBlocks: MutableSet<Block> = mutableSetOf()

    init {
        discoverStructure()
        if (!isValidStructure()) { throw InvalidStructureException() }
        listenForPlayerInteraction()

        listen<BlockBreakEvent> {
            if (it.block in structure) {
                removeBlockFromStructure(it.block)
                if(!isValidStructure()){
                    powerOff()
                }
            }
        }
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

    private fun isValidStructure(): Boolean {
        return controlBlocks.isNotEmpty() && heatBlocks.isNotEmpty() && intakes.isNotEmpty()
    }

    private fun addBlockToStructure(block: Block): Boolean {
        val added = when (block.type) {
            in Manager.configParser.generatorHeatBlocks -> heatBlocks.add(block)
            in Manager.configParser.generatorControlBlocks -> controlBlocks.add(block)
            in Manager.configParser.generatorStructureBlocks -> structureBlocks.add(block)
            in Manager.configParser.generatorDissipationBlocks -> dissipationBlocks.add(block)
            in Manager.configParser.generatorExhaustBlocks -> exhaustBlocks.add(block)
            Material.CHEST -> intakes.add(block)
            else -> false
        }
        if (added) structure.add(block)
        return added
    }

    private fun removeBlockFromStructure(block: Block): Boolean {
        val removed = when (block.type) {
            in Manager.configParser.generatorHeatBlocks -> heatBlocks.remove(block)
            in Manager.configParser.generatorControlBlocks -> controlBlocks.remove(block)
            in Manager.configParser.generatorStructureBlocks -> structureBlocks.remove(block)
            in Manager.configParser.generatorDissipationBlocks -> dissipationBlocks.remove(block)
            in Manager.configParser.generatorExhaustBlocks -> exhaustBlocks.remove(block)
            Material.CHEST -> intakes.remove(block)
            else -> false
        }
        if (removed) structure.remove(block)
        return removed
    }

    private fun listenForPlayerInteraction() {
        listen<PlayerInteractEvent> {
            if (controlBlocks.contains(it.clickedBlock)) {
                openControlPanel(it)
                it.isCancelled = true
            }
        }
    }

    // TODO Modularize this further
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
                        powerOn()
                    } else {
                        powerOff()
                    }
                    it.player.closeInventory()
                }

                pageChanger(Slots.RowOneSlotFive, ItemStack(Material.PAPER), 2, null, null)
            }
            
            page(2) {
                placeholder(Slots.Border, ItemStack(Material.BLACK_STAINED_GLASS_PANE))

                // Display fuel consumption
                val consumptionStack = ItemStack(Material.COAL)
                consumptionStack.itemMeta = consumptionStack.itemMeta.apply {
                    displayName(Component.text("Fuel"))
                    lore(listOf(Component.text("Consumption: $consumption")))
                }

                // Display heat
                val heatStack = ItemStack(Material.BLAZE_POWDER)
                heatStack.itemMeta = heatStack.itemMeta.apply {
                    displayName(Component.text("Heat"))
                    lore(listOf(Component.text("Heat: $heat °K")))
                }

                // Display stress
                val stressStack = ItemStack(Material.REDSTONE)
                stressStack.itemMeta = stressStack.itemMeta.apply {
                    displayName(Component.text("Stress"))
                    lore(listOf(Component.text("Stress: $stress")))
                }

                // Display durability
                val durabilityStack = ItemStack(Material.DIAMOND)
                durabilityStack.itemMeta = durabilityStack.itemMeta.apply {
                    displayName(Component.text("Durability"))
                    lore(listOf(Component.text("Durability: $durability")))
                }

                // Display range
                val rangeStack = ItemStack(Material.BLAZE_ROD)
                rangeStack.itemMeta = rangeStack.itemMeta.apply {
                    displayName(Component.text("Range"))
                    lore(listOf(Component.text("Range: $range")))
                }

                // Display all stacks
                placeholder(Slots.RowOneSlotOne, consumptionStack)
                placeholder(Slots.RowOneSlotTwo, heatStack)
                placeholder(Slots.RowOneSlotThree, stressStack)
                placeholder(Slots.RowOneSlotFour, durabilityStack)
                placeholder(Slots.RowOneSlotFive, rangeStack)

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
        heat = when {
            heat < targetHeat -> (heat + TEMPERATURE_INCREASE_RATE).coerceAtMost(targetHeat)
            heat > targetHeat -> (heat - TEMPERATURE_INCREASE_RATE).coerceAtLeast(targetHeat)
            else -> heat  // Already at the target
        }

        if (heat == 0.0f && state == GeneratorState.OFF) {
            runTaskId?.let { Bukkit.getScheduler().cancelTask(it) }
             runTaskId = null
        }

    }

    private fun calculateMaxHeat() : Float {
        return heatBlocks.size * Manager.configParser.generatorHeatBlockHeat + exhaustBlocks.size * Manager.configParser.generatorExhaustHeat
    }

    private fun calculateStress() {
        stress = max( dissipationBlocks.size * Manager.configParser.generatorDissipationBlockStress - controlBlocks.size * Manager.configParser.generatorControlBlocksStressDecrease, 0.0f)
    }

    private fun calculateRange() {
        range = BASE_HEAT_RANGE + dissipationBlocks.size * Manager.configParser.generatorDissipationBlockRange
    }

    private fun calculateDurability() {
        durability = structureBlocks.size * Manager.configParser.generatorStructureBlockDurability
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

    // TODO check that we have enough fuel
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

    fun powerOn() : Boolean {
        discoverStructure()
        if (!hasFuel()) {
            return false
        }

        if(!isValidStructure()){
            return false
        }


        state = GeneratorState.ON

        targetHeat = calculateMaxHeat()

        setBlocksLit(true)

        runTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, this::run, 5, UPDATE_TICK_RATE.toLong())
        return runTaskId != -1

    }

    fun powerOff() : Boolean {
        // runTaskId?.let { Bukkit.getScheduler().cancelTask(it) }
        // runTaskId = null

        targetHeat = 0.0f
        setBlocksLit(false)
        state = GeneratorState.OFF
        return true
    }

    private fun setBlocksLit(lit: Boolean) {
        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val furnace = block.state.blockData as Furnace
                furnace.isLit = lit
                block.blockData = furnace
                block.state.update()
            }
        }

        structure.forEach { block ->
            if (block.type == Material.CAMPFIRE) {
                val campfire = block.state.blockData as Campfire
                campfire.isLit = lit
                block.blockData = campfire
                block.state.update()
            }
        }
    }

}