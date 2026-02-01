package pl.edu.go;

import org.junit.jupiter.api.Test;
import pl.edu.go.board.Board;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameObserver;
import pl.edu.go.game.GamePhase;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GameReviewFlowTest {

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
    void twoPasses_entersScoringReview_notFinished() {
        Game g = new Game(new Board(9));

        g.pass(PlayerColor.BLACK);
        g.pass(PlayerColor.WHITE);

        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());
        assertFalse(g.isFinished(), "Po 2 PASS gra ma wejść w SCORING_REVIEW, a nie kończyć się natychmiast");
    }

    @Test
    void resume_returnsToPlaying_andGivesTurnToOpponentOfResumer_andResetsPassCounter() {
        Game g = new Game(new Board(9));

        g.pass(PlayerColor.BLACK);
        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());

        // wznawiający oddaje ruch przeciwnikowi
        g.resume(PlayerColor.WHITE);

        assertEquals(GamePhase.PLAYING, g.getPhase());
        assertEquals(PlayerColor.BLACK, g.getCurrentPlayer(), "Po RESUME(WHITE) następny ruch ma BLACK");

        // PASS liczymy od zera po RESUME:
        g.pass(PlayerColor.BLACK);
        assertEquals(GamePhase.PLAYING, g.getPhase());

        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());
    }

    @Test
    void agreeByBoth_endsGame_withTerritoryReason() {
        Game g = new Game(new Board(9));
        AtomicReference<GameResult> resultRef = attachResultCapture(g);

        g.pass(PlayerColor.BLACK);
        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());

        g.agree(PlayerColor.BLACK);
        assertFalse(g.isFinished(), "Po pierwszym AGREE gra nie powinna się kończyć");

        g.agree(PlayerColor.WHITE);

        assertTrue(g.isFinished(), "Po AGREE obu graczy gra powinna się zakończyć");
        assertEquals(GamePhase.FINISHED, g.getPhase());

        GameResult r = resultRef.get();
        assertNotNull(r, "Observer powinien dostać GameResult w onGameEnded()");
        assertNotNull(r.getReason());
        assertTrue(r.getReason().toLowerCase().contains("territory"));
    }

    @Test
    void resign_canBeDoneInScoringReview_andEndsImmediately() {
        Game g = new Game(new Board(9));
        AtomicReference<GameResult> resultRef = attachResultCapture(g);

        g.pass(PlayerColor.BLACK);
        g.pass(PlayerColor.WHITE);
        assertEquals(GamePhase.SCORING_REVIEW, g.getPhase());

        g.resign(PlayerColor.BLACK);

        assertTrue(g.isFinished());
        assertEquals(GamePhase.FINISHED, g.getPhase());

        GameResult r = resultRef.get();
        assertNotNull(r);
        assertEquals(PlayerColor.WHITE, r.getWinner());
        assertTrue(r.getReason().toLowerCase().contains("resign"));
    }
}
