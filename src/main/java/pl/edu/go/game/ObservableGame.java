package pl.edu.go.game;

/**
 * {@code ObservableGame} definiuje kontrakt gry publikującej zdarzenia do {@link GameObserver}.
 *
 * <p><b>Wzorzec projektowy:</b> <b>Observer</b>.
 * Implementacja (np. {@link Game}) przechowuje listę obserwatorów i powiadamia ich o zmianach stanu.
 */
public interface ObservableGame {

    /**
     * Rejestruje obserwatora zdarzeń gry.
     *
     * @param observer obserwator do dodania
     */
    void addObserver(GameObserver observer);

    /**
     * Usuwa wcześniej zarejestrowanego obserwatora.
     *
     * @param observer obserwator do usunięcia
     */
    void removeObserver(GameObserver observer);
}
