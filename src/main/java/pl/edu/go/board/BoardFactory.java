package pl.edu.go.board;

/**
 * Fabryka tworząca obiekty planszy gry Go.
 *
 * <p>
 * Uproszczona implementacja wzorca projektowego
 * Factory Method, umożliwiająca łatwą zmianę
 * sposobu tworzenia planszy w przyszłości.
 */
public class BoardFactory {

    /**
     * Tworzy nową planszę gry Go o podanym rozmiarze.
     *
     * @param size rozmiar planszy
     * @return nowy obiekt {@link Board}
     */
    public static Board createBoard(int size) {
        return new Board(size);
    }
}
