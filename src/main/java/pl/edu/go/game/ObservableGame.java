package pl.edu.go.game;

/**
 * Interfejs ObservableGame — "obserwowalna" gra.
 *
 * Wzorzec projektowy:
 * - Observer:
 *   - ObservableGame definiuje metody do rejestracji i wyrejestrowania
 *     obserwatorów stanu gry (GameObserver).
 *   - Konkretna implementacja (np. Game) powinna:
 *     * przechowywać listę GameObserver,
 *     * wywoływać ich metody przy każdej zmianie stanu (plansza, ruch, koniec gry).
 *
 * Rola interfejsu:
 * - oddziela "kontrakt" obserwowalnej gry od konkretnej klasy Game,
 * - pozwala w przyszłości podmienić implementację gry, zachowując ten sam interfejs
 *   dla obserwatorów (np. inna logika punktowania, różne zasady).
 */

public interface ObservableGame {
    void addObserver(GameObserver observer);
    void removeObserver(GameObserver observer);
}
