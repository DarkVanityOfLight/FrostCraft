import org.bukkit.entity.Player

class FrostPlayer(val player: Player) : Player by  player{
    var temperature : Float = 0.0f
}