import generators.Generator
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
import org.bukkit.block.Block
import org.bukkit.event.player.PlayerInteractEvent

class FrostCraft : KSpigot() {
    companion object {
        lateinit var INSTANCE: FrostCraft; private set
    }

    lateinit var playerManager: PlayerManager
    lateinit var climateManager: ClimateManager

    override fun load() {
        INSTANCE = this
    }

    override fun startup() {

        playerManager = PlayerManager()
        climateManager = ClimateManager()

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
                var clicks = 0
                val blocks: MutableList<Block> = mutableListOf()

                listener = listen<PlayerInteractEvent> { playerInteractEvent ->
                    playerInteractEvent.isCancelled = true
                    playerInteractEvent.clickedBlock?.let { blocks.add(it) }
                    clicks++
                    if (clicks == 4) {
                        listener!!.unregister()
                        // Create generator
                        generator = Generator(
                            60, blocks[0].location.toSimple(), blocks[0],
                            1, 373.15f, 1.0f, blocks[0].world
                        )
                        generator!!.addBlocks(blocks)
                        Bukkit.getLogger().info("Generator created")


                    }
                }
            }
        }

        command("powerOn") {
            runs {
                generator!!.findIntakes()
                generator!!.powerOn()
            }
        }

    }


    override fun shutdown() {
    }
}

val Manager by lazy { FrostCraft.INSTANCE }