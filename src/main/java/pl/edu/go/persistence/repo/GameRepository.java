package pl.edu.go.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.go.persistence.entity.GameEntity;

/**
 * Repozytorium JPA dla {@link pl.edu.go.persistence.entity.GameEntity}.
 *
 * <p><b>Wzorzec projektowy:</b> Repository (Spring Data generuje implementację).</p>
 */
public interface GameRepository extends JpaRepository<GameEntity, Long> {
}
