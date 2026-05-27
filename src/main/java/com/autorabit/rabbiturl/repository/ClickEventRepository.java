package com.autorabit.rabbiturl.repository;

import com.autorabit.rabbiturl.model.ClickEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    @Query("SELECT CAST(c.clickedAt AS date) as day, COUNT(c) as count " +
            "FROM ClickEvent c WHERE c.shortCode = :shortCode " +
            "AND c.clickedAt >= :since " +
            "GROUP BY CAST(c.clickedAt AS date) ORDER BY day ASC")
    List<Object[]> findDailyClickCounts(
            @Param("shortCode") String shortCode,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
    List<Object[]> findDeviceBreakdown(@Param("shortCode") String shortCode);

    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortCode = :shortCode GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<Object[]> findBrowserBreakdown(@Param("shortCode") String shortCode);

    @Query("SELECT c.referrer, COUNT(c) as cnt FROM ClickEvent c " +
            "WHERE c.shortCode = :shortCode AND c.referrer IS NOT NULL " +
            "GROUP BY c.referrer ORDER BY cnt DESC")
    List<Object[]> findTopReferrers(
            @Param("shortCode") String shortCode,
            Pageable pageable
    );

    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c " +
            "WHERE c.shortCode = :shortCode AND c.country IS NOT NULL " +
            "GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> findCountryBreakdown(@Param("shortCode") String shortCode);

    long countByShortCode(String shortCode);

    @Modifying
    @Query("DELETE FROM ClickEvent c WHERE c.clickedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
