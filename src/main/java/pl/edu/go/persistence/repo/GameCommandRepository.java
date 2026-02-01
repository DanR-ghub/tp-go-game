package pl.edu.go.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.go.persistence.entity.GameCommandEntity;

import java.util.List;

/**
 * Repozytorium JPA dla {@link pl.edu.go.persistence.entity.GameCommandEntity}.
 *
 * <p>Udostępnia metodę pobrania komend w kolejności ({@code seqNo}) potrzebnej do replay.</p>
 */
public interface GameCommandRepository extends JpaRepository<GameCommandEntity, Long> {

    /**
     * Zwraca wszystkie komendy dla gry w kolejności wykonania.
     *
     * @param gameId id gry
     * @return lista komend uporządkowana rosnąco po {@code seqNo}
     */
    List<GameCommandEntity> findByGameIdOrderBySeqNoAsc(Long gameId);
}
