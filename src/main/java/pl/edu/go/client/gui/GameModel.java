package pl.edu.go.client.gui;

/**
 * Klasa GameModel — model danych dla klienta GUI.
 *
 * Wzorce:
 * - Observer / MVC:
 *   - Owijka na obiekt Game po stronie klienta.
 *   - Może implementować GameObserver i reagować na zmiany stanu gry,
 *     udostępniając dane widokowi (BoardView).
 *
 * Rola klasy:
 * - przechowuje bieżący stan gry z punktu widzenia klienta GUI,
 * - udostępnia gettery używane przez warstwę widoku.
 */