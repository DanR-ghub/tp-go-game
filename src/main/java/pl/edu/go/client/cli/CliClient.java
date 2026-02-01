package pl.edu.go.client.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import pl.edu.go.move.MoveAdapter;
import java.util.Locale;


/**
 * Klasa CliClient — klient konsolowy gry Go.
 *
 * Rola klasy:
 * - łączy się z serwerem (host + port),
 * - w osobnym wątku nasłuchuje komunikatów z serwera:
 *   * INFO, WELCOME, TURN, ERROR, END,
 *   * BOARD / ROW / END_BOARD — opis aktualnej planszy,
 * - parsuje BOARD / ROW / END_BOARD i rysuje planszę w czytelnej formie
 *   (siatka z numerami wierszy i kolumn),
 * - w głównej pętli czyta komendy użytkownika z klawiatury i wysyła je
 *   do serwera (MOVE x y, PASS, RESIGN),
 * - po otrzymaniu komunikatu END ... automatycznie kończy działanie.
 *
 * Klasa pełni rolę prostego interfejsu tekstowego (UI) dla gry Go.
 */
public class CliClient {

    // flaga sterująca główną pętlą; zmieniana przez wątek nasłuchujący
    private static volatile boolean running = true;

    public static void main(String[] args) {
        String host = "localhost";
        int port = 5001;

        // opcjonalne parametry: host port
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }

        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to " + host + ":" + port);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            // Czytanie komend z klawiatury
            Scanner scanner = new Scanner(System.in);
            System.out.println("Commands: MOVE <col> <row> (np. MOVE B 2) | PASS | RESIGN  (or: exit)");

            // Wątek nasłuchujący serwera (startujemy po wypisaniu komend,
            // żeby nie mieszać się z pierwszym rysowaniem planszy)
            Thread listener = new Thread(() -> listenToServer(in), "ServerListener");
            listener.setDaemon(true);
            listener.start();

            // Główna pętla: odczyt linii od użytkownika
            while (running && scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // użytkownik chce zakończyć klienta ręcznie
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                    break;
                }

                // Obsługa skróconego formatu: MOVE B 2 (kolumna jako litera)
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String upper = trimmed.toUpperCase(Locale.ROOT);

                // MOVE: tylko notacja literowa (B2 albo B 2). Inaczej błąd i nie wysyłamy nic.
                if (upper.startsWith("MOVE")) {
                    String payload = trimmed.substring(4).trim(); // wszystko po "MOVE"

                    try {
                        int[] pos = MoveAdapter.toInternal(payload); // akceptuje: B2, B 2
                        // wysyłamy do serwera 0-based (silnik)
                        out.println("MOVE " + pos[0] + " " + pos[1]);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("ERROR: " + ex.getMessage());
                        System.out.println("Allowed format: MOVE B2  or  MOVE B 2  (row counted from 1).");
                        // nie wysyłamy nic do serwera, użytkownik wpisuje ponownie
                    }

                } else {
                    // inne komendy (PASS/RESIGN/QUIT/itd.) lecą bez zmian
                    out.println(trimmed);
                }
            }
            System.out.println("Client exiting...");
        } catch (IOException e) {
            System.out.println("Cannot connect: " + e.getMessage());
        }
    }

    /**
     * Wątek nasłuchujący komunikatów z serwera.
     *
     * Tutaj parsujemy:
     * - BOARD <size>
     * - ROW <ciąg znaków X/O/.>
     * - END_BOARD
     *
     * oraz wypisujemy inne komunikaty (INFO, TURN, ERROR, END).
     */
    private static void listenToServer(BufferedReader in) {
        Integer boardSize = null;
        List<String> boardRows = new ArrayList<>();

        try {
            String line;
            while ((line = in.readLine()) != null) {

                // ---- Parsowanie planszy (BOARD/ROW/END_BOARD) ----

                if (line.startsWith("BOARD ")) {
                    // początek nowej planszy
                    try {
                        boardSize = Integer.parseInt(line.substring("BOARD ".length()).trim());
                    } catch (NumberFormatException e) {
                        boardSize = null;
                    }
                    boardRows.clear();
                    // nie wypisujemy surowej linii BOARD
                    continue;
                }

                if (line.startsWith("ROW ")) {
                    // kolejny wiersz planszy
                    if (boardSize != null) {
                        String row = line.substring("ROW ".length());
                        boardRows.add(row);
                    }
                    // nie wypisujemy surowej linii ROW
                    continue;
                }

                if ("END_BOARD".equals(line)) {
                    // koniec opisu planszy -> rysujemy ją
                    if (boardSize != null && boardRows.size() == boardSize) {
                        displayBoard(boardSize, boardRows);
                    } else {
                        System.out.println("(Received incomplete board data)");
                    }
                    boardSize = null;
                    boardRows.clear();
                    continue;
                }

                // ---- Inne komunikaty ----

                System.out.println(line);

                // koniec gry -> kończymy klienta
                if (line.startsWith("END ")) {
                    running = false;
                    break;
                }
            }

            System.out.println("Server closed connection.");
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
            running = false;
        }
    }

    /**
     * Rysuje planszę w terminalu na podstawie listy wierszy ('.', 'X', 'O').
     *
     * Przykład:
     *      A B C D E
     *   1  . . X . .
     *   2  . O . . .
     *   ...
     */
    private static void displayBoard(int size, List<String> rows) {
        System.out.println();
        System.out.println("Current board:");

        // nagłówek z literami kolumn (A, B, C, ...)
        System.out.print("    ");
        for (int x = 0; x < size; x++) {
            char col = (char) ('A' + x);
            System.out.print(col + " ");
        }
        System.out.println();


        // każdy wiersz planszy
        for (int y = 0; y < size; y++) {
            String row = rows.get(y);

            // numer wiersza z lewej
            System.out.printf("%2d  ", y+1);

            for (int x = 0; x < size; x++) {
                char c = (x < row.length()) ? row.charAt(x) : '.';

                // mapowanie na ładniejsze symbole
                char symbol = switch (c) {
                    case 'X' -> '●';  // black
                    case 'O' -> '○';  // white
                    case '.' -> '.';
                    default -> c;
                };

                System.out.print(symbol + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
