package pl.edu.go.board;

import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

import java.util.*;

/**
 * Reprezentuje planszę gry Go oraz implementuje logikę wykonywania ruchów.
 *
 * <p>
 * Klasa przechowuje aktualny stan planszy, umożliwia wykonywanie ruchów
 * oraz egzekwuje podstawowe reguły gry Go związane z łańcuchami kamieni,
 * oddechami, biciem oraz regułą KO.
 *
 * <p>
 * <b>Implementowane zasady gry Go:</b>
 * <ul>
 * <li><b>Zasada 4</b> – kamienie jednego koloru tworzą łańcuchy
 * posiadające wspólne oddechy,</li>
 * <li><b>Zasada 5</b> – zakaz samobójstwa z wyjątkiem ruchów
 * prowadzących do zbicia kamieni przeciwnika,</li>
 * <li><b>Zasada 6</b> – reguła KO (zakaz natychmiastowego
 * powtórzenia pozycji).</li>
 * </ul>
 *
 * <p>
 * Klasa {@code Board} nie odpowiada za punktację ani zakończenie gry.
 * Analiza pozycji i liczenie punktów realizowane są w osobnych klasach.
 */
public class Board {

    /** Stała oznaczająca puste pole planszy */
    public static final int EMPTY = 0;

    /** Stała oznaczająca czarny kamień */
    public static final int BLACK = 1;

    /** Stała oznaczająca biały kamień */
    public static final int WHITE = 2;

    /** Rozmiar planszy (N × N) */
    private final int size;

    /** Aktualny stan planszy */
    private final int[][] board;

    /**
     * Poprzedni stan planszy.
     *
     * <p>
     * Używany do sprawdzania reguły KO
     * (zasada 6 gry Go).
     */
    private int[][] previousBoard = null;

