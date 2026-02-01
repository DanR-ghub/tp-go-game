package pl.edu.go.command;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

/**
 * PassCommand — komenda "PASS".
 *
 * Wzorce:
 * - Command:
 *   - reprezentuje akcję PASS jako obiekt GameCommand.
 *
 * Skąd wiemy, kto pasuje:
 * - PlayerColor nie pochodzi z tekstu "PASS",
 * - jest brany z kontekstu serwera (ClientHandler -> GameSession -> TextCommandFactory).
 *
 * Zasada odpowiedzialności:
 * - PassCommand nie sprawdza tury/finished.
 * - Game.pass(player) jest jedynym miejscem walidacji (single source of truth).
 *
 * Skutek:
 * - PASS zmienia turę,
 * - dwa kolejne PASS kończą grę (w uproszczeniu).
 */


public class PassCommand implements GameCommand {

    private final PlayerColor player;

    public PassCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        // bez checka tury tutaj
        game.pass(player);
    }
}
