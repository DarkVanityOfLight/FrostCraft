import net.axay.kspigot.event.listen
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class PlayerManager {
    private val players = mutableMapOf<UUID, FrostPlayer>()

    init {
        listen<PlayerJoinEvent> {
            registerPlayer(it.player)
        }

        listen<PlayerQuitEvent> {
            deregisterPlayer(it.player)
        }

        listen<PlayerDeathEvent>{
            if (it.entity.uniqueId in players){
               if (players[it.entity.uniqueId]!!.diedFromFrost){
                   it.deathMessage = "${it.entity.name} died from the cold"
                   players[it.entity.uniqueId]!!.diedFromFrost = false
               }
            }
        }

        // Add a task to check the temperature of every player every second
        // This is a simple way to do it, but it's not efficient, it's better to use a more efficient way to do it
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, Runnable {
            players.values.forEach {
                Bukkit.getScheduler().runTaskAsynchronously(Manager, Runnable {
                    it.checkTemperature(Bukkit.getPlayer(it.playerId)!!)
                })

            }
        }, 0, 20)
    }

    fun registerPlayer(player: Player){
        players[player.uniqueId] = FrostPlayer(player.uniqueId)
    }

    fun deregisterPlayer(player: Player){
        players.remove(player.uniqueId)
    }

    fun getPlayer(player: UUID): FrostPlayer? {
        return players[player]
    }

}