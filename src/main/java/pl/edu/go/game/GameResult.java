package pl.edu.go.game;

/**
 * Klasa GameResult — prosty obiekt opisujący wynik gry.
 *
 * Rola klasy:
 * - przechowuje:
 *   * zwycięzcę (PlayerColor lub null, jeśli brak),
 *   * powód zakończenia gry (np. "resign", "two passes"),
 * - przekazywana do obserwatorów w metodzie onGameEnded.
 */

public class GameResult {
    private final PlayerColor winner;
    private final String reason; // np. "resign", "both passed"

    public GameResult(PlayerColor winner, String reason) {
        this.winner = winner;
        this.reason = reason;
    }

    public PlayerColor getWinner() {
        return winner;
    }

    public String getReason() {
        return reason;
    }
}
