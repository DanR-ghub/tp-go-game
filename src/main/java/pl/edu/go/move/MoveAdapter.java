package pl.edu.go.move;

/**
 * Klasa MoveAdapter — adapter pomiędzy notacją użytkownika a współrzędnymi planszy.
 *
 * Wzorzec projektowy:
 * - Adapter:
 *   - Tłumaczy zewnętrzną reprezentację ruchu (np. tekstową notację "D4")
 *     na współrzędne (x, y) i ewentualnie obiekt Move.
 *   - Pozwala utrzymać klasę Board niezależną od formatu danych wejściowych.
 *
 * Rola klasy:
 * - zamiana danych z UI/klienta na formę zrozumiałą dla logiki gry.
 */

import java.util.Locale;

public class MoveAdapter {

    // Akceptuje: "B2", "b2", "B 2", "b 2"
    public static int[] toInternal(String move) {
        if (move == null) {
            throw new IllegalArgumentException("Ruch nie może być wartością null");
        }

        String m = move.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty()) {
            throw new IllegalArgumentException("Ruch nie może być pusty");
        }

        // litera + (opcjonalne spacje) + liczba
        if (!m.matches("^[A-Z]\\s*\\d+$")) {
            throw new IllegalArgumentException("Niepoprawny ruch (wymagane zastosowanie to litera + numer wiersza)");
        }

        char column = m.charAt(0);
        int x = column - 'A';

        int row1Based = Integer.parseInt(m.substring(1).trim());
        if (row1Based < 1) {
            throw new IllegalArgumentException("Wiersz musi być >= 1");
        }

        int y = row1Based - 1;
        return new int[] { x, y };
    }

    public static String toExternal(int x, int y) {
        char col = (char) ('A' + x);
        return "" + col + (y + 1);
    }
}