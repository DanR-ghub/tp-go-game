package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.move.Move;

/**
 * {@code PlaceStoneCommand} enkapsuluje żądanie wykonania ruchu ({@code MOVE}) w grze.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Komenda deleguje wykonanie do {@link Game}; walidacja reguł (legalność ruchu, ko, tura)
 * jest po stronie {@code Game/Board}.
 */
public class PlaceStoneCommand implements GameCommand {

    /** Obiekt ruchu (kolor + współrzędne) zbudowany przez parser protokołu. */
    private final Move move;

    /**
     * @param move ruch do wykonania
     */
    public PlaceStoneCommand(Move move) {
        this.move = move;
    }

    @Override
    public void execute(Game game) {
        // bez żadnych checków tury tutaj — serwer jest Single Source of Truth dla walidacji
        game.playMove(move);
    }
}
