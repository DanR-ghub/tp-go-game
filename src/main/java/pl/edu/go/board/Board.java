package pl.edu.go.board;

import pl.edu.go.model.Stone;
import pl.edu.go.model.StoneGroup;

import java.util.*;

/**
 * Klasa Board — logika gry Go dla Iteracji 1 (zasady 1–3).
 *
 * Wzorce projektowe:
 *
 * 1. Composite (Stone + StoneGroup)
 * - Kamień jest elementem, StoneGroup jest kompozytem.
 * - Board korzysta z kompozytu do wyszukiwania grup, liczenia oddechów
 * i usuwania całych łańcuchów jednym wywołaniem.
 *
 * 2. Adapter (MoveAdapter — używany poza Board)
 * - Board operuje na współrzędnych (x, y).
 * - Adapter tłumaczy notację użytkownika (np. „D4”) na współrzędne tablicy.
 * - Dzięki temu Board pozostaje czystym silnikiem gry.
 *
 * 3. Factory Method (BoardFactory — używany poza Board)
 * - Oddziela tworzenie planszy od jej używania.
 * - Ułatwia testowanie i ewentualne późniejsze rozszerzenia.
 *
 * Klasa implementuje logikę:
 * - stawiania kamieni,
 * - grup i oddechów,
 * - bicia kamieni przeciwnika,
 * - zakazu samobójstwa.
 */

public class Board {

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private final int size;
    private final int[][] board;

    public Board(int size) {
        this.size = size;
        this.board = new int[size][size];
    }

    private boolean inside(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    private List<int[]> neighbors(int x, int y) {
        List<int[]> n = new ArrayList<>();
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (inside(nx, ny))
                n.add(new int[] { nx, ny });
        }
        return n;
    }

    private StoneGroup getGroup(int x, int y) {
        int color = board[x][y];
        StoneGroup group = new StoneGroup(color);

        Set<String> visited = new HashSet<>();
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[] { x, y });

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int cx = pos[0], cy = pos[1];
            String key = cx + "," + cy;

            if (visited.contains(key))
                continue;
            visited.add(key);

            group.addStone(new Stone(cx, cy, color));

            for (int[] nb : neighbors(cx, cy)) {
                if (board[nb[0]][nb[1]] == color) {
                    stack.push(nb);
                }
            }
        }

        return group;
    }

    private int countLiberties(StoneGroup group) {
        Set<String> liberties = new HashSet<>();

        for (Stone s : group.getStones()) {
            for (int[] nb : neighbors(s.getX(), s.getY())) {
                if (board[nb[0]][nb[1]] == EMPTY) {
                    liberties.add(nb[0] + "," + nb[1]);
                }
            }
        }
        return liberties.size();
    }

    private void removeGroup(StoneGroup group) {
        for (Stone s : group.getStones()) {
            board[s.getX()][s.getY()] = EMPTY;
        }
    }

    public boolean playMove(int color, int x, int y) {

        if (!inside(x, y))
            return false;
        if (board[x][y] != EMPTY)
            return false;

        board[x][y] = color;

        int opp = (color == BLACK ? WHITE : BLACK);
        boolean captured = false; // zmienna pozwalająca określić czy dany ruch jest legalny

        for (int[] nb : neighbors(x, y)) {
            int nx = nb[0], ny = nb[1];
            if (board[nx][ny] == opp) {
                StoneGroup g = getGroup(nx, ny);
                if (countLiberties(g) == 0) {
                    removeGroup(g);
                    captured = true;
                }
            }
        }

        StoneGroup myGroup = getGroup(x, y);
        if (countLiberties(myGroup) > 0)
            return true;

        if (!captured) {
            board[x][y] = EMPTY;
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String[] symbols = { ".", "○", "●" };

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                sb.append(symbols[board[x][y]]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public int[][] getState() {
        int[][] copy = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(board[i], 0, copy[i], 0, size);
        return copy;
    }
}
