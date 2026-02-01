package pl.edu.go.client.gui;

import pl.edu.go.client.net.NetworkClient;
import pl.edu.go.game.GamePhase;

import java.io.IOException;

/**
 * {@code GameController} obsługuje akcje użytkownika w GUI i wysyła komendy do serwera.
 *
 * <p><b>MVC:</b> pełni rolę <b>Controller</b>. Mapuje kliknięcia i przyciski na protokół:
 * {@code MOVE}, {@code PASS}, {@code RESIGN}, {@code AGREE}, {@code RESUME}.
 *
 * <p>Kontroler nie implementuje reguł Go — walidacja należy do serwera ({@code Game/Board}).
 */

public final class GameController {

    /** Klient sieciowy odpowiedzialny za wysyłanie linii protokołu do serwera. */
    private final NetworkClient net;

    /** Model GUI z aktualnym stanem gry (faza, możliwość ruchu, zakończenie gry). */
    private final GameModel model;

    /**
     * Tworzy kontroler dla wskazanego klienta sieciowego i modelu GUI.
     *
     * @param net   połączenie/klient sieciowy
     * @param model model stanu gry po stronie klienta
     */
    public GameController(NetworkClient net, GameModel model) {
        this.net = net;
        this.model = model;
    }

    /**
     * Obsługuje kliknięcie w przecięcie planszy.
     * Wysyła {@code MOVE x y}, jeśli gracz może aktualnie wykonać ruch.
     *
     * @param x kolumna planszy
     * @param y wiersz planszy
     */
    public void onIntersectionClicked(int x, int y) {
        if (!net.isConnected()) return;
        if (model.isFinished()) return;
        if (model.getPhase() != GamePhase.PLAYING) return;
        if (!model.canPlayNow()) return;

        try {
            net.sendLine("MOVE " + x + " " + y);
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    /**
     * Wysyła komendę {@code PASS}, jeśli klient jest połączony i gracz może teraz zagrać.
     */
    public void sendPass() {
        if (!net.isConnected()) return;
        if (!model.canPlayNow()) return;

        try {
            net.sendLine("PASS");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    /**
     * Wysyła komendę {@code RESIGN} (poddanie gry).
     */
    public void sendResign() {
        if (!net.isConnected()) return;

        try {
            net.sendLine("RESIGN");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    /**
     * Wysyła komendę {@code AGREE} w trybie review (akceptacja wyniku/punktacji).
     */
    public void sendAgree() {
        if (!net.isConnected()) return;
        if (!model.inReview()) return;

        try {
            net.sendLine("AGREE");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }

    /**
     * Wysyła komendę {@code RESUME} w trybie review (powrót do gry po sporze).
     */
    public void sendResume() {
        if (!net.isConnected()) return;
        if (!model.inReview()) return;

        try {
            net.sendLine("RESUME");
        } catch (IOException e) {
            System.err.println("[CLIENT] " + e.getMessage());
        }
    }
}
