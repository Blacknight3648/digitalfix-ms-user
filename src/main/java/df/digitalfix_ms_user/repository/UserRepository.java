package df.digitalfix_ms_user.repository;

import df.digitalfix_ms_user.model.UserEntity;
import df.digitalfix_ms_user.model.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUserEntraIdAndEstado(String userEntraId, EntityStatus estado);
    Optional<UserEntity> findByEmailAndEstado(String email, EntityStatus estado);
    Optional<UserEntity> findByIdAndEstado(Long id, EntityStatus estado);
}
