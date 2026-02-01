package pl.edu.go;

import org.junit.jupiter.api.Test;
import pl.edu.go.analysis.ScoreCalculator;
import pl.edu.go.board.Board;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

    @Test
    void computeScore_emptyBoard_returnsZeros() {
        Board b = new Board(9);

        int[] score = ScoreCalculator.computeScore(b);

        assertArrayEquals(new int[]{0, 0}, score);
    }

    @Test
    void computeScore_countsTerritoryAndDeadStones() {
        // 3x3, czarny kamień w centrum.
        // territory: 4 puste pola wokół -> BLACK +4
        // dead stones wg PositionAnalyzer (2 oczy): ten kamień nie ma 2 oczu -> WHITE +1
        // total: BLACK=4, WHITE=1
        Board b = new Board(3);
        assertTrue(b.playMove(Board.BLACK, 1, 1));

        int[] score = ScoreCalculator.computeScore(b);

        assertEquals(4, score[0]);
        assertEquals(1, score[1]);
    }
}
