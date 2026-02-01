package pl.edu.go.server;

import pl.edu.go.board.Board;
import pl.edu.go.board.BoardFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * {@code GameServer} uruchamia serwer TCP dla gry Go.
 *
 * <p><b>Architektura:</b> Client–Server.
 * {@code GameServer} inicjalizuje warstwę transportową (socket), tworzy sesję gry
 * ({@link pl.edu.go.server.GameSession}) i przypisuje łączących się klientów do kolorów.
 *
 * <p>Klasa nie implementuje reguł gry ani punktacji; odpowiada za bootstrap i cykl życia serwera.
 */
public final class GameServer {

    /**
     * Punkt wejścia serwera.
     *
     * <p>Flow:
     * <ol>
     *   <li>tworzy {@link Board} i {@link Game},</li>
     *   <li>tworzy {@link GameSession} spinającą warstwę sieciową z logiką gry,</li>
     *   <li>akceptuje dwóch klientów i przypisuje im kolory (BLACK, potem WHITE),</li>
     *   <li>uruchamia wątki {@link ClientHandler} i startuje grę.</li>
     * </ol>
     */
    public static void main(String[] args) {
        int port = 5001;
        int boardSize = 9; // testowo 9x9

        // Inicjalizacja stanu gry po stronie serwera (Single Source of Truth)
        Board board = BoardFactory.createBoard(boardSize);
        Game game = new Game(board);

        // Sesja łączy protokół sieciowy (ClientHandler) z wywołaniami na Game
        GameSession session = new GameSession(game);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            // pierwszy klient = BLACK
            Socket s1 = serverSocket.accept();
            System.out.println("First player connected (BLACK)");
            ClientHandler h1 = new ClientHandler(s1, session, PlayerColor.BLACK);
            session.setPlayer(PlayerColor.BLACK, h1);
            Thread t1 = new Thread(h1, "Client-BLACK");
            t1.start();

            // drugi klient = WHITE
            Socket s2 = serverSocket.accept();
            System.out.println("Second player connected (WHITE)");
            ClientHandler h2 = new ClientHandler(s2, session, PlayerColor.WHITE);
            session.setPlayer(PlayerColor.WHITE, h2);
            Thread t2 = new Thread(h2, "Client-WHITE");
            t2.start();

            // KLUCZOWE: czekamy aż oba handlery będą gotowe wysyłać (żeby nie zgubić WELCOME)
            boolean r1 = h1.awaitReady(2000);
            boolean r2 = h2.awaitReady(2000);
            if (!r1 || !r2) {
                System.out.println("WARNING: Some client handlers not ready in time. Starting game anyway.");
            }

            // Start sesji: wysyłka komunikatów startowych (WELCOME/BOARD/TURN/PHASE) i gotowość na komendy
            session.startGame();
            System.out.println("Game started. Waiting for moves...");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
