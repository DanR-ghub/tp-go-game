package pl.edu.go.server;

import pl.edu.go.analysis.PositionAnalyzer;
import pl.edu.go.analysis.ScoreCalculator;
import pl.edu.go.analysis.TerritoryAnalyzer;
import pl.edu.go.board.Board;
import pl.edu.go.board.Territory;
import pl.edu.go.command.GameCommand;
import pl.edu.go.command.TextCommandFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameObserver;
import pl.edu.go.game.GamePhase;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;
import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;


/**
 * {@code GameSession} reprezentuje jedną sesję gry na serwerze i stanowi „most”
 * pomiędzy logiką gry ({@link pl.edu.go.game.Game}) a komunikacją sieciową z klientami.
 *
 * <p><b>Architektura:</b> Client–Server oraz Layered Architecture.
 * {@code GameSession} to warstwa aplikacyjna serwera: orkiestruje logikę gry i format protokołu,
 * podczas gdy {@code ClientHandler} jest warstwą transportową (I/O TCP).
 *
 * <p><b>Wzorce projektowe:</b>
 * <ul>
 *   <li><b>Observer</b> — implementuje {@link pl.edu.go.game.GameObserver} i rejestruje się w {@code Game}.
 *       Reaguje na zmiany (plansza/tura/faza/koniec) i rozsyła komunikaty protokołu do klientów.</li>
 *   <li><b>Command</b> — odbiera surowe linie tekstu od klientów, mapuje je na obiekty
 *       {@link pl.edu.go.command.GameCommand} (przez {@link pl.edu.go.command.TextCommandFactory})
 *       i wykonuje na {@code Game}.</li>
 * </ul>
 *
 * <p><b>Zasada 8 (minimal review):</b> po dwóch kolejnych {@code PASS} gra przechodzi do
 * {@code SCORING_REVIEW} (AGREE/RESUME). W tej fazie serwer wysyła:
 * <ul>
 *   <li>{@code SCORE} — wynik wg {@link pl.edu.go.analysis.ScoreCalculator},</li>
 *   <li>{@code TERRITORY} — mapa terytorium do wizualizacji,</li>
 *   <li>{@code DEADSTONES} — maska kamieni uznanych za martwe (wyjaśnia punkty).</li>
 * </ul>
 *
 * <p><b>Format DEADSTONES:</b>
 * <pre>
 * DEADSTONES &lt;size&gt;
 * DROW 010010...
 * ...
 * END_DEADSTONES
 * </pre>
 *
 * <p>{@code '1'} oznacza kamień uznany za martwy przez {@code PositionAnalyzer.getDeadGroups()},
 * czyli dokładnie to, co {@code ScoreCalculator} dolicza jako jeńców.
 */
public class GameSession implements GameObserver {

    /** Serwerowy „single source of truth” – logika sesji gry. */
    private final Game game;

    /** Parser protokołu: tekst → obiekt komendy (Command). */
    private final TextCommandFactory commandFactory = new TextCommandFactory();

    /** Handler klienta BLACK (może być null do czasu połączenia). */
    private ClientHandler blackPlayer;

    /** Handler klienta WHITE (może być null do czasu połączenia). */
    private ClientHandler whitePlayer;

    /**
     * Tworzy sesję i rejestruje się jako obserwator gry (Observer).
     *
     * @param game logika gry
     */
    public GameSession(Game game) {
        this.game = game;
        this.game.addObserver(this);
    }

    /**
     * Przypisuje handler do koloru gracza w tej sesji.
     *
     * @param color   BLACK/WHITE
     * @param handler handler klienta
     */
    public synchronized void setPlayer(PlayerColor color, ClientHandler handler) {
        if (color == PlayerColor.BLACK) {
            blackPlayer = handler;
        } else {
            whitePlayer = handler;
        }
    }

    /**
     * Uruchamia rozgrywkę: wysyła komunikaty startowe i publikuje pierwszy stan.
     *
     * <p>Wysyłane na start:
     * {@code WELCOME}, {@code PHASE}, a następnie aktualny {@code BOARD} i {@code TURN}.</p>
     */
    public synchronized void startGame() {
        if (blackPlayer != null) blackPlayer.sendLine("WELCOME BLACK");
        if (whitePlayer != null) whitePlayer.sendLine("WELCOME WHITE");

        broadcast("INFO Game started. BLACK moves first.");
        broadcast("PHASE " + game.getPhase().name());

        onBoardChanged(game.getBoard());
        onPlayerToMoveChanged(game.getCurrentPlayer());
    }

    /**
     * Wysyła linię do obu klientów (jeśli są połączeni).
     *
     * @param line linia protokołu
     */
    private void broadcast(String line) {
        if (blackPlayer != null) blackPlayer.sendLine(line);
        if (whitePlayer != null) whitePlayer.sendLine(line);
    }

