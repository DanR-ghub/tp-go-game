package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

/**
 * Analizuje planszę gry Go i przypisuje puste pola
 * do terytorium czarnego, białego lub neutralnego.
 *
 * <p>
 * <b>Realizacja zasady 7 gry Go:</b>
 * Implementuje pojęcia terytorium, punktów neutralnych
 * oraz seki.
 */
public class TerritoryAnalyzer {

    private final Board board;
    private final int size;

    /**
     * Tworzy analizator terytorium dla podanej planszy.
     *
     * @param board aktualny stan planszy
     */
    public TerritoryAnalyzer(Board board) {
        this.board = board;
        this.size = board.getState().length;
    }

    /**
     * Oblicza końcowe przypisanie terytorium.
     * korekta globalna + seki
     *
     * @return tablica terytorium dla każdego pola planszy
     */
    public Territory[][] computeTerritory() {
        Territory[][] raw = computeRawTerritory();
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board.getState()[x][y] == Board.EMPTY) { // seki dotyczy grup kamieni, nie pustych pól.
                    out[x][y] = raw[x][y];
                    continue;
                }

                StoneGroup g = board.getGroup(x, y); // seki analizujemy na poziomie grupy

                // Wykrywanie seki
                if (board.countLiberties(g) >= 2 && groupTouchesNeutral(g, raw))
                    out[x][y] = Territory.SEKI;
                else
                    out[x][y] = Territory.NEUTRAL;
            }
        }
        return out;
    }

    /**
     * Wstępna analiza terytorium na podstawie sąsiedztwa - Lokalna, uproszczona
     * decyzja.
     * wstępne przypisanie pustych pól
     * bez analizy seki
     */
    private Territory[][] computeRawTerritory() {
        Territory[][] out = new Territory[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {

                if (board.getState()[x][y] != Board.EMPTY) {
                    out[x][y] = Territory.NEUTRAL;
                    continue;
                }

                boolean b = false, w = false; // b - czy pole styka się z czarnym kamieniem, w - czy pole styka się z
                                              // białym kamieniem

                for (int[] nb : board.neighbors(x, y)) {
                    if (board.getState()[nb[0]][nb[1]] == Board.BLACK)
                        b = true;
                    if (board.getState()[nb[0]][nb[1]] == Board.WHITE)
                        w = true;
                }

                if (b && w)
                    out[x][y] = Territory.NEUTRAL;
                else if (b)
                    out[x][y] = Territory.BLACK;
                else if (w)
                    out[x][y] = Territory.WHITE;
                else
                    out[x][y] = Territory.NEUTRAL;
            }
        }
        return out;
    }

    /**
     * Sprawdza, czy grupa kamieni styka się z neutralnym obszarem (najmniej
     * jednym), co jest jednym
     * z warunków wykrywania seki.
     */
    private boolean groupTouchesNeutral(StoneGroup g, Territory[][] raw) {
        for (Stone s : g.getStones()) {
            for (int[] nb : board.neighbors(s.getX(), s.getY())) {
                if (board.getState()[nb[0]][nb[1]] == Board.EMPTY &&
                        raw[nb[0]][nb[1]] == Territory.NEUTRAL)
                    return true;
            }
        }
        return false;
    }
}
