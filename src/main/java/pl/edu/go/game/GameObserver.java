package pl.edu.go.game;

/**
 * Interfejs GameObserver — obserwator stanu gry.
 *
 * Wzorzec projektowy:
 * - Observer:
 *   - GameObserver definiuje "kontrakt" dla obiektów, które chcą
 *     reagować na zmiany w Game (np. serwer, GUI, logger).
 *
 * Metody:
 * - onBoardChanged(Board board)         — plansza uległa zmianie,
 * - onGameEnded(GameResult result)      — gra się zakończyła,
 * - onPlayerToMoveChanged(PlayerColor)  — zmienił się gracz mający ruch.
 *
 * Implementacje:
 * - GameSession na serwerze — aktualizuje klientów po każdej zmianie gry.
 */

import pl.edu.go.board.Board;

public interface GameObserver {
    void onBoardChanged(Board board);
    void onGameEnded(GameResult result);
    void onPlayerToMoveChanged(PlayerColor player);
}
