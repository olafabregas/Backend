package com.reviewflow.repository;

import com.reviewflow.model.entity.CourseEnrollment;
import com.reviewflow.model.entity.CourseEnrollment.CourseEnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, CourseEnrollmentId> {

    List<CourseEnrollment> findByCourse_Id(Long courseId);

    Optional<CourseEnrollment> findByCourse_IdAndUser_Id(Long courseId, Long userId);

    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);

    void deleteByCourse_IdAndUser_Id(Long courseId, Long userId);

    long countByCourse_Id(Long courseId);
    
    long countByUser_Id(Long userId);
}
