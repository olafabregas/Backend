package com.reviewflow.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reviewflow.model.entity.AuditLog;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorId(Long actorId, Pageable pageable);

    Page<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
}
