package pl.edu.go.command;

/**
 * Interfejs GameCommand — wspólne API dla wszystkich komend gry.
 *
 * Wzorzec projektowy:
 * - Command:
 *   - Każda akcja wywoływana przez gracza (ruch, pass, rezygnacja)
 *     jest reprezentowana jako obiekt typu GameCommand.
 *   - Serwer może przekazać komendę do Game bez znajomości szczegółów
 *     jej wykonania.
 *
 * Metoda:
 * - execute(Game game) — wykonuje konkretną akcję na podanej instancji gry.
 */

import pl.edu.go.game.Game;

public interface GameCommand {
    void execute(Game game) throws Exception;
}