package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code AgreeCommand} enkapsuluje komendę {@code AGREE} w fazie {@code SCORING_REVIEW}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Po zgodzie obu graczy serwer finalizuje punktację i kończy grę.
 */
public class AgreeCommand implements GameCommand {

    /** Kolor gracza, który wysłał komendę. */
    private final PlayerColor player;

    /**
     * @param player gracz wysyłający {@code AGREE}
     */
    public AgreeCommand(PlayerColor player) {
        this.player = player;
    }

    /**
     * Deleguje obsługę do {@link Game#agree(PlayerColor)}.
     */
    @Override
    public void execute(Game game) {
        game.agree(player);
    }
}
