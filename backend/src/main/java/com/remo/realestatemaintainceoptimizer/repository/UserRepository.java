package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persists accounts in the {@code users} table via Spring Data JPA.
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Returns the account with the given, already normalized username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Returns how many demo accounts currently exist.
     */
    long countByDemoAccountTrue();

    /**
     * Deletes every demo account that expired at or before the given instant, cascading to its data in the database.
     */
    @Modifying
    @Query("delete from User user where user.demoAccount = true and user.expiresAt <= :instant")
    int deleteDemoAccountsExpiredAt(@Param("instant") Instant instant);
}
