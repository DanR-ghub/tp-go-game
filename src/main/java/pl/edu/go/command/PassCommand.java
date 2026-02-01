package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * {@code PassCommand} enkapsuluje komendę {@code PASS}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Komenda deleguje do {@code Game.pass(...)}; logika przejścia do {@code SCORING_REVIEW}
 * zależy od stanu gry (np. dwa PASS).
 */
public class PassCommand implements GameCommand {

    /** Kolor gracza, który wykonał PASS. */
    private final PlayerColor player;

    /**
     * @param player gracz wykonujący {@code PASS}
     */
    public PassCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        // bez checka tury tutaj — walidacja tego, czy to tura gracza, jest po stronie Game/Board
        game.pass(player);
    }
}
