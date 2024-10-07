import net.axay.kspigot.event.listen
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

/**
 * Manages the players in the FrostCraft plugin.
 * Registers and deregisters players, and handles player-related events.
 */
class PlayerManager {
    // A map to store FrostPlayer instances by their UUIDs
    private val players = mutableMapOf<UUID, FrostPlayer>()

    init {
        // Listen for player join events to register the player
        listen<PlayerJoinEvent> {
            registerPlayer(it.player)
        }

        // Listen for player quit events to deregister the player
        listen<PlayerQuitEvent> {
            deregisterPlayer(it.player)
        }

        // Listen for player death events to customize the death message if the player died from frost
        listen<PlayerDeathEvent> {
            if (it.entity.uniqueId in players) {
                if (players[it.entity.uniqueId]!!.diedFromFrost) {
                    it.deathMessage = "${it.entity.name} died from the cold"
                    players[it.entity.uniqueId]!!.diedFromFrost = false
                }
            }
        }

        // Schedule a repeating task to check the temperature of every player every second
        // This is a simple way to do it, but it's not efficient, it's better to use a more efficient way to do it
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, Runnable {
            players.values.forEach {
                Bukkit.getScheduler().runTaskAsynchronously(Manager, Runnable {
                    it.checkTemperature(Bukkit.getPlayer(it.playerId)!!)
                })
            }
        }, 0, 20)
    }

    /**
     * Registers a player by adding them to the players map.
     * @param player The player to register.
     */
    fun registerPlayer(player: Player) {
        players[player.uniqueId] = FrostPlayer(player.uniqueId)
    }

    /**
     * Deregisters a player by removing them from the players map.
     * @param player The player to deregister.
     */
    fun deregisterPlayer(player: Player) {
        players.remove(player.uniqueId)
    }

    /**
     * Retrieves a FrostPlayer instance by the player's UUID.
     * @param player The UUID of the player.
     * @return The FrostPlayer instance, or null if the player is not registered.
     */
    fun getPlayer(player: UUID): FrostPlayer? {
        return players[player]
    }
}