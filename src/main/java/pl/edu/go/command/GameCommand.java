package pl.edu.go.command;

import pl.edu.go.game.Game;

/**
 * {@code GameCommand} reprezentuje komendę wykonywaną na obiekcie {@link Game}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Command</b>.
 * Implementacje enkapsulują żądania protokołu (MOVE/PASS/RESIGN/AGREE/RESUME)
 * i delegują wykonanie do {@code Game}.
 *
 * <p>Interfejs nie określa walidacji reguł — te są po stronie {@code Game/Board}.
 */
public interface GameCommand {

    /**
     * Wykonuje komendę na instancji gry.
     *
     * @param game gra, na której ma zostać wykonana operacja
     * @throws Exception gdy wykonanie jest niemożliwe (np. niepoprawny stan gry)
     */
    void execute(Game game) throws Exception;
}
