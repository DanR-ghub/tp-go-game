package pl.edu.go.command;

import pl.edu.go.game.PlayerColor;
import pl.edu.go.move.Move;
import pl.edu.go.move.MoveFactory;

/**
 * {@code TextCommandFactory} mapuje surową linię tekstu protokołu na obiekt {@link GameCommand}.
 *
 * <p><b>Wzorce projektowe:</b>
 * <ul>
 *   <li><b>Simple Factory / Factory Method</b> — centralizuje tworzenie obiektów komend na podstawie tokenów.</li>
 *   <li><b>Command</b> — zwracane obiekty są implementacjami {@link GameCommand}.</li>
 * </ul>
 *
 * <p>Fabryka waliduje format wiadomości (liczbę argumentów, typy), ale nie waliduje reguł gry.
 */
public class TextCommandFactory {

    /**
     * Parsuje wiadomość protokołu i buduje odpowiadającą jej komendę.
     *
     * @param message linia z sieci (np. {@code "MOVE 3 4"})
     * @param player  gracz, w imieniu którego wykonujemy komendę
     * @return obiekt komendy gotowy do wykonania na {@code Game}
     * @throws IllegalArgumentException gdy format wiadomości jest niepoprawny lub komenda nieznana
     */
    public GameCommand fromNetworkMessage(String message, PlayerColor player) {
        if (message == null) {
            throw new IllegalArgumentException("Empty command");
        }

        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        String[] parts = trimmed.split("\\s+");
        String keyword = parts[0].toUpperCase();

        return switch (keyword) {
            case "MOVE" -> {
                if (parts.length != 3) {
                    throw new IllegalArgumentException("MOVE format: MOVE x y");
                }
                int x;
                int y;
                try {
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("MOVE coordinates must be integers: MOVE x y");
                }

                // MoveFactory buduje obiekt ruchu; walidacja legalności ruchu należy do Game/Board
                Move move = MoveFactory.createMove(player.toBoardColor(), x, y);
                yield new PlaceStoneCommand(move);
            }

            case "PASS" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("PASS takes no arguments");
                }
                yield new PassCommand(player);
            }

            case "RESIGN" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("RESIGN takes no arguments");
                }
                yield new ResignCommand(player);
            }

            case "AGREE" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("AGREE takes no arguments");
                }
                yield new AgreeCommand(player);
            }

            case "RESUME" -> {
                if (parts.length != 1) {
                    throw new IllegalArgumentException("RESUME takes no arguments");
                }
                yield new ResumeCommand(player);
            }

            default -> throw new IllegalArgumentException("Unknown command: " + keyword);
        };
    }
}
