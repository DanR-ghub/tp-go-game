package pl.edu.go.board;

/**
 * Wzorzec: Factory Method
 * -------------------------
 * Tworzy obiekt Board.
 * Ułatwia zmianę sposobu tworzenia planszy w przyszłości
 * (np. różne warianty, tryby testowe, rozmiary).
 */
public class BoardFactory {
    public static Board createBoard(int size) {
        return new Board(size);
    }
}
