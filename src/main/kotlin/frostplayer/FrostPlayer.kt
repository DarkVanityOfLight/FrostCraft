package frostplayer

import Manager
import calculateHeatSources
import calculateInsulation
import isEnclosed
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.geometry.toSimple
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

enum class BodyTemperatureState {
    WARM,
    NORMAL,
    MILD,
    MODERATE,
    SEVERE,
}

/**
 * Manages the player's temperature and applies effects based on the temperature.
 *
 * @property playerId The UUID of the player.
 * @property temperature The current temperature of the player.
 */
class FrostPlayer(var playerId: java.util.UUID) {
    var temperature: Float = Manager.configParser.playerBodyTemperature
        private set

    var bodyTemperatureState: BodyTemperatureState = BodyTemperatureState.NORMAL
        private set

    private var coldTicks = 0
    var bar : BossBar = BossBar.bossBar(literalText(""), 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
        private set


    /**
     * Main function that checks the player's temperature and applies effects.
     * Called asynchronously.
     *
     * @param player The player whose temperature is being checked.
     */
    fun checkTemperature(player: Player) {
        assert(player.uniqueId == playerId)

        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        val heatSources = emptySet<Material>()
        val zoneTemperature = Manager.climateManager.getTemperature(player.location)
        val insulationMaterials = if (isEnclosed) insulation else emptyList()

        updateTemperature(heatSources, insulationMaterials, zoneTemperature)

       Bukkit.getScheduler().callSyncMethod(Manager) {
           showTemperatureState()
       }

        if (player.gameMode == org.bukkit.GameMode.CREATIVE || player.gameMode == org.bukkit.GameMode.SPECTATOR) return

        // Apply effects on the next tick (sync)
        Bukkit.getScheduler().callSyncMethod(Manager) {
            applyTemperatureEffects(player)
        }
    }

    /**
     * Updates the player's temperature based on heat sources, insulation, and zone temperature.
     * Also updates the player's temperature state.
     */
    private fun updateTemperature(heatSources: Set<Material>, insulation: List<Material>, zoneTemperature: Float) {
        val insulationFactor = calculateInsulation(insulation)
        val heatSourceContribution = calculateHeatSources(heatSources)
        val targetTemperature = insulationFactor * (heatSourceContribution + zoneTemperature)

        temperature = adjustTemperatureTowardsTarget(targetTemperature)

        // Update state based on temperature
        updateTemperatureState()
    }

    /**
     * Adjusts the current temperature towards the target temperature.
     */
    private fun adjustTemperatureTowardsTarget(targetTemperature: Float): Float {
        val changeRate = Manager.configParser.playerGradualTemperatureChangeRate
        return when {
            temperature < targetTemperature -> temperature + changeRate
            temperature > targetTemperature -> temperature - changeRate
            else -> temperature
        }
    }

    /**
     * Updates the player's temperature state based on the current temperature.
     */
    private fun updateTemperatureState() {
        bodyTemperatureState = when {
            temperature < Manager.configParser.playerCriticalLowTemp -> BodyTemperatureState.SEVERE
            temperature < Manager.configParser.playerModerateEffectsThreshold -> BodyTemperatureState.MODERATE
            temperature < Manager.configParser.playerMildEffectsThreshold -> BodyTemperatureState.MILD
            temperature > Manager.configParser.playerWarmEffectsThreshold -> BodyTemperatureState.WARM
            else -> BodyTemperatureState.NORMAL
        }
    }

    /**
     * Applies the correct effects based on the player's current temperature state.
     */
    private fun applyTemperatureEffects(player: Player) {
        player.lockFreezeTicks(false)

        when (bodyTemperatureState) {
            BodyTemperatureState.SEVERE -> coldTicks += 3
            BodyTemperatureState.MODERATE -> coldTicks += 2
            BodyTemperatureState.MILD -> coldTicks += 1
            BodyTemperatureState.NORMAL -> coldTicks -= 2
            BodyTemperatureState.WARM -> coldTicks -= 3
        }

        coldTicks = coldTicks.coerceAtLeast(0).coerceAtMost(player.maxFreezeTicks)

        player.freezeTicks = coldTicks

        player.lockFreezeTicks(true)

    }

    private fun showTemperatureState() {
        val text = when (bodyTemperatureState){
            BodyTemperatureState.SEVERE -> "Freezing"
            BodyTemperatureState.MODERATE -> "Very cold"
            BodyTemperatureState.MILD -> "Cold"
            BodyTemperatureState.NORMAL -> "Comfortable"
            BodyTemperatureState.WARM -> "Warm"
        }

        val color = when (bodyTemperatureState){
            BodyTemperatureState.SEVERE -> BossBar.Color.BLUE
            BodyTemperatureState.MODERATE -> BossBar.Color.BLUE
            BodyTemperatureState.MILD -> BossBar.Color.BLUE
            BodyTemperatureState.NORMAL -> BossBar.Color.YELLOW
            BodyTemperatureState.WARM -> BossBar.Color.RED
        }

        bar.name(literalText("$text - $temperature°K"))
        bar.color(color)
    }

    /**
     * Unfreezes the player.
     */
    fun unfreeze(player: Player) {
        player.freezeTicks = 0
        this.coldTicks = 0
    }

    // ====== API USE ======

    /**
     * Returns the target temperature for the player based on their location and insulation.
     * Is best called async for less lag
     */
    fun targetTemperature(player: Player) : Float {
        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        val heatSources = emptySet<Material>()
        val zoneTemperature = Manager.climateManager.getTemperature(player.location)
        val insulationMaterials = if (isEnclosed) insulation else emptyList()

        return calculateInsulation(insulationMaterials) * (calculateHeatSources(heatSources) + zoneTemperature)
    }
}
