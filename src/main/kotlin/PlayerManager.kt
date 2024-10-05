import org.bukkit.entity.Player
import java.util.UUID

class PlayerManager {
    private val players = mutableMapOf<UUID, FrostPlayer>()

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