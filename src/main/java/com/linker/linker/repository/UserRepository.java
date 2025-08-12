package com.linker.linker.repository;

import com.linker.linker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDateTime;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = """
                INSERT INTO manual_password_change (email, hash, created_at)
                VALUES (:email, :hash, :created_at)
            """, nativeQuery = true)
    void insertIntoResetTable(
            @Param("email") String email,
            @Param("hash") String hash,
            @Param("created_at") LocalDateTime created_at
    );

    @Query(
            value = """
                         SELECT created_at FROM manual_password_change
                         WHERE email = :email
                         ORDER BY created_at
                         DESC LIMIT 1
                    """,
            nativeQuery = true
    )
    LocalDateTime getCreatedAtTimeToResetPass(@Param("email") String email);

    @Query(
            value = """
                         SELECT created_at FROM manual_password_change
                         WHERE hash = :hash
                         ORDER BY created_at
                         DESC LIMIT 1
                    """,
            nativeQuery = true
    )
    LocalDateTime getCreatedAtTimeToUpdatePass(@Param("hash") String hash);

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

    @Modifying
    @Query(value = """
                INSERT INTO auto_password_change (email, secret_code, created_at, user_id, hash)
                VALUES (:email, :secretCode, :createdAt, :userId, :hash)
            """, nativeQuery = true)
    void saveSecretCode(
            @Param("email") String email,
            @Param("secretCode") String secretCode,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("userId") long userId,
            @Param("hash") String hash
    );

    @Query(
            value = """
                        SELECT user_id FROM auto_password_change
                        WHERE hash = :hash
                        ORDER BY created_at
                        LIMIT 1
                    """,
            nativeQuery = true
    )
    long getUserId(String hash);

    @Query(
            value = """
                        SELECT secret_code FROM auto_password_change
                        WHERE hash = :hash
                        ORDER BY created_at
                        LIMIT 1
                    """,
            nativeQuery = true
    )
    String getUserSecretCode(String hash);

    @Query(
            value = """
                         SELECT created_at FROM auto_password_change
                         WHERE email = :email
                         ORDER BY created_at
                         DESC LIMIT 1
                    """,
            nativeQuery = true
    )
    LocalDateTime getCreatedAtTimeToAutoResetPass(@Param("email") String email);

    @Query(
            value = """
                         SELECT created_at FROM auto_password_change
                         WHERE hash = :hash
                         ORDER BY created_at
                         DESC LIMIT 1
                    """, nativeQuery = true
    )
    LocalDateTime getCreatedAtTimeToAutoUpdatePass(@Param("hash") String hash);
}
