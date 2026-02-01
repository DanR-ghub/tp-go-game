package pl.edu.go.game;

import pl.edu.go.analysis.ScoreCalculator;
import pl.edu.go.board.Board;
import pl.edu.go.move.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code Game} implementuje centralną logikę rozgrywki Go na poziomie „sesji gry”
 * (warstwa aplikacyjna nad {@code Board}).
 *
 * <p><b>Single Source of Truth:</b>
 * <ul>
 *   <li>{@code Game} pilnuje: fazy ({@link pl.edu.go.game.GamePhase}), tury gracza, stanu zakończenia,
 *       obsługi PASS/RESIGN/AGREE/RESUME oraz wyzwalania punktacji.</li>
 *   <li>{@code Board} pozostaje źródłem prawdy dla reguł planszy (legalność ruchu, bicie, KO itd.).</li>
 * </ul>
 *
 * <p><b>Wzorzec projektowy:</b>
 * <ul>
 *   <li><b>Observer</b> — {@code Game} publikuje zdarzenia do {@link pl.edu.go.game.GameObserver}
 *       (zmiana planszy, tury, fazy, zakończenie gry).</li>
 * </ul>
 *
 * <p><b>Zasada 8 (minimal review):</b>
 * <ul>
 *   <li>2×PASS → {@code SCORING_REVIEW} (gra nie kończy się automatycznie),</li>
 *   <li>w {@code SCORING_REVIEW}: tylko {@code AGREE}/{@code RESUME},</li>
 *   <li>po {@code RESUME}: reset PASS i <b>ruch ma przeciwnik wznawiającego</b>,</li>
 *   <li>po {@code AGREE}+{@code AGREE}: punktacja (zad. 9) i zakończenie gry.</li>
 * </ul>
 *
 * <p><b>Zadanie 10:</b> gracz może zakończyć grę w dowolnym momencie przez {@code RESIGN}.
 */
public class Game {

    private final Board board;

    private PlayerColor currentPlayer = PlayerColor.BLACK; // zaczyna BLACK
    private boolean finished = false;
    private GameResult result;

    private int consecutivePasses = 0;

    // ZASADA 8
    private GamePhase phase = GamePhase.PLAYING;
    private boolean agreedBlack = false;
    private boolean agreedWhite = false;

    private final List<GameObserver> observers = new ArrayList<>();

    /**
     * Tworzy nową sesję gry na podanej planszy.
     *
     * @param board plansza gry (źródło prawdy dla reguł planszy)
     */
    public Game(Board board) {
        this.board = board;
    }

    /**
     * Zwraca aktualną planszę gry.
     *
     * @return obiekt {@link Board} powiązany z tą sesją gry
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Zwraca gracza, który ma aktualnie wykonać ruch.
     *
     * @return kolor gracza na ruchu
     */
    public PlayerColor getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Informuje, czy gra jest zakończona.
     *
     * @return {@code true} jeśli gra została zakończona (RESIGN lub punktacja)
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Zwraca aktualną fazę gry.
     *
     * @return faza ({@code PLAYING}/{@code SCORING_REVIEW}/{@code FINISHED})
     */
    public GamePhase getPhase() {
        return phase;
    }

    /**
     * Rejestruje obserwatora zdarzeń gry.
     *
     * @param observer obiekt nasłuchujący zmian (plansza/tura/faza/koniec gry)
     */
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /**
     * Usuwa wcześniej zarejestrowanego obserwatora.
     *
     * @param observer obserwator do usunięcia
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * Powiadamia obserwatorów o zmianie planszy.
     * Wywoływane po poprawnym ruchu (MOVE) lub po zdarzeniach wpływających na widok planszy.
     */
    private void notifyBoardChanged() {
        for (GameObserver o : observers) {
            o.onBoardChanged(board);
        }
    }

    /**
     * Powiadamia obserwatorów o zmianie gracza na ruchu.
     * Wywoływane po ruchu lub PASS/RESUME.
     */
    private void notifyPlayerToMoveChanged() {
        for (GameObserver o : observers) {
            o.onPlayerToMoveChanged(currentPlayer);
        }
    }

    /**
     * Powiadamia obserwatorów o zakończeniu gry.
     * Przekazuje końcowy {@link GameResult}.
     */
    private void notifyGameEnded() {
        for (GameObserver o : observers) {
            o.onGameEnded(result);
        }
    }

    /**
     * Powiadamia obserwatorów o zmianie fazy gry.
     * Np. przejście do {@code SCORING_REVIEW} lub {@code FINISHED}.
     */
    private void notifyPhaseChanged() {
        for (GameObserver o : observers) {
            o.onPhaseChanged(phase);
        }
    }

    // ===== MOVE =====

    /**
     * Wykonuje ruch na podstawie obiektu {@link Move}.
     * Kolor ruchu jest mapowany na {@link PlayerColor}, a następnie delegowany do {@link #playMove(PlayerColor, int, int)}.
     *
     * @param move ruch (kolor + współrzędne)
     * @throws IllegalArgumentException gdy {@code move == null}
     */
    public void playMove(Move move) {
        if (move == null) {
            throw new IllegalArgumentException("Move is null");
        }
        PlayerColor player = PlayerColor.fromBoardColor(move.getColor());
        playMove(player, move.getX(), move.getY());
    }

