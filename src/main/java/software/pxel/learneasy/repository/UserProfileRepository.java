package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.model.UserProfile;

import java.util.Collection;
import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Нужен для списочных выдач: профили страницы пользователей забираются
     * одним запросом, иначе на каждую строку списка приходился бы свой SELECT.
     */
    List<UserProfile> findAllByUserIdIn(Collection<Long> userIds);
}
