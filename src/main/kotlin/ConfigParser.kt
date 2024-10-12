import org.bukkit.Material
import org.bukkit.configuration.Configuration
import kotlin.properties.Delegates


// Defaults
const val DEFAULT_GLOBAL_BASE_TEMPERATURE = 0.0f
const val DEFAULT_PLAYER_TOLERANCE_BUFFER = 2.0f
const val DEFAULT_PLAYER_CRITICAL_LOW_TEMP = 263.15f
const val DEFAULT_PLAYER_MILD_EFFECTS_THRESHOLD = 273.15f
const val DEFAULT_PLAYER_MODERATE_EFFECTS_THRESHOLD = 268.15f
const val DEFAULT_PLAYER_GRADUAL_TEMPERATURE_CHANGE_RATE = 1f
const val DEFAULT_PLAYER_TEMPERATURE_DAMAGE = 0.1f
const val DEFAULT_PLAYER_BODY_TEMPERATURE = 310.15f
val DEFAULT_GENERATOR_HEAT_BLOCKS = setOf(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER)
val DEFAULT_GENERATOR_CONTROL_BLOCKS = setOf(Material.REDSTONE_BLOCK)
val DEFAULT_GENERATOR_STRUCTURE_BLOCKS = setOf(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK)
val DEFAULT_GENERATOR_DISSIPATION_BLOCKS = setOf(Material.DISPENSER, Material.DROPPER)
val DEFAULT_GENERATOR_EXHAUST_BLOCKS = setOf(Material.CAMPFIRE)
const val DEFAULT_HEAT_BLOCK_HEAT = 50.0f
const val DEFAULT_EXHAUST_HEAT = 25.0f
const val DEFAULT_DISSIPATION_BLOCK_RANGE = 5.0f
const val DEFAULT_DISSIPATION_BLOCK_STRESS = 10.0f
const val DEFAULT_CONTROL_BLOCKS_STRESS_DECREASE = 5.0f
const val DEFAULT_BASE_HEAT_RANGE = 10.0f
const val DEFAULT_STRUCTURE_BLOCK_DURABILITY = 10.0f


class ConfigParser(private val config: Configuration) {
    var globalBaseTemperature: Float by Delegates.notNull()
        private set

    var playerToleranceBuffer : Float by Delegates.notNull()
        private set

    var playerCriticalLowTemp : Float by Delegates.notNull()
        private set

    var playerMildEffectsThreshold : Float by Delegates.notNull()
        private set

    var playerModerateEffectsThreshold : Float by Delegates.notNull()
        private set

    var playerGradualTemperatureChangeRate : Float by Delegates.notNull()
        private set

    var playerTemperatureDamage : Float by Delegates.notNull()
        private set

    var playerBodyTemperature : Float by Delegates.notNull()
        private set

    lateinit var generatorHeatBlocks : Set<Material> // Increases Heat, increase fuel consumption
        private set
    lateinit var generatorControlBlocks : Set<Material> // Decreases stress
        private set
    lateinit var generatorStructureBlocks : Set<Material> // Increases durability
        private set
    lateinit var generatorDissipationBlocks : Set<Material> // Increases range, increases stress
        private set
    lateinit var generatorExhaustBlocks : Set<Material> // Decrease stress, increases heat/ Constants
        private set

    var generatorHeatBlockHeat : Float by Delegates.notNull()
        private set
    var generatorExhaustHeat : Float by Delegates.notNull()
        private set
    var generatorDissipationBlockRange : Float by Delegates.notNull()
        private set
    var generatorDissipationBlockStress : Float by Delegates.notNull()
        private set
    var generatorControlBlocksStressDecrease : Float by Delegates.notNull()
        private set
    var generatorBaseHeatRange : Float by Delegates.notNull()
        private set
    var generatorStructureBlockDurability : Float by Delegates.notNull()
        private set


    fun parseConfig() {

        config.get("globalBaseTemperature", DEFAULT_GLOBAL_BASE_TEMPERATURE).also { globalBaseTemperature = (it!! as Float) }

        config.getConfigurationSection("player")?.let { configurationSection ->
            configurationSection.get("toleranceBuffer", DEFAULT_PLAYER_TOLERANCE_BUFFER).also { playerToleranceBuffer = (it!! as Float) }
            configurationSection.get("criticalLowTemp", DEFAULT_PLAYER_CRITICAL_LOW_TEMP).also { playerCriticalLowTemp = (it!! as Float) }
            configurationSection.get("mildEffectsThreshold", DEFAULT_PLAYER_MILD_EFFECTS_THRESHOLD).also { playerMildEffectsThreshold = (it!! as Float) }
            configurationSection.get("moderateEffectsThreshold", DEFAULT_PLAYER_MODERATE_EFFECTS_THRESHOLD).also { playerModerateEffectsThreshold = (it!! as Float) }
            configurationSection.get("gradualTemperatureChangeRate", DEFAULT_PLAYER_GRADUAL_TEMPERATURE_CHANGE_RATE).also { playerGradualTemperatureChangeRate = (it!! as Float) }
            configurationSection.get("temperatureDamage", DEFAULT_PLAYER_TEMPERATURE_DAMAGE).also { playerTemperatureDamage = (it!! as Float) }
            configurationSection.get("bodyTemperature", DEFAULT_PLAYER_BODY_TEMPERATURE).also { playerBodyTemperature = (it!! as Float) }
        }

        config.getConfigurationSection("generator")?.let {configurationSection ->
            configurationSection.getList("heatBlocks", DEFAULT_GENERATOR_HEAT_BLOCKS.toList()).also { generatorHeatBlocks = (it!! as List<Material>).toSet() }
            configurationSection.getList("controlBlocks", DEFAULT_GENERATOR_CONTROL_BLOCKS.toList()).also { generatorControlBlocks = (it!! as List<Material>).toSet() }
            configurationSection.getList("structureBlocks", DEFAULT_GENERATOR_STRUCTURE_BLOCKS.toList()).also { generatorStructureBlocks = (it!! as List<Material>).toSet() }
            configurationSection.getList("dissipationBlocks", DEFAULT_GENERATOR_DISSIPATION_BLOCKS.toList()).also { generatorDissipationBlocks = (it!! as List<Material>).toSet() }
            configurationSection.getList("exhaustBlocks", DEFAULT_GENERATOR_EXHAUST_BLOCKS.toList()).also { generatorExhaustBlocks = (it!! as List<Material>).toSet() }

            configurationSection.get("heatBlockHeat", DEFAULT_HEAT_BLOCK_HEAT).also { generatorHeatBlockHeat = (it!! as Float) }
            configurationSection.get("exhaustHeat", DEFAULT_EXHAUST_HEAT).also { generatorExhaustHeat = (it!! as Float) }
            configurationSection.get("dissipationBlockRange", DEFAULT_DISSIPATION_BLOCK_RANGE).also { generatorDissipationBlockRange = (it!! as Float) }
            configurationSection.get("dissipationBlockStress", DEFAULT_DISSIPATION_BLOCK_STRESS).also { generatorDissipationBlockStress = (it!! as Float) }
            configurationSection.get("controlBlocksStressDecrease", DEFAULT_CONTROL_BLOCKS_STRESS_DECREASE).also { generatorControlBlocksStressDecrease = (it!! as Float) }
            configurationSection.get("baseHeatRange", DEFAULT_BASE_HEAT_RANGE).also { generatorBaseHeatRange = (it!! as Float) }
            configurationSection.get("structureBlockDurability", DEFAULT_STRUCTURE_BLOCK_DURABILITY).also { generatorStructureBlockDurability = (it!! as Float) }
        }

    }
}