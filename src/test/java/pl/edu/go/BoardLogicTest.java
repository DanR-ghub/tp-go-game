package pl.edu.go;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import pl.edu.go.board.Board;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;

public class BoardLogicTest {

    @Test
    public void testPlaceStoneOnEmptyField() {
        Board b = new Board(5);
        assertTrue(b.playMove(Board.BLACK, 2, 2),
                "Powinno się udać postawić kamień na pustym polu");
    }

    @Test
    public void testCannotPlaceOnOccupiedField() {
        Board b = new Board(5);
        b.playMove(Board.BLACK, 2, 2);

        assertFalse(b.playMove(Board.WHITE, 2, 2),
                "Nie wolno postawić kamienia na zajętym polu");
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
        var countLiberties = b.getClass().getDeclaredMethod(
                "countLiberties",
                group.getClass());
        countLiberties.setAccessible(true);

        int liberties = (int) countLiberties.invoke(b, group);

        assertEquals(4, liberties,
                "Kamień w środku planszy powinien mieć 4 oddechy");
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

        assertEquals(2, size,
                "Dwa sąsiadujące kamienie powinny tworzyć jedną grupę");
    }

    @Test
    public void testCaptureSingleStone() {
        Board b = new Board(3);

        // Otoczenie kamienia
        b.playMove(Board.BLACK, 1, 0);
        b.playMove(Board.BLACK, 0, 1);
        b.playMove(Board.BLACK, 2, 1);
        b.playMove(Board.BLACK, 1, 2);

        b.playMove(Board.WHITE, 1, 1);

        b.playMove(Board.BLACK, 1, 1);

        int[][] state = b.getState();

        assertEquals(Board.BLACK, state[1][1],
                "Biały kamień powinien zostać zbity i zastąpiony czarnym");
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

        assertEquals(Board.EMPTY, state[2][1],
                "Pierwszy kamień białej grupy powinien zostać zbity");
        assertEquals(Board.EMPTY, state[2][2],
                "Drugi kamień białej grupy powinien zostać zbity");
    }

    @Test
    public void testSuicideForbiddenUnlessCapturing() {
        Board b = new Board(3);

        b.playMove(Board.BLACK, 1, 0);
        b.playMove(Board.BLACK, 0, 1);
        b.playMove(Board.BLACK, 2, 1);
        b.playMove(Board.BLACK, 1, 2);

        assertFalse(b.playMove(Board.WHITE, 1, 1),
                "Ruch samobójczy powinien być niedozwolony");
    }

    // ====== DODATKOWE TESTY DLA GAME (logika wyższego poziomu) ======

    @Test
    public void testGameInitialPlayerIsBlack() {
        Board b = new Board(5);
        Game g = new Game(b);

        assertEquals(PlayerColor.BLACK, g.getCurrentPlayer(),
                "Na początku gry ruch powinien mieć BLACK");
    }

    @Test
    public void testPlayMoveChangesCurrentPlayer() {
        Board b = new Board(5);
        Game g = new Game(b);

        assertDoesNotThrow(() -> g.playMove(PlayerColor.BLACK, 2, 2),
                "Pierwszy ruch BLACK powinien być legalny");

        assertEquals(PlayerColor.WHITE, g.getCurrentPlayer(),
                "Po ruchu BLACK kolej powinna przejść na WHITE");
    }

    @Test
    public void testIllegalMoveDoesNotChangePlayer() {
        Board b = new Board(5);
        Game g = new Game(b);

        // WHITE nie powinien móc zaczynać gry — spodziewamy się wyjątku
        assertThrows(IllegalStateException.class,
                () -> g.playMove(PlayerColor.WHITE, 2, 2),
                "WHITE nie powinien móc zacząć gry jako pierwszy");

        assertEquals(PlayerColor.BLACK, g.getCurrentPlayer(),
                "Po nielegalnym ruchu gracz z ruchem powinien pozostać ten sam");
    }

    @Test
    public void testPassChangesPlayerAndTwoPassesEndGame() {
        Board b = new Board(5);
        Game g = new Game(b);

        g.pass(PlayerColor.BLACK);

        assertFalse(g.isFinished(),
                "Po jednym PASS gra nie powinna być zakończona");
        assertEquals(PlayerColor.WHITE, g.getCurrentPlayer(),
                "Po PASS BLACK ruch powinien mieć WHITE");

        g.pass(PlayerColor.WHITE);

        assertTrue(g.isFinished(),
                "Po dwóch PASS z rzędu gra powinna się zakończyć");

        GameResult result = g.getResult();
        assertNotNull(result,
                "Po zakończeniu gry wynik nie powinien być null");
        assertNull(result.getWinner(),
                "Przy dwóch PASS zwycięzca może być null (brak liczenia punktów)");
        assertEquals("two passes", result.getReason(),
                "Powód zakończenia gry powinien być 'two passes'");
    }

    @Test
    public void testResignEndsGameAndSetsWinner() {
        Board b = new Board(5);
        Game g = new Game(b);

        g.resign(PlayerColor.BLACK);

        assertTrue(g.isFinished(),
                "Po RESIGN gra powinna być zakończona");

        GameResult result = g.getResult();
        assertNotNull(result,
                "Po RESIGN wynik nie powinien być null");
        assertEquals(PlayerColor.WHITE, result.getWinner(),
                "Jeśli BLACK się poddaje, wygrać powinien WHITE");
        assertEquals("resign", result.getReason(),
                "Powód zakończenia gry powinien być 'resign'");
    }
}
