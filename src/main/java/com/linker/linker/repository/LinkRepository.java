package com.linker.linker.repository;

import com.linker.linker.entity.Link;
import com.linker.linker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {
    @Query(value = "SELECT * FROM links WHERE new_url = :hash", nativeQuery = true)
    Optional<Link> findByHash(String hash);

    Page<Link> findByUser(User user, Pageable pageable);
}