    /**
     * Wykonuje ruch gracza w fazie {@code PLAYING}.
     *
     * <p>Waliduje stan sesji (zakończenie, faza, tura), a legalność ruchu na planszy deleguje do {@link Board}.
     * Po poprawnym ruchu resetuje liczbę kolejnych PASS, zmienia turę i publikuje zdarzenia observerów.</p>
     *
     * @param player gracz wykonujący ruch
     * @param x      kolumna
     * @param y      wiersz
     * @throws IllegalStateException    gdy gra zakończona / zła faza / nie tura gracza
     * @throws IllegalArgumentException gdy ruch jest nielegalny na {@link Board}
     */
    public void playMove(PlayerColor player, int x, int y) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.PLAYING) {
            throw new IllegalStateException("Not in PLAYING phase");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        boolean ok = board.playMove(player.toBoardColor(), x, y);
        if (!ok) {
            throw new IllegalArgumentException("Illegal move");
        }

        consecutivePasses = 0;

        currentPlayer = currentPlayer.opposite();
        notifyBoardChanged();
        notifyPlayerToMoveChanged();
    }

    // ===== PASS =====

    /**
     * Wykonuje PASS w fazie {@code PLAYING}.
     *
     * <p>Po dwóch kolejnych PASS gra przechodzi do fazy {@code SCORING_REVIEW}
     * (gra nie kończy się automatycznie).</p>
     *
     * @param player gracz wykonujący PASS
     * @throws IllegalStateException jeśli gra zakończona lub nie w fazie PLAYING albo nie tura gracza
     */
    public void pass(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.PLAYING) {
            throw new IllegalStateException("PASS allowed only in PLAYING phase");
        }
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn: " + player.name());
        }

        consecutivePasses++;

        if (consecutivePasses >= 2) {
            phase = GamePhase.SCORING_REVIEW;
            agreedBlack = false;
            agreedWhite = false;
            notifyPhaseChanged();
            return;
        }

        currentPlayer = currentPlayer.opposite();
        notifyPlayerToMoveChanged();
    }

    // ===== ZASADA 8: REVIEW =====

    /**
     * AGREE — gracz akceptuje automatycznie policzony wynik/terytorium w {@code SCORING_REVIEW}.
     * Gdy obaj gracze wykonają AGREE, uruchamiana jest punktacja (zad. 9) i gra się kończy.
     *
     * @param player gracz akceptujący wynik
     * @throws IllegalStateException jeśli gra zakończona lub nie w fazie {@code SCORING_REVIEW}
     */
    public void agree(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.SCORING_REVIEW) {
            throw new IllegalStateException("AGREE allowed only in SCORING_REVIEW");
        }

        if (player == PlayerColor.BLACK) {
            agreedBlack = true;
        } else {
            agreedWhite = true;
        }

        if (agreedBlack && agreedWhite) {
            endByTerritory();
        }
    }

    /**
     * Wznawia grę z fazy {@code SCORING_REVIEW} do {@code PLAYING}.
     *
     * <p>Zgodnie z wymaganiem zadania: gracz wznawiający oddaje prawo następnego ruchu
     * przeciwnikowi.</p>
     *
     * @param player gracz żądający wznowienia
     * @throws IllegalStateException jeśli nie w fazie SCORING_REVIEW albo gra zakończona
     */
    public void resume(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }
        if (phase != GamePhase.SCORING_REVIEW) {
            throw new IllegalStateException("RESUME allowed only in SCORING_REVIEW");
        }

        phase = GamePhase.PLAYING;
        consecutivePasses = 0;
        agreedBlack = false;
        agreedWhite = false;

        // klucz: wznawiający oddaje ruch przeciwnikowi
        currentPlayer = player.opposite();

        notifyPhaseChanged();
        notifyPlayerToMoveChanged();
    }

    // ===== RESIGN =====

    /**
     * RESIGN — gra kończy się od razu, wygrywa przeciwnik.
     *
     * @param player gracz, który rezygnuje
     * @throws IllegalStateException jeśli gra już jest zakończona
     */
    public void resign(PlayerColor player) {
        if (finished) {
            throw new IllegalStateException("Game already finished");
        }

        finished = true;
        phase = GamePhase.FINISHED;
        notifyPhaseChanged();

        result = new GameResult(player.opposite(), "resign");
        notifyGameEnded();
    }

    // ===== koniec przez terytorium (zasada 9) =====

    /**
     * Kończy grę po uzgodnieniu wyniku w review: liczy punktację terytorialną i publikuje {@link GameResult}.
     */
    private void endByTerritory() {
        finished = true;
        phase = GamePhase.FINISHED;
        notifyPhaseChanged();

        int[] score = ScoreCalculator.computeScore(board);

        PlayerColor winner;
        if (score[0] > score[1]) {
            winner = PlayerColor.BLACK;
        } else if (score[1] > score[0]) {
            winner = PlayerColor.WHITE;
        } else {
            winner = null; // remis
        }

        result = new GameResult(winner, "territory");
        notifyGameEnded();
    }
}
