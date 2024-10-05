import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.main.KSpigot
import net.kyori.adventure.bossbar.BossBar

class FrostCraft : KSpigot() {
    companion object {lateinit var  INSTANCE: FrostCraft; private set}
    lateinit var playerManager: PlayerManager

    override fun load() {
    }

    override fun startup() {

        playerManager = PlayerManager()
    }

    override fun shutdown() {
    }
}

val Manager by lazy { FrostCraft.INSTANCE }