package pl.edu.go.server;

import pl.edu.go.game.PlayerColor;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@code ClientHandler} obsługuje pojedyncze połączenie TCP klienta (socket I/O).
 *
 * <p><b>Architektura:</b> warstwa transportowa (Layered Architecture).
 * Klasa odpowiada wyłącznie za komunikację: czytanie linii i wysyłanie odpowiedzi.
 *
 * <p><b>Wzorzec projektowy:</b>
 * <ul>
 *   <li><b>Reactor/Handler</b> (idiom serwerów sieciowych) — obiekt-hendler odpowiedzialny
 *       za obsługę jednego klienta i przekazywanie danych do warstwy aplikacyjnej
 *       ({@link pl.edu.go.server.GameSession}).</li>
 * </ul>
 *
 * <p>Klasa nie zawiera reguł gry; logika pozostaje w {@code Game}.
 */
public final class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameSession session;
    private final PlayerColor color;

    // Ustawiane dopiero po starcie run()
    private volatile PrintWriter out;

    // Sygnał „gotowości” (czy out jest ustawione i można wysyłać)
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    public ClientHandler(Socket socket, GameSession session, PlayerColor color) {
        this.socket = socket;
        this.session = session;
        this.color = color;
    }

    /**
     * Zwraca kolor przypisany do tego połączenia (BLACK/WHITE).
     *
     * @return kolor klienta
     */
    public PlayerColor getColor() {
        return color;
    }

    /**
     * Czeka aż handler przygotuje strumień wyjściowy ({@code out}).
     * Dzięki temu serwer może bezpiecznie wysłać komunikaty startowe (np. WELCOME/INFO)
     * bez ryzyka, że {@code out == null}.
     *
     * @param timeoutMs maksymalny czas oczekiwania w ms
     * @return {@code true} jeśli handler jest gotowy do wysyłania
     */
    public boolean awaitReady(long timeoutMs) {
        try {
            return readyLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wysyła jedną linię tekstu do klienta.
     *
     * <p>Jeżeli {@code out} nie jest jeszcze ustawione, metoda nic nie wysyła.
     * W praktyce serwer powinien wcześniej użyć {@link #awaitReady(long)}.</p>
     *
     * @param line linia do wysłania (bez '\n')
     */
    void sendLine(String line) {
        PrintWriter w = out;
        if (w != null) {
            w.println(line);
            w.flush();
        }
    }

    /**
     * Główna pętla wątku klienta:
     * <ul>
     *   <li>tworzy strumienie wejścia/wyjścia,</li>
     *   <li>ustawia {@code out} i sygnalizuje gotowość,</li>
     *   <li>wysyła komunikat INFO po połączeniu,</li>
     *   <li>czyta linie od klienta i przekazuje je do {@link GameSession}.</li>
     * </ul>
     */
    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            // ustawiamy strumień wyjściowy, którego używa sendLine(...)
            this.out = writer;
            // sygnał gotowości do wysyłania
            readyLatch.countDown();

            // prosty komunikat informacyjny po połączeniu
            sendLine("INFO Connected as " + color.name());

            String line;
            while ((line = in.readLine()) != null) {
                session.handleClientMessage(this, line);
            }

            System.out.println("Client " + color + " disconnected (EOF).");
        } catch (IOException e) {
            System.out.println("Client " + color + " disconnected: " + e.getMessage());
        } finally {
            // na wypadek gdyby wyjątek był przed ustawieniem out
            readyLatch.countDown();
        }
    }
}
