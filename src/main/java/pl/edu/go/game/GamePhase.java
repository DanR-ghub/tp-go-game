package pl.edu.go.game;

/**
 * {@code GamePhase} opisuje aktualną fazę rozgrywki.
 *
 * <p>Fazy są częścią wymagań zadania 8:
 * {@code PLAYING} (normalna gra), {@code SCORING_REVIEW} (AGREE/RESUME), {@code FINISHED}.
 */
public enum GamePhase {

    /** Normalna rozgrywka: dozwolone MOVE/PASS/RESIGN. */
    PLAYING,

    /** Tryb review po 2×PASS: dozwolone AGREE/RESUME (oraz ewentualnie RESIGN). */
    SCORING_REVIEW,

    /** Gra zakończona (wynik ustalony). */
    FINISHED
}
