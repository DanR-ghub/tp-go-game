package pl.edu.go.game;

/**
 * {@code GameResult} enkapsuluje wynik zakończonej gry: zwycięzcę (lub remis) oraz przyczynę zakończenia
 * (np. {@code resign} lub {@code territory}).
 *
 * <p>Obiekt jest emitowany jako zdarzenie w mechanizmie {@link pl.edu.go.game.GameObserver} (Observer).
 */
public class GameResult {

    /** Zwycięzca; {@code null} oznacza remis. */
    private final PlayerColor winner;

    /** Powód zakończenia (np. "resign", "territory"). */
    private final String reason;

    /**
     * @param winner zwycięzca lub {@code null} przy remisie
     * @param reason przyczyna zakończenia
     */
    public GameResult(PlayerColor winner, String reason) {
        this.winner = winner;
        this.reason = reason;
    }

    /**
     * Zwraca zwycięzcę gry.
     *
     * @return zwycięzca lub {@code null} (remis)
     */
    public PlayerColor getWinner() {
        return winner;
    }

    /**
     * Zwraca przyczynę zakończenia gry.
     *
     * @return powód zakończenia (np. "resign", "territory")
     */
    public String getReason() {
        return reason;
    }
}
