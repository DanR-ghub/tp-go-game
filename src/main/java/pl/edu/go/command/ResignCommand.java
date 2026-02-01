package pl.edu.go.command;

/**
 * ResignCommand — komenda reprezentująca rezygnację gracza.
 *
 * Wzorzec projektowy:
 * - Command:
 *   - Implementuje GameCommand, zawiera informację o graczu,
 *     który się poddaje.
 *
 * Rola klasy:
 * - w metodzie execute(Game game) wywołuje game.resign(player),
 *   co kończy grę i ustawia zwycięzcę.
 *
 * Użycie:
 * - tworzona w TextCommandFactory na podstawie komunikatu "RESIGN".
 */

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

public class ResignCommand implements GameCommand {

    private final PlayerColor player;

    public ResignCommand(PlayerColor player) {
        this.player = player;
    }

    @Override
    public void execute(Game game) {
        game.resign(player);
    }
}
