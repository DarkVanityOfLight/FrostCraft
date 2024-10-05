import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.main.KSpigot

class FrostCraft : KSpigot() {
    companion object {lateinit var  INSTANCE: FrostCraft; private set}
    lateinit var playerManager: PlayerManager

    override fun load() {
    }

    override fun startup() {

        playerManager = PlayerManager()

        command("temperature"){
            runs {
                this.player.sendActionBar(literalText("${playerManager.getPlayer(player.uniqueId)!!.temperature}°C"))
            }
        }
    }

    override fun shutdown() {
    }
}

val Manager by lazy { FrostCraft.INSTANCE }