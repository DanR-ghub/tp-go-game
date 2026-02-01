package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code ResignCommand} enkapsuluje komendę {@code RESIGN}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Komenda pozwala zakończyć grę w dowolnym momencie (poddanie).
 */
public class ResignCommand implements GameCommand {

    /** Gracz, który rezygnuje. */
    private final PlayerColor player;

    /**
     * @param player gracz wysyłający {@code RESIGN}
     */
    public ResignCommand(PlayerColor player) {
        this.player = player;
    }

    /**
     * Deleguje obsługę do {@link Game#resign(PlayerColor)}.
     */
    @Override
    public void execute(Game game) {
        game.resign(player);
    }
}
