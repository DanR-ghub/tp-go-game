package pl.edu.go.command;

import pl.edu.go.game.PlayerColor;
import pl.edu.go.move.Move;
import pl.edu.go.move.MoveFactory;

public class TextCommandFactory {

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
                    throw new IllegalArgumentException("MOVE requires two integers: MOVE <x> <y>");
                }
                int x, y;
                try {
                    x = Integer.parseInt(parts[1]);
                    y = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("MOVE coordinates must be integers: MOVE <x> <y>");
                }
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
            default -> throw new IllegalArgumentException("Unknown command: " + keyword);
        };
    }
}
