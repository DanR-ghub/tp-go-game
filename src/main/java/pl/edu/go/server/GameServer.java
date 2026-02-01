package pl.edu.go.server;

/**
 * Klasa GameServer — punkt startowy serwera gry.
 *
 * Rola klasy:
 * - tworzy obiekt Board i Game dla nowej partii,
 * - tworzy GameSession, która zarządza logiką jednej gry,
 * - otwiera gniazdo serwera (ServerSocket) na porcie 5000,
 * - akceptuje dwóch klientów:
 *   * pierwszy zostaje BLACK,
 *   * drugi zostaje WHITE,
 * - uruchamia osobne wątki ClientHandler dla każdego klienta,
 * - po podłączeniu dwóch graczy wywołuje session.startGame().
 *
 * Klasa zawiera metodę main(...) i służy do uruchamiania serwera z terminala.
 */

import pl.edu.go.board.Board;
import pl.edu.go.board.BoardFactory;

import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {

    public static void main(String[] args) {
        int port = 5001;
        int boardSize = 9; // mniejsza plansza na testy

        Board board = BoardFactory.createBoard(boardSize);
        Game game = new Game(board);
        GameSession session = new GameSession(game);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            // pierwszy klient = BLACK
            Socket s1 = serverSocket.accept();
            System.out.println("First player connected (BLACK)");
            ClientHandler h1 = new ClientHandler(s1, session, PlayerColor.BLACK);
            session.setPlayer(PlayerColor.BLACK, h1);
            new Thread(h1, "Client-BLACK").start();

            // drugi klient = WHITE
            Socket s2 = serverSocket.accept();
            System.out.println("Second player connected (WHITE)");
            ClientHandler h2 = new ClientHandler(s2, session, PlayerColor.WHITE);
            session.setPlayer(PlayerColor.WHITE, h2);
            new Thread(h2, "Client-WHITE").start();

            session.startGame();
            System.out.println("Game started. Waiting for moves...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
