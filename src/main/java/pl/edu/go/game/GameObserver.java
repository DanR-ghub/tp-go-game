package pl.edu.go.game;

import pl.edu.go.board.Board;

/**
 * {@code GameObserver} jest interfejsem obserwatora stanu gry.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Observer</b>.
 * Implementacje subskrybują zdarzenia z {@link pl.edu.go.game.Game}
 * i reagują na zmiany (np. rozsyłając protokół do klientów).
 */
public interface GameObserver {

    /**
     * Zdarzenie: zmiana stanu planszy (np. po MOVE).
     *
     * @param board aktualna plansza gry
     */
    void onBoardChanged(Board board);

    /**
     * Zdarzenie: zakończenie gry (RESIGN lub koniec po punktacji).
     *
     * @param result wynik gry
     */
    void onGameEnded(GameResult result);

    /**
     * Zdarzenie: zmiana gracza na ruchu.
     *
     * @param player gracz, który ma wykonać następny ruch
     */
    void onPlayerToMoveChanged(PlayerColor player);

    /**
     * Zdarzenie: zmiana fazy gry (PLAYING/SCORING_REVIEW/FINISHED).
     *
     * @param phase nowa faza gry
     */
    void onPhaseChanged(GamePhase phase);
}
