package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.api.dto.admin.projection.AnalyticsSummaryProjection;
import software.pxel.learneasy.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query(value = """
            WITH
                UserActivity AS (
                    SELECT DISTINCT user_id, created_at
                    FROM test_attempts
                    WHERE passed = true
                ),
                Params AS (
                    SELECT
                        CASE
                            WHEN :period = 'day' THEN INTERVAL '1 day'
                            WHEN :period = 'week' THEN INTERVAL '7 days'
                            WHEN :period = 'month' THEN INTERVAL '30 days'
                            WHEN :period = 'year' THEN INTERVAL '365 days'
                            ELSE INTERVAL '30 days'
                        END AS interval_value
                )
            SELECT
                COALESCE(COUNT(DISTINCT u.id), 0) AS totalUsers,
            
                COALESCE(COUNT(DISTINCT CASE
                    WHEN u.created_at >= (NOW() - p.interval_value)
                    THEN u.id END), 0) AS newUsers,
            
                COALESCE(COUNT(DISTINCT ua.user_id), 0) AS activeUsers,
            
                COALESCE(COUNT(DISTINCT CASE
                    WHEN u.created_at < (NOW() - p.interval_value)
                    THEN ua.user_id END), 0) AS retainedUsers,
            
                COALESCE(COUNT(DISTINCT CASE
                    WHEN u.created_at < (NOW() - p.interval_value)
                    THEN u.id END), 0) AS totalOldUsers,
            
                (SELECT COALESCE(COUNT(DISTINCT id), 0)
                 FROM users
                 CROSS JOIN Params p2
                 WHERE created_at < (NOW() - p2.interval_value)) AS totalUsersPrevPeriod,
            
                (SELECT COALESCE(COUNT(DISTINCT user_id), 0)
                 FROM UserActivity
                 CROSS JOIN Params p2
                 WHERE created_at >= (NOW() - 2 * p2.interval_value)
                   AND created_at < (NOW() - p2.interval_value)) AS activeUsersPrevPeriod,
            
                (SELECT COALESCE(COUNT(DISTINCT ua.user_id), 0)
                 FROM UserActivity ua
                 CROSS JOIN Params p2
                 JOIN users u_inner ON ua.user_id = u_inner.id
                 WHERE u_inner.created_at < (NOW() - 2 * p2.interval_value)
                   AND ua.created_at >= (NOW() - 2 * p2.interval_value)
                   AND ua.created_at < (NOW() - p2.interval_value)) AS retainedUsersPrevPeriod,
            
                (SELECT COALESCE(COUNT(DISTINCT id), 0)
                 FROM users
                 CROSS JOIN Params p2
                 WHERE created_at < (NOW() - 2 * p2.interval_value)) AS totalOldUsersPrevPeriod
            
            FROM users u
            CROSS JOIN Params p
            LEFT JOIN UserActivity ua
                   ON u.id = ua.user_id
                  AND ua.created_at >= (NOW() - p.interval_value)
            """, nativeQuery = true)
    AnalyticsSummaryProjection getAnalyticsSummary(@Param("period") String period);
}
