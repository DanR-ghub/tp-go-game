package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

import java.util.*;

/**
 * Analizuje pozycję na planszy gry Go w celu określenia,
 * które grupy kamieni są żywe, a które martwe.
 *
 * <p>
 * <b>Realizacja zasady 7 gry Go:</b>
 * Grupa kamieni jest uznana za żywą, jeżeli posiada
 * co najmniej dwa niezależne oczy.
 * Grupy niespełniające tego warunku uznawane są za martwe.
 *
 * <p>
 * Wynik analizy wykorzystywany jest:
 * <ul>
 * <li>podczas punktacji (jako jeńcy),</li>
 * <li>w interfejsie użytkownika do wizualizacji martwych kamieni.</li>
 * </ul>
 */
public class PositionAnalyzer {

    /** Analizowana plansza */
    private final Board board;

    /** Rozmiar planszy */
    private final int size;

    /**
     * Tworzy analizator pozycji dla podanej planszy.
     *
     * @param board aktualny stan planszy
     */
    public PositionAnalyzer(Board board) {
        this.board = board;
        this.size = board.getState().length;
    }

    /**
     * Zwraca listę wszystkich grup kamieni uznanych za martwe.
     *
     * <p>
     * Algorytm:
     * <ol>
     * <li>Iteruje po całej planszy,</li>
     * <li>Dla każdego nieodwiedzonego kamienia wyznacza jego grupę,</li>
     * <li>Sprawdza, czy grupa jest strategicznie żywa,</li>
     * <li>Jeżeli nie – dodaje ją do listy martwych grup.</li>
     * </ol>
     *
     * @return lista martwych grup kamieni
     */
    public List<StoneGroup> getDeadGroups() {
        List<StoneGroup> dead = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board.getState()[x][y] != Board.EMPTY) {
                    String key = x + "," + y;

                    // Pomijamy kamienie należące do już przeanalizowanej grupy
                    if (!visited.contains(key)) {
                        StoneGroup g = board.getGroup(x, y);

                        // Oznaczenie wszystkich kamieni grupy jako odwiedzone
                        for (Stone s : g.getStones())
                            visited.add(s.getX() + "," + s.getY());

                        // Jeżeli grupa nie jest żywa – uznajemy ją za martwą
                        if (!isStrategicallyAlive(g))
                            dead.add(g);
                    }
                }
            }
        }
        return dead;
    }

    /**
     * Sprawdza, czy grupa kamieni jest strategicznie żywa.
     *
     * <p>
     * Obecna implementacja uznaje grupę za żywą
     * wyłącznie wtedy, gdy posiada dwa oczy.
     *
     * @param g analizowana grupa kamieni
     * @return {@code true} jeśli grupa jest żywa
     */
    private boolean isStrategicallyAlive(StoneGroup g) {
        return hasTwoEyes(g);
    }

    /**
     * Sprawdza, czy grupa kamieni posiada co najmniej dwa oczy.
     *
     * <p>
     * Oko rozumiane jest jako spójny obszar pustych pól,
     * całkowicie otoczony kamieniami jednego koloru.
     *
     * @param g grupa kamieni
     * @return {@code true} jeśli grupa ma co najmniej dwa oczy
     */
    private boolean hasTwoEyes(StoneGroup g) {
        List<Set<String>> areas = collectAdjacentEmptyAreas(g);
        int eyes = 0;

        for (Set<String> area : areas) {
            if (isEye(area, g.getColor())) {
                eyes++;
                if (eyes >= 2)
                    return true;
            }
        }
        return false;
    }

    /**
     * Zbiera wszystkie spójne obszary pustych pól
     * przylegające do danej grupy kamieni. Pozwala obliczyć liczbę oczu.
     * 
     * Spójny obszar pustych pól to zbiór pustych punktów planszy,
     * połączonych sąsiedztwem ortogonalnym (góra, dół, lewo, prawo),
     * który może zostać w całości osiągnięty, poruszając się wyłącznie po pustych
     * polach.
     *
     * @param g grupa kamieni
     * @return lista obszarów pustych pól
     */
    private List<Set<String>> collectAdjacentEmptyAreas(StoneGroup g) {
        Set<String> seen = new HashSet<>(); // zapamiętuje puste pola, które już należą do jakiegoś obszaru
        List<Set<String>> areas = new ArrayList<>(); // przechowa wszystkie znalezione obszary

        for (Stone s : g.getStones()) { // oko może stykać się z dowolnym kamieniem grupy
            for (int[] nb : board.neighbors(s.getX(), s.getY())) { // patrzymy tylko na 4 pola wokół kamienia

                if (board.getState()[nb[0]][nb[1]] == Board.EMPTY) {
                    String start = nb[0] + "," + nb[1];

                    if (seen.contains(start)) // sprawdzamy, czy to pole nie było już użyte
                        continue;

                    Set<String> area = new HashSet<>();
                    Stack<int[]> stack = new Stack<>();
                    stack.push(new int[] { nb[0], nb[1] });

                    // Depth-First Search po pustych polach
                    while (!stack.isEmpty()) {
                        int[] p = stack.pop();
                        String key = p[0] + "," + p[1];

                        if (seen.contains(key))
                            continue;

                        seen.add(key);
                        area.add(key);

                        for (int[] nnb : board.neighbors(p[0], p[1])) {
                            if (board.getState()[nnb[0]][nnb[1]] == Board.EMPTY)
                                stack.push(nnb);
                        }
                    }
                    areas.add(area);
                }
            }
        }
        return areas;
    }

    /**
     * Sprawdza, czy dany obszar pustych pól
     * stanowi oko dla określonego koloru.
     * Jeśli jakiekolwiek pole obszaru styka się z kamieniem przeciwnika
     * to nie jest okiem.
     * 
     * Oko to spójny obszar pustych pól, który:
     * przylega do analizowanej grupy kamieni,
     * oraz żadne pole tego obszaru nie sąsiaduje z kamieniem przeciwnika.
     * 
     * @param area  zbiór pustych pól
     * @param color kolor grupy
     * @return {@code true} jeśli obszar jest okiem
     */
    private boolean isEye(Set<String> area, int color) {
        int opp = (color == Board.BLACK ? Board.WHITE : Board.BLACK);

        for (String p : area) {
            String[] parts = p.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);

            // Oko nie może stykać się z kamieniem przeciwnika
            for (int[] nb : board.neighbors(x, y)) {
                if (board.getState()[nb[0]][nb[1]] == opp)
                    return false;
            }
        }
        return true;
    }
}
