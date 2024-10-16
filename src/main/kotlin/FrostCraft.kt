import disease.cold.Cold
import disease.DiseaseManager
import frostplayer.PlayerManager
import generators.Generator
import generators.InvalidStructureException
import net.axay.kspigot.chat.KColors
import net.axay.kspigot.chat.literalText
import net.axay.kspigot.commands.argument
import net.axay.kspigot.commands.command
import net.axay.kspigot.commands.runs
import net.axay.kspigot.event.SingleListener
import net.axay.kspigot.event.listen
import net.axay.kspigot.event.unregister
import net.axay.kspigot.extensions.geometry.toSimple
import net.axay.kspigot.main.KSpigot
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerInteractEvent

class FrostCraft : KSpigot() {
    companion object {
        lateinit var INSTANCE: FrostCraft; private set
    }

    lateinit var configParser: ConfigParser
    lateinit var playerManager: PlayerManager
    lateinit var climateManager: ClimateManager
    lateinit var diseaseManager: DiseaseManager

    override fun load() {
        INSTANCE = this
        saveDefaultConfig()

        configParser = ConfigParser(config)
        configParser.parseConfig()

    }

    override fun startup() {

        playerManager = PlayerManager()
        climateManager = ClimateManager()
        diseaseManager = DiseaseManager()

        command("cold"){
            runs {
                diseaseManager.addDisease(player, Cold())
            }
        }

        command("bodyTemperature") {
            runs {
                this.player.sendActionBar(literalText("${playerManager.getPlayer(player.uniqueId)!!.temperature} °K"))
            }
        }

        command("envTemperature") {
            runs {
                this.player.sendActionBar(literalText("${playerManager.getPlayer(player.uniqueId)!!.targetTemperature(player)} °K"))
            }
        }

        command("createSimpleClimateZone") {
            argument<Float>("radius") {
                argument<Float>("addTemperature") {
                    runs {
                        climateManager.createClimateZone(
                            sender.bukkitWorld!!,
                            sender.location,
                            this.getArgument("radius"),
                            { tmp: Float -> tmp + this.getArgument<Float>("addTemperature") }
                        )
                    }
                }
            }
        }

        command("getClimateZone") {
            runs {
                val zone = climateManager.getTemperature(sender.location)
                sender.bukkitSender.sendMessage(literalText("Zone temperature: $zone"))
            }
        }

        command("isEnclosed") {
            runs {
                val (isEnclosed, insulation) = isEnclosed(sender.location.toSimple(), sender.bukkitWorld!!)
                sender.bukkitSender.sendMessage(literalText("Is enclosed: $isEnclosed"))
                sender.bukkitSender.sendMessage(literalText("Insulation: ${calculateInsulation(insulation)}"))
                if (isEnclosed) {
                    sender.bukkitSender.sendMessage(literalText("Blocks: $insulation"))
                }
            }
        }

        var generator: Generator? = null

        command("createGenerator") {
            runs {
                var listener: SingleListener<PlayerInteractEvent>? = null

                listener = listen<PlayerInteractEvent> { playerInteractEvent ->
                    playerInteractEvent.clickedBlock?.let {
                        playerInteractEvent.isCancelled = true
                        listener!!.unregister()
                        try {
                            generator = Generator(it)
                        } catch (e : InvalidStructureException) {
                            playerInteractEvent.player.sendMessage("Invalid structure, a generator needs an intake, a heat source, and a control panel")
                        }
                    }
                }
            }
        }

        Bukkit.motd(literalText("FrostCraft\n") {
            color = KColors.CYAN
            bold = true
            component(literalText ("Surviving at: ${climateManager.globalTemperature} °K "){
                color = KColors.BLUE
            })
        })

    }


    override fun shutdown() {
    }
}

val Manager by lazy { FrostCraft.INSTANCE }