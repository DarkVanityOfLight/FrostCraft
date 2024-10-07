import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.geometry.toSimple
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent

const val dmg = 0.5
const val bodyTemp = 37.0f

/**
 * Manages the player's temperature and applies effects based on the temperature.
 *
 * @property playerId The UUID of the player.
 * @property temperature The current temperature of the player.
 *
 */
class FrostPlayer(var playerId: java.util.UUID) {
    var temperature: Float = 0.0f
    var diedFromFrost = false
    private var coldMessageInterval = 0

    /**
     * Updates the player's temperature based on heat sources, insulation, and zone temperature.
     *
     * @param heatSources The set of materials that act as heat sources.
     * @param insulation The list of materials that provide insulation.
     * @param zoneTemperature The temperature of the zone the player is in.
     */
    private fun updateTemperature(heatSources: Set<Material>, insulation: List<Material>, zoneTemperature: Float) {
        temperature =
            zoneTemperature + (calculateInsulation(insulation) * (calculateHeatSources(heatSources) + bodyTemp))
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
        if (temperature < bodyTemp) {
            if (coldMessageInterval == 0) {
                player.sendActionBar(literalText("You are cold!") {
                    italic = true
                    color = KColors.LIGHTBLUE
                })
            }

            coldMessageInterval++


            if (coldMessageInterval >= 20) {
                coldMessageInterval = 0
            }

            val source = DamageSource.builder(DamageType.FREEZE).build()
            val event = EntityDamageEvent(player, EntityDamageEvent.DamageCause.FREEZE, source, dmg)
            Bukkit.getPluginManager().callEvent(event)

            if (!event.isCancelled) {
                if (player.health - dmg <= 0) {
                    player.health = 0.0
                    diedFromFrost = true
                } else {
                    player.health -= dmg
                }
            }

        }
    }
}