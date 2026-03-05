package com.reviewflow.repository;

import com.reviewflow.model.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findBySubmission_Id(Long submissionId);

    List<Evaluation> findByInstructor_IdAndSubmission_Assignment_Course_Id(Long instructorId, Long courseId);

    List<Evaluation> findBySubmission_Assignment_Id(Long assignmentId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Evaluation e WHERE e.isDraft = false AND e.submission.team.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId) ORDER BY e.publishedAt DESC")
    org.springframework.data.domain.Page<Evaluation> findPublishedByTeamMemberUserId(@org.springframework.data.repository.query.Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
}
