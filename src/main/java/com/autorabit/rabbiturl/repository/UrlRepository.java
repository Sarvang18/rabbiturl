package com.autorabit.rabbiturl.repository;

import com.autorabit.rabbiturl.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<Url, UUID> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByCustomAlias(String customAlias);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCountByShortCode(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE Url u SET u.isActive = false WHERE u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);
}
