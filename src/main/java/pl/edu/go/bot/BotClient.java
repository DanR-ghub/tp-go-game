package pl.edu.go.bot;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Heurystyczny bot gry Go.
 *
 * Priorytety:
 * 1. Obrona własnych grup (<= 2 oddechy)
 * 2. Zbijanie grup przeciwnika (1 oddech)
 * 3. Presja (zabieranie oddechów przeciwnika)
 * 4. Ruch pozycyjny (centrum, sąsiedztwo)
 *
 * Bot zapamiętuje odrzucone ruchy, więc nigdy się nie zapętla.
 */
public class BotClient implements Runnable {

    private static final int BOARD_SIZE = 9;
    private static final int EMPTY = 0;
    private static final int BLACK = 1;
    private static final int WHITE = 2;

    private final int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private final Random random = new Random();

    private PrintWriter out;

    private final Set<String> forbiddenMoves = new HashSet<>();
    private String lastMove = null;
    private boolean needMove = false;

    private Integer myColor = null;
    private Integer oppColor = null;

    // =====================================================
    // MAIN LOOP
    // =====================================================

    @Override
    public void run() {
        try (
                Socket socket = new Socket("localhost", 5001);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true)) {
            this.out = writer;

            String line;
            while ((line = in.readLine()) != null) {

                System.out.println("[BOT] " + line);

                if (line.startsWith("BOARD")) {
                    parseBoardSafely(line);
                    inferColorIfNeeded();
                }

                if (line.contains("Illegal move")) {
                    if (lastMove != null) {
                        forbiddenMoves.add(lastMove);
                    }
                    needMove = true;
                }

                if (line.contains("TURN")) {
                    needMove = true;
                }

                if (needMove && myColor != null) {
                    String move = decideMove();
                    lastMove = move;
                    System.out.println("[BOT ->] " + move);
                    out.println(move);
                    needMove = false;
                }
            }
        } catch (IOException e) {
            System.out.println("[BOT] ERROR: " + e.getMessage());
        }
    }

    // =====================================================
    // DECISION
    // =====================================================

    private String decideMove() {

        // 1️⃣ OBRONA (<= 2 oddechy)
        String defend = findCriticalMove(myColor, 2);
        if (defend != null)
            return defend;

        // 2️⃣ ATAK (1 oddech)
        String kill = findCriticalMove(oppColor, 1);
        if (kill != null)
            return kill;

        // 3️⃣ PRESJA (2 oddechy)
        String pressure = findCriticalMove(oppColor, 2);
        if (pressure != null)
            return pressure;

        // 4️⃣ POZYCJA
        String pos = positionalMove();
        if (pos != null)
            return pos;

        return "PASS";
    }

    // =====================================================
    // GROUP ANALYSIS
    // =====================================================

    private String findCriticalMove(int color, int targetLiberties) {
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];

        String bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] == color && !visited[x][y]) {

                    Set<String> liberties = new HashSet<>();
                    dfsGroup(x, y, color, visited, liberties);

                    if (liberties.size() == targetLiberties) {
                        for (String lib : liberties) {
                            String move = "MOVE " + lib;
                            if (forbiddenMoves.contains(move))
                                continue;

                            int score = scorePosition(lib);
                            if (score > bestScore) {
                                bestScore = score;
                                bestMove = move;
                            }
                        }
                    }
                }
            }
        }
        return bestMove;
    }

    private void dfsGroup(int x, int y, int color,
            boolean[][] visited,
            Set<String> liberties) {

        if (x < 0 || y < 0 || x >= BOARD_SIZE || y >= BOARD_SIZE)
            return;
        if (visited[x][y])
            return;

        visited[x][y] = true;

        if (board[x][y] == EMPTY) {
            liberties.add(x + " " + y);
            return;
        }

        if (board[x][y] != color)
            return;

        dfsGroup(x + 1, y, color, visited, liberties);
        dfsGroup(x - 1, y, color, visited, liberties);
        dfsGroup(x, y + 1, color, visited, liberties);
        dfsGroup(x, y - 1, color, visited, liberties);
    }

    // =====================================================
    // POSITIONAL MOVE
    // =====================================================

    private String positionalMove() {
        String bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                if (board[x][y] != EMPTY)
                    continue;

                String move = "MOVE " + x + " " + y;
                if (forbiddenMoves.contains(move))
                    continue;

                int score = scorePosition(x + " " + y);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    // =====================================================
    // POSITION SCORING
    // =====================================================

    private int scorePosition(String pos) {
        String[] p = pos.split(" ");
        int x = Integer.parseInt(p[0]);
        int y = Integer.parseInt(p[1]);

        int score = 0;

        // kara za bandę i róg
        if (x == 0 || y == 0 || x == BOARD_SIZE - 1 || y == BOARD_SIZE - 1)
            score -= 50;
        if ((x == 0 || x == BOARD_SIZE - 1) &&
                (y == 0 || y == BOARD_SIZE - 1))
            score -= 50;

        // bonus za centrum
        int cx = BOARD_SIZE / 2;
        int cy = BOARD_SIZE / 2;
        score -= (Math.abs(x - cx) + Math.abs(y - cy)) * 2;

        // bonus za sąsiedztwo własnych kamieni
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && ny >= 0 && nx < BOARD_SIZE && ny < BOARD_SIZE) {
                if (board[nx][ny] == myColor)
                    score += 20;
                if (board[nx][ny] == oppColor)
                    score += 10;
            }
        }
        return score;
    }

    // =====================================================
    // BOARD & COLOR
    // =====================================================

    private void parseBoardSafely(String line) {
        try {
            String[] parts = line.split(" ");
            int idx = 1;
            for (int y = 0; y < BOARD_SIZE; y++)
                for (int x = 0; x < BOARD_SIZE; x++)
                    board[x][y] = Integer.parseInt(parts[idx++]);
        } catch (Exception ignored) {
        }
    }

    private void inferColorIfNeeded() {
        if (myColor != null)
            return;

        int black = 0, white = 0;
        for (int[] row : board)
            for (int v : row)
                if (v == BLACK)
                    black++;
                else if (v == WHITE)
                    white++;

        myColor = (black <= white) ? BLACK : WHITE;
        oppColor = (myColor == BLACK) ? WHITE : BLACK;

        System.out.println("[BOT] My color = " +
                (myColor == BLACK ? "BLACK" : "WHITE"));
    }

    // =====================================================
    // ENTRY POINT
    // =====================================================

    public static void main(String[] args) {
        new Thread(new BotClient(), "Bot-Client").start();
    }
}
