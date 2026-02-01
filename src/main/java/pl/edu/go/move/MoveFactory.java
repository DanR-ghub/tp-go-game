package pl.edu.go.move;

/**
 * Klasa MoveFactory — fabryka obiektów Move.
 *
 * Wzorzec projektowy:
 * - Factory Method:
 *   - Centralizuje tworzenie obiektów Move.
 *   - Ułatwia dodanie walidacji lub różnych typów ruchów,
 *     bez zmiany kodu w wielu miejscach.
 *
 * Rola klasy:
 * - tworzenie obiektów Move na podstawie współrzędnych i koloru,
 * - potencjalnie inne warianty tworzenia ruchów (z notacji tekstowej itd.).
 */

public class MoveFactory {
    public static Move createMove(int color, int x, int y) {
        return new Move(color, x, y);
    }
}
