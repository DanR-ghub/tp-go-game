package pl.edu.go.persistence.entity;

import jakarta.persistence.*;
import pl.edu.go.game.PlayerColor;

import java.time.Instant;

/**
 * Minimalny rekord gry (wymaganie 3.2): gra + metadane do replay.
 */
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer boardSize;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private PlayerColor winner;

    private String endReason;

    /** Konstruktor dla JPA. */
    protected GameEntity() {
        // JPA
    }

    public GameEntity(int boardSize) {
        this.boardSize = boardSize;
        this.startedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Integer getBoardSize() {
        return boardSize;
    }

    public void markFinished(PlayerColor winner, String reason) {
        this.finishedAt = Instant.now();
        this.winner = winner;
        this.endReason = reason;
    }
}
