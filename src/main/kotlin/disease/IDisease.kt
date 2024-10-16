package disease

import frostplayer.FrostPlayer
import org.bukkit.entity.Player

interface IDisease {
    /**
     * Get the name of the disease
     * @return The name of the disease
     */
    fun getName(): String

    /**
     * Get the severity of the disease
     * @return The severity
     */
    fun getSeverity(): Int

    /**
     * This will be called by the DiseaseManager,
     * this should contain your main logic and apply effects to the player
     * @param player The player to progress the disease on, this should be matching the player that actually is infected by this disease
     */
    fun progressDisease(player: Player)

    fun cureBy(player: Player, amount: Int) : Boolean

}