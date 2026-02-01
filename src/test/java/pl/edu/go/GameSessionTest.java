package pl.edu.go;

import org.junit.jupiter.api.Test;
import pl.edu.go.board.Board;
import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;
import pl.edu.go.server.ClientHandler;
import pl.edu.go.server.GameSession;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy warstwy serwerowej (GameSession) bez użycia prawdziwych socketów.
 *
 * Uwaga: ClientHandler jest final, więc nie dziedziczymy po nim.
 * Zamiast tego tworzymy normalny ClientHandler i wstrzykujemy mu PrintWriter przez refleksję.
 */
public class GameSessionTest {

    private static final class CapturingClient {
        final ClientHandler handler;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CapturingClient(GameSession session, PlayerColor color) {
            // socket może być null – nie uruchamiamy run(), tylko chcemy przechwycić sendLine()
            this.handler = new ClientHandler(null, session, color);

            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(baos, StandardCharsets.UTF_8),
                    true
            );
            injectPrintWriter(handler, pw);
        }

        void clear() {
            baos.reset();
        }

        List<String> lines() {
            String raw = baos.toString(StandardCharsets.UTF_8);
            return Arrays.stream(raw.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        boolean containsExact(String line) {
            return lines().stream().anyMatch(s -> s.equals(line));
        }

        boolean containsStartsWith(String prefix) {
            return lines().stream().anyMatch(s -> s.startsWith(prefix));
        }
    }

    private static void injectPrintWriter(ClientHandler h, PrintWriter pw) {
        try {
            // szukamy pola typu PrintWriter (nie zakładamy nazwy "out")
            Field target = null;
            for (Field f : ClientHandler.class.getDeclaredFields()) {
                if (f.getType().equals(PrintWriter.class)) {
                    target = f;
                    break;
                }
            }
            if (target == null) {
                throw new IllegalStateException("ClientHandler has no PrintWriter field to inject.");
            }
            target.setAccessible(true);
            target.set(h, pw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject PrintWriter into ClientHandler: " + e.getMessage(), e);
        }
    }

    @Test
    public void testStartGameSendsWelcomeBoardAndTurn() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        CapturingClient black = new CapturingClient(session, PlayerColor.BLACK);
        CapturingClient white = new CapturingClient(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black.handler);
        session.setPlayer(PlayerColor.WHITE, white.handler);

        session.startGame();

        // minimalne asercje (bez uzależniania od INFO/PHASE)
        assertTrue(black.containsExact("WELCOME BLACK"), "BLACK powinien dostać WELCOME BLACK");
        assertTrue(white.containsExact("WELCOME WHITE"), "WHITE powinien dostać WELCOME WHITE");

        assertTrue(black.containsExact("BOARD 5"), "BLACK powinien dostać BOARD 5");
        assertTrue(white.containsExact("BOARD 5"), "WHITE powinien dostać BOARD 5");

        assertTrue(black.containsExact("TURN BLACK"), "Na starcie ruch powinien mieć BLACK (widok BLACK)");
        assertTrue(white.containsExact("TURN BLACK"), "Na starcie ruch powinien mieć BLACK (widok WHITE)");
    }

    @Test
    public void testLegalMoveUpdatesTurn() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        CapturingClient black = new CapturingClient(session, PlayerColor.BLACK);
        CapturingClient white = new CapturingClient(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black.handler);
        session.setPlayer(PlayerColor.WHITE, white.handler);

        session.startGame();
        black.clear();
        white.clear();

        // BLACK wykonuje legalny ruch
        session.handleClientMessage(black.handler, "MOVE 2 2");

        assertTrue(black.containsExact("TURN WHITE"), "Po ruchu BLACK tura powinna przejść na WHITE (widok BLACK)");
        assertTrue(white.containsExact("TURN WHITE"), "Po ruchu BLACK tura powinna przejść na WHITE (widok WHITE)");
    }

    @Test
    public void testWrongPlayerMoveProducesError() {
        Board board = new Board(5);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        CapturingClient black = new CapturingClient(session, PlayerColor.BLACK);
        CapturingClient white = new CapturingClient(session, PlayerColor.WHITE);

        session.setPlayer(PlayerColor.BLACK, black.handler);
        session.setPlayer(PlayerColor.WHITE, white.handler);

        session.startGame();
        black.clear();
        white.clear();

        // BLACK wykonuje pierwszy, legalny ruch
        session.handleClientMessage(black.handler, "MOVE 2 2");
        black.clear();
        white.clear();

        // Teraz jest tura WHITE, a próbujemy ruchem BLACK
        session.handleClientMessage(black.handler, "MOVE 1 1");

        assertTrue(
                black.containsStartsWith("ERROR"),
                "Jeśli BLACK próbuje ruszyć w turze WHITE, powinien dostać ERROR"
        );
    }
}
