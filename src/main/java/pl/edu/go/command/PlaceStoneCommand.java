package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.move.Move;

/**
 * PlaceStoneCommand — komenda ruchu "postaw kamień".
 *
 * Wzorce:
 * - Command:
 *   - enkapsuluje żądanie wykonania ruchu jako obiekt,
 *   - pozwala GameSession traktować MOVE/PASS/RESIGN jednolicie (GameCommand.execute).
 *
 * Zasada odpowiedzialności:
 * - PlaceStoneCommand NIE waliduje tury ani legalności ruchu.
 * - Walidacja jest SINGLE SOURCE OF TRUTH w Game (tura/finished) i Board (reguły planszy).
 *
 * Przepływ:
 * - TextCommandFactory parsuje "MOVE x y" i buduje Move przez MoveFactory,
 * - PlaceStoneCommand wywołuje game.playMove(move),
 * - w razie błędu Game rzuca wyjątek, który GameSession mapuje na "ERROR ...".
 */


public class PlaceStoneCommand implements GameCommand {

    private final Move move;

    public PlaceStoneCommand(Move move) {
        this.move = move;
    }

    @Override
    public void execute(Game game) {
        // bez żadnych checków tury tutaj
        game.playMove(move);
    }
}
