import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.argument
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.extensions.geometry.SimpleLocation3D
import net.axay.kspigot.extensions.geometry.toSimple
import net.axay.kspigot.main.KSpigot

class FrostCraft : KSpigot() {
    companion object {lateinit var  INSTANCE: FrostCraft; private set}
    lateinit var playerManager: PlayerManager
    lateinit var climateManager: ClimateManager

    override fun load() {
        INSTANCE = this
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