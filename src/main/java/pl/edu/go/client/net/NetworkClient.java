package pl.edu.go.client.net;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * {@code NetworkClient} to prosta warstwa TCP dla klienta (GUI/CLI).
 *
 * <p>Odpowiada za:
 * <ul>
 *   <li>utrzymanie połączenia ({@link Socket}),</li>
 *   <li>asynchroniczny odczyt linii w osobnym wątku (read loop),</li>
 *   <li>delegowanie odebranych linii do callbacku {@code onLine},</li>
 *   <li>wysyłanie komend tekstowych metodą {@link #sendLine(String)}.</li>
 * </ul>
 */
public final class NetworkClient {

    /** Gniazdo TCP (null, gdy rozłączono). */
    private Socket socket;

    /** Strumień wejściowy w trybie liniowym (UTF-8). */
    private BufferedReader in;

    /** Strumień wyjściowy w trybie liniowym (UTF-8). */
    private BufferedWriter out;

    /** Wątek, który czyta linie z serwera i wywołuje {@code onLine}. */
    private Thread readerThread;

    /** Flaga pracy pętli odczytu; pozwala przerwać read loop przy rozłączaniu. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Callback wywoływany dla każdej odebranej linii protokołu. */
    private Consumer<String> onLine = s -> {};

    /** Callback wywoływany przy błędzie sieciowym podczas działania klienta. */
    private Consumer<Exception> onError = e -> {};

    /**
     * Ustawia callback dla odebranych linii.
     *
     * @param onLine funkcja obsługująca linie z serwera (nie może być null)
     */
    public void setOnLine(Consumer<String> onLine) {
        this.onLine = Objects.requireNonNull(onLine);
    }

    /**
     * Ustawia callback dla błędów sieciowych.
     *
     * @param onError funkcja obsługująca wyjątki (nie może być null)
     */
    public void setOnError(Consumer<Exception> onError) {
        this.onError = Objects.requireNonNull(onError);
    }

    /**
     * Nawiązuje połączenie z serwerem i uruchamia wątek odczytu.
     *
     * @param host adres serwera
     * @param port port serwera
     * @throws IOException gdy już połączono lub nie udało się połączyć
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (isConnected()) {
            throw new IOException("Already connected");
        }

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        running.set(true);
        readerThread = new Thread(this::readLoop, "NetworkClient-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Rozłącza klienta i zatrzymuje pętlę odczytu.
     * Metoda jest bezpieczna do wielokrotnego wywołania.
     */
    public synchronized void disconnect() {
        running.set(false);
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // ignore
        }
        socket = null;
        in = null;
        out = null;
    }

    /**
     * Sprawdza, czy klient jest aktualnie połączony.
     *
     * @return {@code true} jeśli socket istnieje i nie jest zamknięty
     */
    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Wysyła pojedynczą linię protokołu do serwera (zakończoną '\n').
     *
     * @param line linia do wysłania
     * @throws IOException jeśli klient nie jest połączony lub wystąpił błąd zapisu
     */
    public synchronized void sendLine(String line) throws IOException {
        if (!isConnected() || out == null) {
            throw new IOException("Not connected");
        }
        out.write(line);
        out.write("\n");
        out.flush();
    }

    /**
     * Pętla odczytu działająca w osobnym wątku.
     * Czyta linie dopóki {@code running == true} i strumień nie zostanie zamknięty.
     */
    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                onLine.accept(line);
            }
        } catch (Exception e) {
            if (running.get()) {
                onError.accept(e);
            }
        } finally {
            disconnect();
        }
    }
}