    /**
     * Obsługuje linię otrzymaną od klienta: parsuje komendę i wykonuje ją na {@link Game}.
     *
     * <p>Walidacja reguł gry pozostaje w {@code Game/Board}; tu walidujemy głównie format protokołu
     * oraz raportujemy błędy do nadawcy jako {@code ERROR ...}.</p>
     *
     * @param from    klient (BLACK/WHITE)
     * @param message surowa linia protokołu
     */
    public synchronized void handleClientMessage(ClientHandler from, String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) return;

        if (game.isFinished()) {
            from.sendLine("INFO Game already finished. Please close client.");
            return;
        }

        System.out.println("Received from " + from.getColor() + ": " + trimmed);

        try {
            GameCommand command = commandFactory.fromNetworkMessage(trimmed, from.getColor());
            command.execute(game);
        } catch (Exception e) {
            from.sendLine("ERROR " + e.getMessage());
            System.out.println("Error for " + from.getColor() + ": " + e.getMessage());
        }
    }

    /**
     * Observer: zmiana planszy.
     *
     * <p>Serializuje stan {@link Board} do formatu protokołu:
     * {@code BOARD <size>} + {@code ROW ...} + {@code END_BOARD}.
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
     * Observer: zakończenie gry.
     *
     * <p>Wysyła {@code END <winner> <reason>}, gdzie {@code winner} to {@code BLACK}/{@code WHITE}/{@code NONE}.
     */
    @Override
    public void onGameEnded(GameResult result) {
        String winnerStr = (result.getWinner() == null) ? "NONE" : result.getWinner().name();
        broadcast("END " + winnerStr + " " + result.getReason());
    }

    /**
     * Observer: zmiana gracza na ruchu.
     *
     * <p>Wysyła {@code TURN <color>} do obu klientów.
     */
    @Override
    public void onPlayerToMoveChanged(PlayerColor player) {
        broadcast("TURN " + player.name());
    }

    /**
     * Observer: zmiana fazy gry.
     *
     * <p>Wysyła {@code PHASE <phase>}. Przy wejściu do {@code SCORING_REVIEW} dosyła pakiet punktacji
     * ({@code SCORE}/{@code TERRITORY}/{@code DEADSTONES}). Przy {@code PLAYING} informuje o wznowieniu.
     */
    @Override
    public void onPhaseChanged(GamePhase phase) {
        broadcast("PHASE " + phase.name());

        // W fazie review dosyłamy dane do wizualizacji punktacji (zad. 8/9)
        if (phase == GamePhase.SCORING_REVIEW) {
            broadcast("INFO Scoring review: AGREE to accept or RESUME to continue.");
            sendScoreTerritoryAndDeadMask();
        } else if (phase == GamePhase.PLAYING) {
            broadcast("INFO Resumed. Next move: " + game.getCurrentPlayer().name());
        }
    }


    /**
     * Wysyła do klientów pakiet danych punktacji dla trybu review:
     * {@code SCORE}, {@code TERRITORY} oraz {@code DEADSTONES}.
     *
     * <p>Te dane są wykorzystywane po stronie GUI wyłącznie do overlay (bez zmiany reguł gry).</p>
     */
    private void sendScoreTerritoryAndDeadMask() {
        Board b = game.getBoard();
        int size = b.getState().length;

        // SCORE (zasada 9)
        int[] score = ScoreCalculator.computeScore(b);
        broadcast("SCORE " + score[0] + " " + score[1]);

        // TERRITORY (do overlay na pustych polach)
        TerritoryAnalyzer analyzer = new TerritoryAnalyzer(b);
        Territory[][] t = analyzer.computeTerritory();
        int[][] state = b.getState();

        broadcast("TERRITORY " + size);
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < size; x++) {
                int cell = state[x][y];
                if (cell == Board.BLACK) row.append('X');
                else if (cell == Board.WHITE) row.append('O');
                else {
                    Territory tt = t[x][y];
                    char ch = switch (tt) {
                        case BLACK -> 'b';
                        case WHITE -> 'w';
                        case SEKI -> 's';
                        default -> '.';
                    };
                    row.append(ch);
                }
            }
            broadcast("TROW " + row);
        }
        broadcast("END_TERRITORY");

        // DEADSTONES (to, co ScoreCalculator dolicza jako jeńców)
        boolean[][] dead = new boolean[size][size];
        PositionAnalyzer pa = new PositionAnalyzer(b);
        for (StoneGroup g : pa.getDeadGroups()) {
            for (Stone s : g.getStones()) {
                int x = s.getX();
                int y = s.getY();
                if (x >= 0 && y >= 0 && x < size && y < size) {
                    dead[x][y] = true;
                }
            }
        }

        broadcast("DEADSTONES " + size);
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < size; x++) {
                row.append(dead[x][y] ? '1' : '0');
            }
            broadcast("DROW " + row);
        }
        broadcast("END_DEADSTONES");
    }
}
