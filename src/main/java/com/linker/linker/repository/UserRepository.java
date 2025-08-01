package com.linker.linker.repository;

import com.linker.linker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO manual_password_change (email, hash, created_at)
        VALUES (:email, :hash, :created_at)
    """, nativeQuery = true)
    void insertIntoResetTable(
            @Param("email") String email,
            @Param("hash") String hash,
            @Param("created_at") LocalDateTime created_at
    );

    @Transactional
    @Query(
        value = """
            SELECT created_at FROM manual_password_change
            WHERE email = :email
            ORDER BY created_at
            DESC LIMIT 1
       """,
        nativeQuery = true
    )
    Optional<LocalDateTime> getCreatedAtTimeToResetPass(@Param("email") String email);

    @Transactional
    @Query(
        value = """
            SELECT created_at FROM manual_password_change
            WHERE hash = :hash
            ORDER BY created_at
            DESC LIMIT 1
       """,
        nativeQuery = true
    )
    Optional<LocalDateTime> getCreatedAtTimeToUpdatePass(@Param("hash") String hash);

    @Transactional
    @Query(
        value = """
            SELECT * FROM users
            WHERE email = (
                SELECT email FROM manual_password_change 
                WHERE hash = :hash
                ORDER BY created_at
                LIMIT 1
            )
        """,
        nativeQuery = true
    )
    Optional<User> getUserByHash(@Param("hash") String hash);
}
