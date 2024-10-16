package frostplayer

import Manager
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.event.listen
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

/**
 * Manages the players in the FrostCraft plugin.
 * Registers and deregisters players, and handles player-related events.
 */
class PlayerManager {
    // A map to store frostplayer.FrostPlayer instances by their UUIDs
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
                players[it.entity.uniqueId]?.unfreeze(it.entity)
            }
        }

        listen<PlayerGameModeChangeEvent> {
            if (it.newGameMode == GameMode.SPECTATOR || it.newGameMode == GameMode.CREATIVE) {
                players[it.player.uniqueId]?.unfreeze(it.player)
            }
        }

        // Schedule a repeating task to check the temperature of every player every second
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, {
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
        val fp = FrostPlayer(player.uniqueId)
        players[player.uniqueId] = fp
        player.showBossBar(fp.bar)
    }

    /**
     * Deregisters a player by removing them from the players map.
     * @param player The player to deregister.
     */
    fun deregisterPlayer(player: Player) {
        players.remove(player.uniqueId)
    }

    /**
     * Retrieves a frostplayer.FrostPlayer instance by the player's UUID.
     * @param player The UUID of the player.
     * @return The frostplayer.FrostPlayer instance, or null if the player is not registered.
     */
    fun getPlayer(player: UUID): FrostPlayer? {
        return players[player]
    }
}