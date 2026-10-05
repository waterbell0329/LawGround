package com.lawground.question.repository;

import com.lawground.question.entity.Question;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    boolean existsByOwnerIdAndContentHashAndDeletedAtIsNull(UUID ownerId, String contentHash);

    @EntityGraph(attributePaths = "choices")
    @Query(
            "select q from Question q where q.id = :id and q.deletedAt is null and (q.ownerId = :memberId or q.visibility = 'PUBLIC')")
    Optional<Question> findReadable(@Param("id") UUID id, @Param("memberId") UUID memberId);
}
