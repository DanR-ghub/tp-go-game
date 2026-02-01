package pl.edu.go.persistence.entity;

import jakarta.persistence.*;
import pl.edu.go.game.PlayerColor;

import java.time.Instant;

/**
 * Minimalny zapis „ruchu” jako surowej komendy protokołu.
 * Replay nie dubluje logiki gry: odtwarza przez GameCommand.execute(game).
 */
@Entity
@Table(name = "game_commands", indexes = {
        @Index(name = "idx_game_seq", columnList = "gameId, seqNo")
})
public class GameCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameId;

    /** Numer kolejny komendy w danej grze (1..N). Kluczowy dla deterministycznego replay. */
    @Column(nullable = false)
    private Long seqNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerColor player;

    /** Surowa linia protokołu, np. "MOVE 3 4". */
    @Column(nullable = false, length = 128)
    private String raw;

    @Column(nullable = false)
    private Instant createdAt;

    /** Konstruktor dla JPA. */
    protected GameCommandEntity() {
        // JPA
    }

    public GameCommandEntity(long gameId, long seqNo, PlayerColor player, String raw) {
        this.gameId = gameId;
        this.seqNo = seqNo;
        this.player = player;
        this.raw = raw;
        this.createdAt = Instant.now();
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public PlayerColor getPlayer() {
        return player;
    }

    public String getRaw() {
        return raw;
    }
}