    /**
     * Tworzy nową, pustą planszę gry Go o zadanym rozmiarze.
     *
     * @param size rozmiar planszy
     */
    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
    }

    /**
     * Sprawdza, czy dane współrzędne znajdują się w granicach planszy.
     *
     * @param x współrzędna x
     * @param y współrzędna y
     * @return {@code true} jeśli pole leży na planszy
     */
    public boolean inside(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    /**
     * Zwraca listę sąsiednich pól danego punktu planszy.
     *
     * <p>
     * Sąsiedztwo jest ortogonalne (góra, dół, lewo, prawo),
     * zgodnie z zasadami gry Go.
     *
     * @param x współrzędna x pola
     * @param y współrzędna y pola
     * @return lista współrzędnych sąsiadów
     */
    public List<int[]> neighbors(int x, int y) {
        List<int[]> n = new ArrayList<>();

        // Kierunki: prawo, lewo, góra, dół
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];

            // Dodajemy tylko pola znajdujące się na planszy
            if (inside(nx, ny))
                n.add(new int[] { nx, ny });
        }
        return n;
    }

    /**
     * Wyznacza grupę (łańcuch) kamieni jednego koloru
     * połączonych sąsiedztwem ortogonalnym.
     *
     * <p>
     * <b>Realizacja zasady 4 gry Go:</b>
     * Kamienie jednego koloru stojące obok siebie
     * tworzą łańcuch posiadający wspólne oddechy.
     *
     * <p>
     * Algorytm:
     * <ol>
     * <li>Rozpoczyna przeszukiwanie od wskazanego kamienia,</li>
     * <li>Odwiedza wszystkich sąsiadów tego samego koloru,</li>
     * <li>Buduje obiekt {@link StoneGroup} zawierający cały łańcuch.</li>
     * </ol>
     *
     * @param x współrzędna x kamienia
     * @param y współrzędna y kamienia
     * @return grupa kamieni (łańcuch)
     */
    public StoneGroup getGroup(int x, int y) {
        int color = board[x][y];
        StoneGroup g = new StoneGroup(color);

        Set<String> visited = new HashSet<>();
        Stack<int[]> stack = new Stack<>();

        // Rozpoczęcie przeszukiwania od punktu startowego
        stack.push(new int[] { x, y });

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            String key = p[0] + "," + p[1];

            // Pomijamy już odwiedzone pola
            if (visited.contains(key))
                continue;

            visited.add(key);
            g.addStone(new Stone(p[0], p[1], color));

            // Dodajemy sąsiadów tego samego koloru do dalszego przeszukiwania
            for (int[] nb : neighbors(p[0], p[1]))
                if (board[nb[0]][nb[1]] == color)
                    stack.push(nb);
        }
        return g;
    }

    /**
     * Oblicza liczbę oddechów (liberties) dla danej grupy kamieni.
     *
     * <p>
     * <b>Realizacja zasady 4 gry Go:</b>
     * Oddechy liczone są wspólnie dla całej grupy.
     *
     * @param g grupa kamieni
     * @return liczba unikalnych oddechów
     */
    public int countLiberties(StoneGroup g) {
        Set<String> libs = new HashSet<>();

        // Dla każdego kamienia w grupie sprawdzamy sąsiednie pola
        for (Stone s : g.getStones()) {
            for (int[] nb : neighbors(s.getX(), s.getY())) {

                // Każde puste pole sąsiadujące z grupą jest oddechem
                if (board[nb[0]][nb[1]] == EMPTY)
                    libs.add(nb[0] + "," + nb[1]);
            }
        }
        return libs.size();
    }

    /**
     * Usuwa całą grupę kamieni z planszy.
     *
     * <p>
     * Wywoływane w momencie zbicia,
     * gdy grupa traci wszystkie oddechy.
     *
     * @param g grupa kamieni do usunięcia
     */
    private void removeGroup(StoneGroup g) {
        for (Stone s : g.getStones())
            board[s.getX()][s.getY()] = EMPTY;
    }

    /**
     * Próbuje wykonać ruch gracza na planszy.
     *
     * <p>
     * Metoda sprawdza legalność ruchu,
     * usuwa zbite grupy przeciwnika,
     * zapobiega samobójstwu oraz egzekwuje regułę KO.
     *
     * <p>
     * <b>Realizowane zasady gry Go:</b>
     * <ul>
     * <li><b>Zasada 5</b> – zakaz samobójstwa,</li>
     * <li><b>Zasada 6</b> – reguła KO.</li>
     * </ul>
     *
     * @param color kolor gracza
     * @param x     współrzędna x
     * @param y     współrzędna y
     * @return {@code true} jeśli ruch jest legalny
     */
    public boolean playMove(int color, int x, int y) {

        // Sprawdzenie, czy pole jest poprawne i puste
        if (!inside(x, y) || board[x][y] != EMPTY)
            return false;

        // Zachowanie stanu planszy sprzed ruchu
        int[][] before = deepCopy(board);

        // Tymczasowe postawienie kamienia
        board[x][y] = color;

        // Wyznaczenie koloru przeciwnika
        int opp = (color == BLACK ? WHITE : BLACK);

        // Licznik zbitych kamieni w tym ruchu
        int capturedStones = 0;

        // Analiza sąsiadów nowo postawionego kamienia
        for (int[] nb : neighbors(x, y)) {
            int nx = nb[0], ny = nb[1];

            // Sprawdzamy tylko grupy przeciwnika
            if (board[nx][ny] == opp) {
                StoneGroup g = getGroup(nx, ny);

                // Jeżeli grupa przeciwnika nie ma oddechów – zostaje zbita
                if (countLiberties(g) == 0) {
                    capturedStones += g.getStones().size();
                    removeGroup(g);
                }
            }
        }

        // Wyznaczenie własnej grupy po wykonaniu ruchu
        StoneGroup my = getGroup(x, y);

        // Sprawdzenie zakazu samobójstwa -- ZASADA 5
        if (countLiberties(my) == 0 && capturedStones == 0) {

            // Cofnięcie ruchu w przypadku samobójstwa
            board[x][y] = EMPTY;
            return false;
        }

        // Sprawdzenie reguły KO -- ZASADA 6
        if (capturedStones == 1 && previousBoard != null &&
                boardsEqual(board, previousBoard)) {

            // Przywrócenie stanu planszy sprzed ruchu
            for (int i = 0; i < size; i++)
                System.arraycopy(before[i], 0, board[i], 0, size);

            return false;
        }

        // Zapamiętanie aktualnego stanu planszy
        previousBoard = before;

        // Ruch wykonany poprawnie
        return true;
    }

    /**
     * Tworzy głęboką kopię tablicy planszy.
     *
     * @param src tablica źródłowa
     * @return kopia tablicy
     */
    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(src[i], 0, copy[i], 0, size);
        return copy;
    }

    /**
     * Porównuje dwa stany planszy pole po polu.
     *
     * @param a pierwszy stan
     * @param b drugi stan
     * @return {@code true} jeśli stany są identyczne
     */
    private boolean boardsEqual(int[][] a, int[][] b) {
        if (a == null || b == null)
            return false;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (a[i][j] != b[i][j])
                    return false;

        return true;
    }

    /**
     * Zwraca kopię aktualnego stanu planszy.
     *
     * <p>
     * Zapewnia enkapsulację –
     * kod zewnętrzny nie może modyfikować
     * wewnętrznej reprezentacji planszy.
     *
     * @return kopia planszy
     */
    public int[][] getState() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(board[i], 0, copy[i], 0, size);
        return copy;
    }
}
