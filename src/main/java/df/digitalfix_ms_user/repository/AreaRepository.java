package df.digitalfix_ms_user.repository;

import df.digitalfix_ms_user.model.AreaEntity;
import df.digitalfix_ms_user.model.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<AreaEntity, Long> {
    List<AreaEntity> findByEstadoOrderByNombreAsc(EntityStatus estado);
    Optional<AreaEntity> findByNombreIgnoreCase(String nombre);
}
