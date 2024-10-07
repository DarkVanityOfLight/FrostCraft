package Generators

import ClimateManager
import Manager
import net.axay.kspigot.event.listen
import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import net.axay.kspigot.gui.GUIType
import net.axay.kspigot.gui.Slots
import net.axay.kspigot.gui.kSpigotGUI
import net.axay.kspigot.gui.openGUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.data.type.Furnace
import org.bukkit.entity.Item
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

enum class GeneratorState{
    ON, OFF, POWERING_ON, POWERING_OFF
}


class Generator(
    private var radius: Int,
    private val origin: SimpleLocation3D,
    private val controler: Block,
    private var consumption: Int,
    private var heat: Float,
    private var durability: Float,
    private val world: World
) {
    private val structure: MutableSet<Block> = mutableSetOf()
    private var stress: Float = 0.0f
    private val intakes: MutableSet<Block> = mutableSetOf()
    private var state: GeneratorState = GeneratorState.OFF
    private var consumerId: Int? = null

    init {


        listen<PlayerInteractEvent> {
            if (it.clickedBlock == controler){

                val gui = kSpigotGUI(GUIType.ONE_BY_FIVE) {
                    defaultPage = 1
                    title = Component.text("Generator control panel")

                    page(1) {
                        placeholder(Slots.Border, ItemStack(Material.BLACK_STAINED_GLASS_PANE))

                        if (state == GeneratorState.OFF){
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

    private fun generateZone() {
        Manager.climateManager.createClimateZone(world, origin, radius.toFloat()) { tmp -> tmp + heat }
    }

    private fun removeZone() {
        Manager.climateManager.removeClimateZoneAt(world, origin)
    }

    // TODO: Can be partly merged with has fuel for more efficiency
    private fun consumeFuel(){
        if (!hasFuel()) {
            powerOff()
            return
        }

        for (intake in intakes){
            if ((intake.state as Chest).blockInventory.contains(Material.COAL)){
                (intake.state as Chest).blockInventory.removeItem(ItemStack(Material.COAL, consumption))
                //stress += 0.1f
                break
            }

        }

    }

    private fun hasFuel() : Boolean {
        for (intake in intakes){
            if ((intake.state as Chest).blockInventory.contains(Material.COAL)){
                return true
            }
        }
        return false
    }

    fun powerOn() {
        if (!hasFuel()){ Bukkit.getLogger().info("No fuel"); return }

        Bukkit.getLogger().info("Powering on generator")

        //find every furnace and turn it on
        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val f : Furnace = (block.state.blockData as Furnace)
                f.isLit = true
                block.blockData = f
                block.state.update()
            }
        }

        // create the heat zone
        generateZone()

        // start fuel consumption
        consumerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, this::consumeFuel, 5, 20)

        if (consumerId == -1){
            Bukkit.getLogger().severe("Failed to start generator consumer")
        }

        this.state = GeneratorState.ON

    }

    fun powerOff() {
        Bukkit.getScheduler().cancelTask(consumerId!!)
        consumerId = null

        //find every furnace and turn it off
        structure.forEach { block ->
            if (block.type == Material.FURNACE) {
                val f : Furnace = (block.state.blockData as Furnace)
                f.isLit = false
                block.blockData = f
                block.state.update()
            }
        }

        removeZone()

        this.state = GeneratorState.OFF


    }

    fun findIntakes() {
        structure.forEach { block ->
            if (block.type == Material.CHEST) {
                intakes.add(block)
            }
        }
    }

    fun addBlock(block: Block) {
        structure.add(block)
    }

    fun addBlocks(blocks: List<Block>) {
        structure.addAll(blocks)
    }
}