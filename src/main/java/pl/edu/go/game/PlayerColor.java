package pl.edu.go.game;

import pl.edu.go.board.Board;

/**
 * {@code PlayerColor} reprezentuje kolor gracza (BLACK/WHITE) oraz operacje pomocnicze
 * (np. zamiana na przeciwnika).
 *
 * <p>Enum jest używany w logice gry oraz w protokole komunikacji klient–serwer.
 */
public enum PlayerColor {
    BLACK(Board.BLACK),
    WHITE(Board.WHITE);

    /** Kolor w reprezentacji używanej przez {@link Board}. */
    private final int boardColor;

    PlayerColor(int boardColor) {
        this.boardColor = boardColor;
    }

    /**
     * Zwraca kolor w postaci {@code int} używanej przez {@link Board} (Board.BLACK / Board.WHITE).
     *
     * @return kolor w formacie planszy
     */
    public int toBoardColor() {
        return boardColor;
    }

    /**
     * Zwraca przeciwnika (BLACK ↔ WHITE).
     *
     * @return kolor przeciwnika
     */
    public PlayerColor opposite() {
        return this == BLACK ? WHITE : BLACK;
    }

    /**
     * Mapuje kolor z {@link Board} na {@link PlayerColor}.
     *
     * @param color kolor w formacie planszy
     * @return odpowiadający {@link PlayerColor}
     * @throws IllegalArgumentException gdy {@code color} nie oznacza koloru kamienia
     */
    public static PlayerColor fromBoardColor(int color) {
        return switch (color) {
            case Board.BLACK -> BLACK;
            case Board.WHITE -> WHITE;
            default -> throw new IllegalArgumentException("Not a stone color: " + color);
        };
    }
}
