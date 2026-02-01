package pl.edu.go.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import pl.edu.go.board.Board;
import pl.edu.go.board.BoardFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.PlayerColor;
import pl.edu.go.persistence.GameStorageService;
import pl.edu.go.server.ClientHandler;
import pl.edu.go.server.GameSession;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Minimalna aplikacja Spring pod wymaganie 3.2:
 * - tryb server: uruchamia serwer TCP i zapisuje grę + komendy do DB
 * - tryb replay: odtwarza grę z DB na podstawie zapisanych komend
 */
@SpringBootApplication(scanBasePackages = "pl.edu.go")
@EntityScan(basePackages = "pl.edu.go.persistence.entity")
@EnableJpaRepositories(basePackages = "pl.edu.go.persistence.repo")
public class GoSpringApplication implements CommandLineRunner {

    @Value("${go.mode:server}")
    private String mode;

    @Value("${go.server.port:5001}")
    private int port;

    @Value("${go.board.size:9}")
    private int boardSize;

    @Value("${go.replay.gameId:0}")
    private long replayGameId;

    private final GameStorageService storage;

    public GoSpringApplication(GameStorageService storage) {
        this.storage = storage;
    }

    public static void main(String[] args) {
        SpringApplication.run(GoSpringApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if ("replay".equalsIgnoreCase(mode)) {
            if (replayGameId <= 0) {
                System.out.println("Brak gameId. Uruchom np.: --go.mode=replay --go.replay.gameId=1");
                return;
            }
            storage.printReplay(replayGameId);
            return;
        }

        runServer();
    }

    private void runServer() throws Exception {
        Board board = BoardFactory.createBoard(boardSize);
        Game game = new Game(board);

        // 3.2: tworzymy rekord gry w DB i dostajemy gameId
        long gameId = storage.startGame(boardSize);

        // 3.2: sesja będzie zapisywać komendy po udanym execute()
        GameSession session = new GameSession(game, storage, gameId);

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("SERVER (Spring) port=" + port + " gameId=" + gameId);

            Socket s1 = ss.accept();
            ClientHandler h1 = new ClientHandler(s1, session, PlayerColor.BLACK);
            session.setPlayer(PlayerColor.BLACK, h1);
            new Thread(h1, "Client-BLACK").start();

            Socket s2 = ss.accept();
            ClientHandler h2 = new ClientHandler(s2, session, PlayerColor.WHITE);
            session.setPlayer(PlayerColor.WHITE, h2);
            new Thread(h2, "Client-WHITE").start();

            session.startGame();
        }
    }
}
