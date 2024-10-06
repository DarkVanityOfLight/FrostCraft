import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.extensions.geometry.toSimple
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent

const val dmg = 0.5;
const val bodyTemp = 37.0f

class FrostPlayer(var playerId: java.util.UUID) {
    var temperature : Float = 0.0f
    var diedFromFrost = false
    private var coldMessageInterval = 0

    private fun updateTemperature(heatSources: Set<Material>, insulation : List<Material>, zoneTemperature: Float){
        temperature = zoneTemperature + (calculateInsulation(insulation) * (calculateHeatSources(heatSources) + bodyTemp))
    }


    // Do this async(I hate sync/async stuff)
    fun checkTemperature(player: Player){
        assert(player.uniqueId == playerId)


        val (isEnclosed, insulation) = isEnclosed(player.location.toSimple(), player.world)

        // If we are enclosed we care about insulation if not we don't
        if (isEnclosed){
            updateTemperature(emptySet(), insulation, Manager.climateManager.getTemperature(player.location))
        }else {
            updateTemperature(emptySet(), emptyList(), Manager.climateManager.getTemperature(player.location))
        }

        if (player.gameMode == org.bukkit.GameMode.CREATIVE || player.gameMode == org.bukkit.GameMode.SPECTATOR){
            return
        }

        // Apply effects in sync on the next tick
        Bukkit.getScheduler().callSyncMethod<Unit>(Manager){
            applyTemperatureEffects(player)
        }
    }


    // Run this in sync
    private fun applyTemperatureEffects(player: Player) {
        if (temperature < bodyTemp){
            if (coldMessageInterval == 0 ){
                player.sendActionBar(literalText("You are cold!"){
                    italic = true
                    color = KColors.LIGHTBLUE
                })
            }

            coldMessageInterval++;


            if(coldMessageInterval >= 20){
                coldMessageInterval = 0
            }

            val source = DamageSource.builder(DamageType.FREEZE).build()
            val event = EntityDamageEvent(player, EntityDamageEvent.DamageCause.FREEZE, source, dmg)
            Bukkit.getPluginManager().callEvent(event)

            if (!event.isCancelled){
                if (player.health - dmg <= 0){
                    player.health = 0.0
                    diedFromFrost = true
                }else{
                    player.health = player.health - dmg
                }
            }

        }
    }
}