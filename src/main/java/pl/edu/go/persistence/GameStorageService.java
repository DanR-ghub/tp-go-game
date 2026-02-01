package pl.edu.go.persistence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.edu.go.board.Board;
import pl.edu.go.board.BoardFactory;
import pl.edu.go.command.GameCommand;
import pl.edu.go.command.TextCommandFactory;
import pl.edu.go.game.Game;
import pl.edu.go.game.GameResult;
import pl.edu.go.game.PlayerColor;
import pl.edu.go.persistence.entity.GameCommandEntity;
import pl.edu.go.persistence.entity.GameEntity;
import pl.edu.go.persistence.repo.GameCommandRepository;
import pl.edu.go.persistence.repo.GameRepository;

import java.util.List;

/**
 * Minimalna warstwa aplikacyjna do wymagania 3.2:
 * - zapis gry
 * - zapis komend
 * - zakończenie gry
 * - replay (ponowne wykonanie komend na świeżym Game)
 */
@Service
public class GameStorageService {

    private final GameRepository games;
    private final GameCommandRepository commands;

    private final TextCommandFactory factory = new TextCommandFactory();

    public GameStorageService(GameRepository games, GameCommandRepository commands) {
        this.games = games;
        this.commands = commands;
    }

    @Transactional
    public long startGame(int boardSize) {
        GameEntity g = new GameEntity(boardSize);
        return games.save(g).getId();
    }

    @Transactional
    public void recordCommand(long gameId, long seqNo, PlayerColor player, String raw) {
        if (gameId <= 0) return;
        if (raw == null || raw.trim().isEmpty()) return;
        commands.save(new GameCommandEntity(gameId, seqNo, player, raw.trim()));
    }

    @Transactional
    public void finishGame(long gameId, GameResult result) {
        if (gameId <= 0) return;
        GameEntity g = games.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono gry: " + gameId));
        PlayerColor winner = result == null ? null : result.getWinner();
        String reason = result == null ? null : result.getReason();
        g.markFinished(winner, reason);
        games.save(g);
    }

    @Transactional(readOnly = true)
    public void printReplay(long gameId) throws Exception {
        GameEntity meta = games.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono gry: " + gameId));
        List<GameCommandEntity> list = commands.findByGameIdOrderBySeqNoAsc(gameId);

        Board board = BoardFactory.createBoard(meta.getBoardSize());
        Game game = new Game(board);

        System.out.println("REPLAY gameId=" + gameId + " boardSize=" + meta.getBoardSize());

        for (GameCommandEntity e : list) {
            try {
                GameCommand cmd = factory.fromNetworkMessage(e.getRaw(), e.getPlayer());
                cmd.execute(game);
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Replay wywalił się na seqNo=" + e.getSeqNo()
                                + " gracz=" + e.getPlayer()
                                + " komenda=\"" + e.getRaw() + "\"",
                        ex
                );
            }

            System.out.println("# " + e.getSeqNo() + " " + e.getPlayer() + " -> " + e.getRaw());
            System.out.println(boardToAscii(game.getBoard()));
        }
    }

    private String boardToAscii(Board b) {
        int[][] s = b.getState();
        int n = s.length;
        StringBuilder out = new StringBuilder();

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int cell = s[x][y];
                char c = (cell == Board.BLACK) ? 'X' : (cell == Board.WHITE) ? 'O' : '.';
                out.append(c);
            }
            out.append('\n');
        }
        return out.toString();
    }
}
