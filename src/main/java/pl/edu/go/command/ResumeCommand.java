package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code ResumeCommand} enkapsuluje komendę {@code RESUME} w fazie {@code SCORING_REVIEW}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Wznowienie resetuje licznik PASS oraz przekazuje ruch przeciwnikowi wznawiającego.
 */
public class ResumeCommand implements GameCommand {

    /** Gracz wznawiający grę. */
    private final PlayerColor player;

    /**
     * @param player gracz wysyłający {@code RESUME}
     */
    public ResumeCommand(PlayerColor player) {
        this.player = player;
    }

    /**
     * Deleguje obsługę do {@link Game#resume(PlayerColor)}.
     */
    @Override
    public void execute(Game game) {
        game.resume(player);
    }
}
