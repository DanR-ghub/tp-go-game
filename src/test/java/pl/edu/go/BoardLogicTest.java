package pl.edu.go;

import org.junit.jupiter.api.Test;
import pl.edu.go.analysis.PositionAnalyzer;
import pl.edu.go.analysis.TerritoryAnalyzer;
import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameObserver;
import pl.edu.go.game.GamePhase;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class BoardLogicTest {

    // ====== TESTY BOARD (ZASADY 1–6) ======

    @Test
    public void testPlaceStoneOnEmptyField() {
        Board b = new Board(5);
        assertTrue(b.playMove(Board.BLACK, 2, 2));
    }

    @Test
    public void testCannotPlaceOnOccupiedField() {
        Board b = new Board(5);
        b.playMove(Board.BLACK, 2, 2);
        assertFalse(b.playMove(Board.WHITE, 2, 2));
    }

    @Test
    public void testCannotPlaceOutsideBoard() {
        Board b = new Board(5);
        assertFalse(b.playMove(Board.BLACK, -1, 0));
        assertFalse(b.playMove(Board.BLACK, 0, -1));
        assertFalse(b.playMove(Board.BLACK, 5, 0));
        assertFalse(b.playMove(Board.BLACK, 0, 5));
    }

    @Test
    public void testSingleStoneLibertiesCenter() throws Exception {
        Board b = new Board(5);
        b.playMove(Board.BLACK, 2, 2);

        var getGroup = b.getClass().getDeclaredMethod("getGroup", int.class, int.class);
        getGroup.setAccessible(true);
        Object group = getGroup.invoke(b, 2, 2);

        var countLiberties = b.getClass().getDeclaredMethod("countLiberties", group.getClass());
        countLiberties.setAccessible(true);

        int liberties = (int) countLiberties.invoke(b, group);
        assertEquals(4, liberties);
    }

    @Test
    public void testConnectedStonesFormGroup() throws Exception {
        Board b = new Board(5);
        b.playMove(Board.BLACK, 1, 1);
        b.playMove(Board.BLACK, 2, 1);

        var gmethod = b.getClass().getDeclaredMethod("getGroup", int.class, int.class);
        gmethod.setAccessible(true);
        Object group = gmethod.invoke(b, 1, 1);

        var stonesMethod = group.getClass().getMethod("getStones");
        int size = ((java.util.Set<?>) stonesMethod.invoke(group)).size();

        assertEquals(2, size);
    }

    @Test
    public void testCaptureSingleStone() {
        Board b = new Board(3);

        b.playMove(Board.BLACK, 1, 0);
        b.playMove(Board.BLACK, 0, 1);
        b.playMove(Board.BLACK, 2, 1);
        b.playMove(Board.BLACK, 1, 2);

        b.playMove(Board.WHITE, 1, 1);
        b.playMove(Board.BLACK, 1, 1);

        assertEquals(Board.BLACK, b.getState()[1][1]);
    }

    @Test
    public void testCaptureGroup() {
        Board b = new Board(5);

        b.playMove(Board.WHITE, 2, 1);
        b.playMove(Board.WHITE, 2, 2);

        b.playMove(Board.BLACK, 1, 1);
        b.playMove(Board.BLACK, 3, 1);
        b.playMove(Board.BLACK, 1, 2);
        b.playMove(Board.BLACK, 3, 2);
        b.playMove(Board.BLACK, 2, 3);
        b.playMove(Board.BLACK, 2, 0);

        int[][] state = b.getState();
        assertEquals(Board.EMPTY, state[2][1]);
        assertEquals(Board.EMPTY, state[2][2]);
    }

    @Test
    public void testSuicideForbiddenUnlessCapturing() {
        Board b = new Board(3);

        b.playMove(Board.BLACK, 1, 0);
        b.playMove(Board.BLACK, 0, 1);
        b.playMove(Board.BLACK, 2, 1);
        b.playMove(Board.BLACK, 1, 2);

        assertFalse(b.playMove(Board.WHITE, 1, 1));
    }

    // ====== TESTY GAME (ZASADA 8/10) ======

    private static AtomicReference<GameResult> attachResultCapture(Game g) {
        AtomicReference<GameResult> ref = new AtomicReference<>();

        GameObserver observer = (GameObserver) Proxy.newProxyInstance(
                GameObserver.class.getClassLoader(),
                new Class[]{GameObserver.class},
                (proxy, method, args) -> {
                    if ("onGameEnded".equals(method.getName()) && args != null && args.length == 1) {
                        ref.set((GameResult) args[0]);
                    }
                    return null;
                }
        );

        g.addObserver(observer);
        return ref;
    }

    @Test
    public void testGameInitialPlayerIsBlack() {
        Game g = new Game(new Board(5));
        assertEquals(PlayerColor.BLACK, g.getCurrentPlayer());
    }

    @Test
    public void testPlayMoveChangesCurrentPlayer() {
        Game g = new Game(new Board(5));
        g.playMove(PlayerColor.BLACK, 2, 2);
        assertEquals(PlayerColor.WHITE, g.getCurrentPlayer());
    }

    @Test
    public void testIllegalMoveDoesNotChangePlayer() {
        Game g = new Game(new Board(5));
        assertThrows(IllegalStateException.class, () -> g.playMove(PlayerColor.WHITE, 2, 2));
        assertEquals(PlayerColor.BLACK, g.getCurrentPlayer());
    }

    @Test
    public void testTwoPassesEnterReview_NotFinish() {
        Game g = new Game(new Board(5));

        g.pass(PlayerColor.BLACK);
        assertEquals(PlayerColor.WHITE, g.getCurrentPlayer());
        assertEquals(GamePhase.PLAYING, g.getPhase());

        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());
        assertFalse(g.isFinished());
    }

    @Test
    public void testAgreeAgreeEndsGameWithTerritoryReason() {
        Game g = new Game(new Board(5));
        AtomicReference<GameResult> resultRef = attachResultCapture(g);

        g.pass(PlayerColor.BLACK);
        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());

        g.agree(PlayerColor.BLACK);
        assertFalse(g.isFinished());

        g.agree(PlayerColor.WHITE);
        assertTrue(g.isFinished());
        assertEquals(GamePhase.FINISHED, g.getPhase());

        GameResult result = resultRef.get();
        assertNotNull(result);
        assertTrue(result.getReason().toLowerCase().contains("territory"));
    }

    @Test
    public void testResignEndsGameAndSetsWinner() {
        Game g = new Game(new Board(5));
        AtomicReference<GameResult> resultRef = attachResultCapture(g);

        g.resign(PlayerColor.BLACK);

        GameResult result = resultRef.get();
        assertNotNull(result);
        assertEquals(PlayerColor.WHITE, result.getWinner());
        assertTrue(result.getReason().toLowerCase().contains("resign"));
    }

    // ====== TESTY ANALIZATORÓW (ZASADY 7–9) ======

    @Test
    public void testDeadGroupDetected() {
        Board b = new Board(5);

        b.playMove(Board.WHITE, 1, 1);
        b.playMove(Board.WHITE, 2, 1);
        b.playMove(Board.WHITE, 3, 1);
        b.playMove(Board.WHITE, 1, 2);
        b.playMove(Board.WHITE, 2, 2);
        b.playMove(Board.WHITE, 3, 2);

        b.playMove(Board.BLACK, 0, 1);
        b.playMove(Board.BLACK, 0, 2);
        b.playMove(Board.BLACK, 1, 3);
        b.playMove(Board.BLACK, 2, 3);
        b.playMove(Board.BLACK, 3, 3);
        b.playMove(Board.BLACK, 4, 1);
        b.playMove(Board.BLACK, 4, 2);

        PositionAnalyzer pa = new PositionAnalyzer(b);
        assertFalse(pa.getDeadGroups().isEmpty());
    }

    @Test
    void aliveGroupTouchingNeutralIsSeki() {
        Board b = new Board(5);
        b.playMove(Board.BLACK, 1, 1);
        b.playMove(Board.WHITE, 3, 1);

        TerritoryAnalyzer ta = new TerritoryAnalyzer(b);
        Territory[][] t = ta.computeTerritory();

        assertEquals(Territory.SEKI, t[1][1]);
        assertEquals(Territory.SEKI, t[3][1]);
    }

    @Test
    public void testTerritorySimple() {
        Board b = new Board(5);

        b.playMove(Board.BLACK, 1, 1);
        b.playMove(Board.BLACK, 2, 1);
        b.playMove(Board.BLACK, 3, 1);
        b.playMove(Board.BLACK, 1, 2);
        b.playMove(Board.BLACK, 3, 2);
        b.playMove(Board.BLACK, 1, 3);
        b.playMove(Board.BLACK, 2, 3);
        b.playMove(Board.BLACK, 3, 3);

        TerritoryAnalyzer ta = new TerritoryAnalyzer(b);
        Territory[][] t = ta.computeTerritory();

        assertEquals(Territory.BLACK, t[2][2]);
    }

    @Test
    public void testNeutralPoint() {
        Board b = new Board(5);

        b.playMove(Board.BLACK, 1, 1);
        b.playMove(Board.WHITE, 3, 1);

        TerritoryAnalyzer ta = new TerritoryAnalyzer(b);
        Territory[][] t = ta.computeTerritory();

        assertEquals(Territory.NEUTRAL, t[2][1]);
    }
}
