import net.axay.kspigot.event.listen
import org.bukkit.entity.Player
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
    }

    fun registerPlayer(player: Player){
        players[player.uniqueId] = FrostPlayer(player)
    }

    fun deregisterPlayer(player: Player){
        players.remove(player.uniqueId)
    }

    fun getPlayer(player: UUID): FrostPlayer? {
        return players[player]
    }

}