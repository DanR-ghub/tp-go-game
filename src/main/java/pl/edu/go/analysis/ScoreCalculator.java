package pl.edu.go.analysis;

import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.model.StoneGroup;

/**
 * Oblicza końcowy wynik gry Go w wariancie punktacji terytorialnej.
 *
 * <p>
 * <b>Realizacja zasady 9 gry Go:</b>
 * Martwe kamienie traktowane są jako jeńcy i dodawane
 * do punktów przeciwnika, a następnie sumowane z terytorium.
 */
public class ScoreCalculator {

    /**
     * Oblicza wynik końcowy gry.
     *
     * @param board aktualny stan planszy
     * @return tablica wyników: [BLACK, WHITE]
     */
    public static int[] computeScore(Board board) {

        TerritoryAnalyzer territoryAnalyzer = new TerritoryAnalyzer(board);
        PositionAnalyzer positionAnalyzer = new PositionAnalyzer(board);

        Territory[][] t = territoryAnalyzer.computeTerritory(); // które puste pola dają punkty i komu

        int black = 0;
        int white = 0;

        // Liczenie punktów terytorium
        for (int x = 0; x < t.length; x++) {
            for (int y = 0; y < t.length; y++) {
                if (t[x][y] == Territory.BLACK)
                    black++;
                else if (t[x][y] == Territory.WHITE)
                    white++;
            }
        }

        // Liczenie jeńców (martwych kamieni)
        for (StoneGroup g : positionAnalyzer.getDeadGroups()) {
            int cnt = g.getStones().size();

            if (g.getColor() == Board.BLACK)
                white += cnt;
            else
                black += cnt;
        }

        return new int[] { black, white };
    }
}
