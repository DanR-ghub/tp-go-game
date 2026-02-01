package pl.edu.go;

import org.junit.jupiter.api.Test;
import pl.edu.go.board.Board;
import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;
import pl.edu.go.server.ClientHandler;
import pl.edu.go.server.GameSession;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy warstwy serwerowej (GameSession) bez użycia prawdziwych socketów.
 */
public class GameSessionTest {

    /**
     * Prosty "fałszywy" klient: dziedziczy po ClientHandler
     * i nadpisuje sendLine, żeby zbierać komunikaty w liście.
     * Nie wywołujemy run(), więc socket może być null.
     */
    private static class TestClientHandler extends ClientHandler {

        private final List<String> sent = new ArrayList<>();

        public TestClientHandler(GameSession session, PlayerColor color) {
            super(null, session, color); // socket = null, nie używamy run()
        }

        @Override
        public void sendLine(String line) {
            sent.add(line);
        }

        public List<String> getSent() {
            return sent;
        }
    }

    @Test
    public void testStartGameSendsWelcomeBoardAndTurn() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        TestClientHandler black = new TestClientHandler(session, PlayerColor.BLACK);
        TestClientHandler white = new TestClientHandler(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black);
        session.setPlayer(PlayerColor.WHITE, white);

        session.startGame();

        // BLACK
        assertTrue(
                black.getSent().contains("WELCOME BLACK"),
                "BLACK powinien dostać WELCOME BLACK"
        );
        assertTrue(
                black.getSent().contains("BOARD 5"),
                "BLACK powinien dostać opis planszy BOARD 5"
        );
        assertTrue(
                black.getSent().contains("TURN BLACK"),
                "Na starcie gry ruch powinien mieć BLACK"
        );

        // WHITE
        assertTrue(
                white.getSent().contains("WELCOME WHITE"),
                "WHITE powinien dostać WELCOME WHITE"
        );
        assertTrue(
                white.getSent().contains("BOARD 5"),
                "WHITE powinien dostać opis planszy BOARD 5"
        );
        assertTrue(
                white.getSent().contains("TURN BLACK"),
                "WHITE też powinien widzieć, że ruch ma BLACK"
        );
    }

    @Test
    public void testLegalMoveUpdatesTurn() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        TestClientHandler black = new TestClientHandler(session, PlayerColor.BLACK);
        TestClientHandler white = new TestClientHandler(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black);
        session.setPlayer(PlayerColor.WHITE, white);

        session.startGame();
        black.getSent().clear();
        white.getSent().clear();

        // BLACK wykonuje legalny ruch
        session.handleClientMessage(black, "MOVE 2 2");

        // powinna być zmiana gracza na WHITE
        assertTrue(
                black.getSent().contains("TURN WHITE"),
                "Po legalnym ruchu BLACK ruch powinien przejść na WHITE (BLACK widzi TURN WHITE)"
        );
        assertTrue(
                white.getSent().contains("TURN WHITE"),
                "WHITE też powinien dostać TURN WHITE"
        );
    }

    @Test
    public void testWrongPlayerMoveProducesError() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        TestClientHandler black = new TestClientHandler(session, PlayerColor.BLACK);
        TestClientHandler white = new TestClientHandler(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black);
        session.setPlayer(PlayerColor.WHITE, white);

        session.startGame();
        black.getSent().clear();
        white.getSent().clear();

        // BLACK wykonuje pierwszy, legalny ruch
        session.handleClientMessage(black, "MOVE 2 2");
        black.getSent().clear();

        // Teraz jest tura WHITE, ale spróbujemy ruchem BLACK
        session.handleClientMessage(black, "MOVE 1 1");

        boolean hasError = black.getSent().stream()
                .anyMatch(s -> s.startsWith("ERROR"));

        assertTrue(hasError,
                "Jeśli BLACK próbuje ruszyć w turze WHITE, powinien dostać komunikat ERROR");
    }
}
