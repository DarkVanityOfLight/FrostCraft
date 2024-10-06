import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.event.listen
import net.axay.kspigot.extensions.geometry.toSimple
import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent

const val dmg = 0.5;

class FrostPlayer(var playerId: java.util.UUID) {
    var temperature : Float = 0.0f
    var diedFromFrost = false
    var coldMessageInterval = 0

    fun updateTemperature(){}


    // Do this async(I hate sync/async stuff)
    fun checkTemperature(player: Player){
        assert(player.uniqueId == playerId)

        if (player.gameMode == org.bukkit.GameMode.CREATIVE || player.gameMode == org.bukkit.GameMode.SPECTATOR){
            return
        }

        isEnclosed(player.location.toSimple(), player.world).let {
            temperature = if (it.first){
                this.coldMessageInterval = 0
                0.0f
            }else{
                -1.0f
            }
        }

        // Apply effects in sync on the next tick
        Bukkit.getScheduler().callSyncMethod<Unit>(Manager){
            applyTemperatureEffects(player)
        }
    }


    // Run this in sync
    private fun applyTemperatureEffects(player: Player) {
        if (temperature < 0){
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