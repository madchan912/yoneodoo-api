package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    long countByStatus(String status);

    List<Recipe> findByStatus(String status);

    @Query("""
            SELECT COUNT(r) FROM Recipe r
            WHERE r.status IS NULL
               OR (r.status <> 'SUCCESS' AND r.status <> 'NO_SUBTITLES' AND r.status <> 'SKIP')
            """)
    long countPendingOrUnknown();

    @Query("""
            SELECT r FROM Recipe r
            WHERE r.status IS NULL
               OR (r.status <> 'SUCCESS' AND r.status <> 'NO_SUBTITLES' AND r.status <> 'SKIP')
            ORDER BY r.createdAt DESC
            """)
    List<Recipe> findPendingForAdmin();
}