package pl.edu.go;

import pl.edu.go.board.Board;
import pl.edu.go.move.MoveAdapter;

/**
 * {@code MainTest} to prosty punkt startowy do ręcznego testowania logiki (bez klient–serwer).
 *
 * <p>Umożliwia szybkie uruchomienie i podgląd działania {@link Board} oraz konwersji ruchów
 * (np. przez {@link MoveAdapter}) podczas debugowania.
 *
 * <p>Klasa pomocnicza — nie należy do właściwej aplikacji produkcyjnej.
 */
public class MainTest {

    /**
     * Uruchamia przykładowy scenariusz: tworzy planszę, wykonuje jeden ruch i wypisuje stan.
     *
     * @param args nieużywane
     */
    public static void main(String[] args) {
        Board board = new Board(5);

        System.out.println("Pusta plansza:");
        System.out.println(board);

        int[] pos = MoveAdapter.toInternal("B2");
        board.playMove(Board.BLACK, pos[0], pos[1]);

        System.out.println("Po ruchu czarnych B2:");
        System.out.println(board);
    }
}
