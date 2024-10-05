import net.axay.kspigot.event.listen
import org.bukkit.Bukkit
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.entity.EntityDamageEvent

const val dmg = 0.5;

class FrostPlayer(var playerId: java.util.UUID) {
    var temperature : Float = -1.0f
    var diedFromFrost = false

    fun updateTemperature(){}

    fun checkTemperature(){

        val player = Bukkit.getPlayer(playerId)
        if (temperature < 0){
            val source = DamageSource.builder(DamageType.FREEZE).build()
            val event = EntityDamageEvent(player!!, EntityDamageEvent.DamageCause.FREEZE, source, dmg)
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