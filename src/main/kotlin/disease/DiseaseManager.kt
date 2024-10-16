package disease

import Manager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

// Every 20 ticks = 1 second
const val PROGRESS_INTERVAL = 20


fun callCountToSeconds(callCount: Int): Int {
    return callCount / (PROGRESS_INTERVAL/20)
}

// TODO might be wrong
fun secondsToCallCount(seconds: Int): Int {
    return seconds * (PROGRESS_INTERVAL*20)
}


class DiseaseManager {
    private val playerDiseases = mutableMapOf<UUID, MutableSet<IDisease>>()

    init {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Manager, {
            playerDiseases.keys.forEach { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                progressAllDiseases(player)
            }
        }, 0, PROGRESS_INTERVAL.toLong())

    }

    fun addDisease(player: Player, disease: IDisease) {
        playerDiseases.computeIfAbsent(player.uniqueId) { mutableSetOf() }.add(disease)
    }

    fun progressAllDiseases(player: Player){
        playerDiseases[player.uniqueId]?.forEach { disease ->
            disease.progressDisease(player)
        }
    }

    fun getDisease(player: Player, diseaseName: String): IDisease? {
        return playerDiseases[player.uniqueId]?.find { it.getName() == diseaseName }
    }

    fun applyCure(player: Player, cure: ICure) {
        val cures = cure.cures()
        this.playerDiseases[player.uniqueId]?.forEach {
            if (cures.contains(it.getName())) {
                if (cure.cure(player, it)){
                    playerDiseases[player.uniqueId]?.remove(it)
                }
            }
        }
    }

}