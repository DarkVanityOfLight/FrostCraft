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

const val dmg = 0.1
const val bodyTemp = 310.0f

const val toleranceBuffer = 2.0f  // Buffer before effects are applied
const val criticalLowTemp = 263.15f  // Critical low temperature threshold
const val mildEffectsThreshold = 273.15f  // Threshold for mild effects
const val moderateEffectsThreshold = 268.15f  // Threshold for moderate effects
const val gradualTemperatureChangeRate = 1f

/**
 * Manages the player's temperature and applies effects based on the temperature.
 *
 * @property playerId The UUID of the player.
 * @property temperature The current temperature of the player.
 *
 */
class FrostPlayer(var playerId: java.util.UUID) {
    var temperature: Float = bodyTemp
    var diedFromFrost = false
    private var coldMessageInterval = 0
    private var frozen = false


    /**
     * Updates the player's temperature based on heat sources, insulation, and zone temperature.
     *
     * @param heatSources The set of materials that act as heat sources.
     * @param insulation The list of materials that provide insulation.
     * @param zoneTemperature The temperature of the zone the player is in.
     */
    private fun updateTemperature(heatSources: Set<Material>, insulation: List<Material>, zoneTemperature: Float) {
        val insulationFactor = calculateInsulation(insulation)
        val heatSourceContribution = calculateHeatSources(heatSources)

        // Gradually adjust the player's temperature
        val targetTemperature = insulationFactor * (heatSourceContribution + zoneTemperature)

        if (temperature < targetTemperature) {
            temperature += gradualTemperatureChangeRate
        } else if (temperature > targetTemperature) {
            temperature -= gradualTemperatureChangeRate
        }
    }

    // API use
    fun targetTemperature(player: Player): Float {

        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        val insulationFactor = if (isEnclosed) {
            calculateInsulation(insulation)
        } else {
            1.0f
        }

        val heatSourceContribution = calculateHeatSources(emptySet())

        return insulationFactor * (heatSourceContribution + Manager.climateManager.getTemperature(player.location))
    }


    /**
     * Checks the player's temperature and applies effects if necessary.
     * This method should be called asynchronously.
     *
     * @param player The player whose temperature is being checked.
     */
    fun checkTemperature(player: Player) {
        assert(player.uniqueId == playerId)


        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        // If we are enclosed we care about insulation if not we don't
        if (isEnclosed) {
            updateTemperature(emptySet(), insulation, Manager.climateManager.getTemperature(player.location))
        } else {
            updateTemperature(emptySet(), emptyList(), Manager.climateManager.getTemperature(player.location))
        }

        if (player.gameMode == org.bukkit.GameMode.CREATIVE || player.gameMode == org.bukkit.GameMode.SPECTATOR) {
            return
        }

        // Apply effects in sync on the next tick
        Bukkit.getScheduler().callSyncMethod(Manager) {
            applyTemperatureEffects(player)
        }
    }


    /**
     * Applies temperature effects to the player.
     * This method should be called synchronously.
     *
     * @param player The player to apply effects to.
     */
    private fun applyTemperatureEffects(player: Player) {
        if (temperature < bodyTemp - toleranceBuffer) {
            when {
                temperature < criticalLowTemp -> {
                    // Severe cold effects: constant damage
                    applyColdDamage(player, dmg * 2)
                    freeze(player)
                }
                temperature < moderateEffectsThreshold -> {
                    // Moderate cold effects: slowness, weakness, minor damage
                    applyColdEffects(player)
                    applyColdDamage(player, dmg)
                }

                temperature < mildEffectsThreshold -> {
                    // Mild cold effects: slowness, hunger
                    applyColdEffects(player, mild = true)
                }
            }
        } else {
            // Player is warming up or within comfort range, remove cold effects
            if(frozen){
                unfreeze(player)
            }

            removeColdEffects(player)
            coldMessageInterval = 0
        }
    }


    private fun applyColdDamage(player: Player, damage: Double) {
        if (coldMessageInterval == 0) {
            player.sendActionBar(literalText("You are freezing!") {
                italic = true
                color = KColors.LIGHTBLUE
            })
        }

        coldMessageInterval++
        if (coldMessageInterval >= 20) coldMessageInterval = 0

        val source = DamageSource.builder(DamageType.FREEZE).build()
        val event = EntityDamageEvent(player, EntityDamageEvent.DamageCause.FREEZE, source, damage)
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

    private fun applyColdEffects(player: Player, mild: Boolean = false) {
        if (mild) {
            // Apply mild cold effects
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 0))
            player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, 100, 0))
        } else {
            // Apply moderate/severe cold effects
            player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 1))
            player.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 100, 1))
        }
    }

    fun removeColdEffects(player: Player) {
        // Remove all cold-related potion effects when the player warms up
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
        player.removePotionEffect(PotionEffectType.HUNGER)
    }

    // TODO freeze gradually
    private fun freeze(player: Player){
        frozen = true
        player.freezeTicks = Int.MAX_VALUE
    }

    fun unfreeze(player: Player){
        frozen = false
        player.freezeTicks = 0
    }
}