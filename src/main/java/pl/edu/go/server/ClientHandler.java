package pl.edu.go.server;

import pl.edu.go.game.PlayerColor;

import java.io.*;
import java.net.Socket;

/**
 * Klasa ClientHandler — obsługa pojedynczego klienta po stronie serwera.
 *
 * Rola klasy:
 * - opakowuje gniazdo sieciowe (Socket) jednego gracza,
 * - w osobnym wątku:
 *   * czyta linie tekstu od klienta,
 *   * przekazuje je do GameSession.handleClientMessage(...),
 * - udostępnia metodę sendLine(...) do wysyłania komunikatów do klienta,
 * - pamięta przypisany kolor gracza (BLACK lub WHITE).
 *
 * Wzorce:
 * - nie realizuje konkretnego wzorca GoF, ale współpracuje z:
 *   * Command (GameCommand wykonywane w GameSession),
 *   * Observer (GameSession jako GameObserver reaguje na zmiany w Game).
 */
public class ClientHandler implements Runnable {

    // gniazdo TCP dla tego klienta
    private final Socket socket;

    // referencja do sesji gry, której ten klient jest częścią
    private final GameSession session;

    // kolor gracza (BLACK / WHITE)
    private final PlayerColor color;

    // strumień wyjściowy do klienta, ustawiany w run()
    private PrintWriter out;

    public ClientHandler(Socket socket, GameSession session, PlayerColor color) {
        this.socket = socket;
        this.session = session;
        this.color = color;
    }

    /**
     * Zwraca kolor przypisany temu klientowi (BLACK/WHITE).
     */
    public PlayerColor getColor() {
        return color;
    }

    /**
     * Wysyła pojedynczą linię tekstu do klienta.
     * Jeśli out nie jest jeszcze ustawiony (np. przed startem run),
     * metoda nic nie zrobi.
     */
    public void sendLine(String line) {
        if (out != null) {
            out.println(line);
            out.flush();
        }
    }

    /**
     * Główna pętla wątku klienta:
     * - tworzy strumienie wejścia/wyjścia,
     * - wysyła powitalne INFO,
     * - czyta linie od klienta i przekazuje je do GameSession,
     * - kończy działanie po zamknięciu połączenia lub błędzie.
     */
    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            // ustawiamy strumień wyjściowy, którego używa sendLine(...)
            this.out = writer;

            // prosty komunikat informacyjny po połączeniu
            sendLine("INFO Connected as " + color.name());

            String line;
            // czytamy kolejne linie dopóki klient nie zamknie połączenia (readLine() == null)
            while ((line = in.readLine()) != null) {
                // przekazujemy wiadomość do GameSession
                session.handleClientMessage(this, line);
            }

            System.out.println("Client " + color + " disconnected (EOF).");
        } catch (IOException e) {
            System.out.println("Client " + color + " disconnected: " + e.getMessage());
        }
    }
}
