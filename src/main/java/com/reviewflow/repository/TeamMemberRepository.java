package com.reviewflow.repository;

import com.reviewflow.model.entity.TeamMember;
import com.reviewflow.model.entity.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeam_Id(Long teamId);

    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);

    List<TeamMember> findByUser_IdAndStatus(Long userId, TeamMemberStatus status);

    boolean existsByAssignment_IdAndUser_Id(Long assignmentId, Long userId);
    
    boolean existsByAssignment_IdAndUser_IdAndStatus(Long assignmentId, Long userId, TeamMemberStatus status);
    
    boolean existsByTeam_IdAndUser_IdAndStatus(Long teamId, Long userId, TeamMemberStatus status);

    List<TeamMember> findByAssignment_IdAndUser_IdAndStatus(Long assignmentId, Long userId, TeamMemberStatus status);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.assignment.id = :assignmentId AND tm.user.id = :userId")
    List<TeamMember> findByAssignmentIdAndUserId(@Param("assignmentId") Long assignmentId, @Param("userId") Long userId);

        @Query("""
            SELECT tm
            FROM TeamMember tm
            JOIN FETCH tm.user u
            WHERE tm.team.id IN :teamIds
              AND tm.status = :status
            """)
        List<TeamMember> findByTeamIdsAndStatusWithUser(@Param("teamIds") List<Long> teamIds,
            @Param("status") TeamMemberStatus status);
    
    long countByUser_Id(Long userId);
}
