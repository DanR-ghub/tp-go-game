package pl.edu.go.client.gui;

/**
 * Klasa GameController — kontroler logiki w kliencie GUI.
 *
 * Wzorzec:
 * - MVC / MVP:
 *   - Łączy widok (BoardView) i model (GameModel).
 *
 * Rola klasy:
 * - reaguje na akcje użytkownika (kliknięcia w planszę, przyciski),
 * - wysyła odpowiednie komendy tekstowe do serwera (MOVE, PASS, RESIGN),
 * - w razie potrzeby aktualizuje model i odświeża widok.
 */
