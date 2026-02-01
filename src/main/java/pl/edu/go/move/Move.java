package pl.edu.go.move;

/**
 * Klasa Move — reprezentacja pojedynczego ruchu w grze Go.
 *
 * Wzorce projektowe:
 * - Factory Method (MoveFactory tworzy obiekty Move),
 * - Adapter (MoveAdapter tłumaczy notację użytkownika na Move).
 *
 * Rola klasy Move:
 * ------------------------------------
 * - przechowuje informacje o ruchu: kolor gracza oraz współrzędne (x, y),
 * - jest obiektem przenoszącym dane pomiędzy warstwami systemu:
 * - zapewnia spójny i rozszerzalny sposób reprezentowania ruchów,
 *
 * ------------------------------------
 * - Komendy tekstowe (PlaceStoneCommand) operują na obiektach Move,
 * - Game.playMove(Move move) deleguje ruch do Board,
 * - MoveAdapter konwertuje notację typu "D4" na Move,
 * - MoveFactory tworzy obiekty Move na podstawie współrzędnych i koloru.
 */
public class Move {

    private final int color;
    private final int x;
    private final int y;

    public Move(int color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public int getColor() {
        return color;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
