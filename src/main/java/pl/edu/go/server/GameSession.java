package pl.edu.go.server;

import pl.edu.go.board.Board;
import pl.edu.go.command.GameCommand;
import pl.edu.go.command.TextCommandFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameObserver;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;

/**
 * Klasa GameSession — reprezentuje jedną sesję gry na serwerze.
 *
 * Wzorce projektowe:
 * - Observer:
 *   - Implementuje GameObserver i rejestruje się w Game.
 *   - Po każdej zmianie planszy / gracza / zakończeniu gry wysyła
 *     odpowiednie komunikaty tekstowe do klientów (BOARD, TURN, END).
 *
 * - Command:
 *   - Odbiera od ClientHandler surowe linie tekstu od klientów,
 *     przekształca je w GameCommand przez TextCommandFactory
 *     i wykonuje na obiekcie Game.
 *
 * Rola klasy:
 * - przechowuje referencję do Game (logika gry),
 * - przechowuje parę ClientHandler (BLACK i WHITE),
 * - po otrzymaniu linii od klienta:
 *   * parsuje komendę,
 *   * wykonuje ją na Game,
 *   * reaguje na zmiany przez metody GameObserver (BOARD, TURN, END),
 * - wysyła komunikaty do obu klientów (metoda broadcast).
 *
 * GameSession jest "mostem" pomiędzy logiką gry a komunikacją sieciową.
 */
public class GameSession implements GameObserver {

    private final Game game;
    private final TextCommandFactory commandFactory = new TextCommandFactory();

    // referencje do handlerów obu graczy
    private ClientHandler blackPlayer;
    private ClientHandler whitePlayer;

    public GameSession(Game game) {
        this.game = game;
        // rejestracja jako obserwator stanu gry
        this.game.addObserver(this);
    }

    /**
     * Przypisuje handler do gracza o danym kolorze (BLACK/WHITE).
     */
    public synchronized void setPlayer(PlayerColor color, ClientHandler handler) {
        if (color == PlayerColor.BLACK) {
            blackPlayer = handler;
        } else {
            whitePlayer = handler;
        }
    }

    /**
     * Obsługa wiadomości tekstowej od konkretnego klienta.
     * Tutaj:
     * - sprawdzamy, czy gra nie jest zakończona,
     * - zamieniamy tekst na GameCommand przy użyciu TextCommandFactory,
     * - wykonujemy komendę na obiekcie Game.
     */
    public synchronized void handleClientMessage(ClientHandler from, String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        // Nie pozwalamy na wykonywanie komend po zakończeniu gry
        if (game.isFinished()) {
            from.sendLine("INFO Game already finished. Please close client.");
            return;
        }

        System.out.println("Received from " + from.getColor() + ": " + trimmed);

        try {
            // zamiana linii tekstu na obiekt komendy
            GameCommand command = commandFactory.fromNetworkMessage(trimmed, from.getColor());
            // wykonanie komendy na logice gry
            command.execute(game);
            // dalsze skutki (aktualizacja planszy, END) rozchodzą się przez Observer
        } catch (Exception e) {
            from.sendLine("ERROR " + e.getMessage());
            System.out.println("Error for " + from.getColor() + ": " + e.getMessage());
        }
    }

    /**
     * Wywoływane po podłączeniu obu graczy.
     * Wysyła podstawowe informacje i pierwszy stan planszy.
     */
    public synchronized void startGame() {
        if (blackPlayer != null) {
            blackPlayer.sendLine("WELCOME BLACK");
        }
        if (whitePlayer != null) {
            whitePlayer.sendLine("WELCOME WHITE");
        }
        broadcast("INFO Game started. BLACK moves first.");
        // Początkowy stan planszy i informacja o tym, kto ma ruch
        onBoardChanged(game.getBoard());
        onPlayerToMoveChanged(game.getCurrentPlayer());
    }

    /**
     * Wysyła jedną linię do obu graczy (jeśli są podłączeni).
     */
    void broadcast(String line) {
        if (blackPlayer != null) {
            blackPlayer.sendLine(line);
        }
        if (whitePlayer != null) {
            whitePlayer.sendLine(line);
        }
    }

    // --------- Implementacja GameObserver ---------

    /**
     * Reakcja na zmianę planszy.
     * Koduje planszę jako sekwencję:
     *  BOARD <size>
     *  ROW .....
     *  ...
     *  END_BOARD
     */
    @Override
    public void onBoardChanged(Board board) {
        int[][] state = board.getState();
        int size = state.length;

        broadcast("BOARD " + size);
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < size; x++) {
                int cell = state[x][y];
                char symbol = switch (cell) {
                    case Board.BLACK -> 'X';
                    case Board.WHITE -> 'O';
                    default -> '.';
                };
                row.append(symbol);
            }
            broadcast("ROW " + row);
        }
        broadcast("END_BOARD");
    }

    /**
     * Reakcja na zakończenie gry.
     * Wysyła komunikat END <WINNER> <reason> do obu klientów.
     */
    @Override
    public void onGameEnded(GameResult result) {
        String winnerStr = result.getWinner() == null
                ? "NONE"
                : result.getWinner().name();
        broadcast("END " + winnerStr + " " + result.getReason());
    }

    /**
     * Reakcja na zmianę gracza, który ma ruch.
     */
    @Override
    public void onPlayerToMoveChanged(PlayerColor player) {
        broadcast("TURN " + player.name());
    }
}
