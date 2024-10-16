package disease

import org.bukkit.entity.Player

interface ICure {
    fun cure(player: Player, disease: IDisease) : Boolean

    fun cures(): List<String>
}