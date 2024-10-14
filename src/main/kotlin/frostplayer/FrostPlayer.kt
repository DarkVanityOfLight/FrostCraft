package frostplayer

import Manager
import calculateHeatSources
import calculateInsulation
import isEnclosed
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.geometry.toSimple
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

enum class BodyTemperatureState {
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

    var state: BodyTemperatureState = BodyTemperatureState.NORMAL
        private set

    var diedFromFrost = false

    private var coldMessageInterval = 0
    private var frozen = false

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
        state = when {
            temperature < Manager.configParser.playerCriticalLowTemp -> BodyTemperatureState.SEVERE
            temperature < Manager.configParser.playerModerateEffectsThreshold -> BodyTemperatureState.MODERATE
            temperature < Manager.configParser.playerMildEffectsThreshold -> BodyTemperatureState.MILD
            else -> BodyTemperatureState.NORMAL
        }
    }

    /**
     * Applies the correct effects based on the player's current temperature state.
     */
    private fun applyTemperatureEffects(player: Player) {
        when (state) {
            BodyTemperatureState.SEVERE -> handleSevereCold(player)
            BodyTemperatureState.MODERATE -> handleModerateCold(player)
            BodyTemperatureState.MILD -> handleMildCold(player)
            BodyTemperatureState.NORMAL -> warmUp(player)
        }
    }

    /**
     * Handles the effects of severe cold (constant damage and freezing).
     */
    private fun handleSevereCold(player: Player) {
        applyColdDamage(player, Manager.configParser.playerTemperatureDamage * 2)
        freezePlayer(player)
    }

    /**
     * Handles the effects of moderate cold (slowness, weakness, and minor damage).
     */
    private fun handleModerateCold(player: Player) {
        applyColdEffects(player)
        applyColdDamage(player, Manager.configParser.playerTemperatureDamage)
    }

    /**
     * Handles the effects of mild cold (slowness, hunger).
     */
    private fun handleMildCold(player: Player) {
        applyColdEffects(player)
    }

    /**
     * Warms the player up and removes cold effects if they're in a normal temperature range.
     */
    private fun warmUp(player: Player) {
        if (frozen) unfreeze(player)
        removeColdEffects(player)
        coldMessageInterval = 0
    }

    /**
     * Applies damage to the player due to cold exposure.
     */
    private fun applyColdDamage(player: Player, damage: Float) {
        showColdMessage(player)

        val source = DamageSource.builder(DamageType.FREEZE).build()
        val event = EntityDamageEvent(player, EntityDamageEvent.DamageCause.FREEZE, source, damage.toDouble())
        Bukkit.getPluginManager().callEvent(event)

        if (!event.isCancelled) {
            if (player.health - damage <= 0) {
                player.health = 0.0
                diedFromFrost = true
            } else {
                player.health -= damage
            }
        }
    }

    /**
     * Shows a cold-related message on the player's action bar.
     */
    private fun showColdMessage(player: Player) {
        if (coldMessageInterval == 0) {
            player.sendActionBar(literalText("You are freezing!") {
                italic = true
                color = KColors.LIGHTBLUE
            })
        }

        coldMessageInterval++
        if (coldMessageInterval >= 20) coldMessageInterval = 0
    }

    /**
     * Applies the appropriate cold-related potion effects to the player based on their state.
     */
    private fun applyColdEffects(player: Player) {
        when (state) {
            BodyTemperatureState.MILD -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 0))
                player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 100, 0))
            }
            BodyTemperatureState.MODERATE -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 1))
                player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 1))
            }
            BodyTemperatureState.SEVERE -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 2))
                player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 2))
            }
            else -> {
                // No cold effects in NORMAL state
            }
        }
    }

    /**
     * Removes all cold-related potion effects.
     */
    fun removeColdEffects(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
        player.removePotionEffect(PotionEffectType.HUNGER)
    }

    /**
     * Freezes the player.
     */
    private fun freezePlayer(player: Player) {
        frozen = true
        player.freezeTicks = Int.MAX_VALUE
    }

    /**
     * Unfreezes the player.
     */
    fun unfreeze(player: Player) {
        frozen = false
        player.freezeTicks = 0
    }

    // ====== API USE ======

    fun targetTemperature(player: Player) : Float {
        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        val heatSources = emptySet<Material>()
        val zoneTemperature = Manager.climateManager.getTemperature(player.location)
        val insulationMaterials = if (isEnclosed) insulation else emptyList()

        return calculateInsulation(insulationMaterials) * (calculateHeatSources(heatSources) + zoneTemperature)
    }
}
