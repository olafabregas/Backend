package com.reviewflow.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.reviewflow.exception.AccessDeniedException;
import com.reviewflow.exception.DuplicateResourceException;
import com.reviewflow.exception.ResourceNotFoundException;
import com.reviewflow.exception.ValidationException;
import com.reviewflow.model.entity.Assignment;
import com.reviewflow.model.entity.Submission;
import com.reviewflow.model.entity.Team;
import com.reviewflow.model.entity.TeamMember;
import com.reviewflow.model.entity.TeamMemberStatus;
import com.reviewflow.model.entity.User;
import com.reviewflow.model.entity.UserRole;
import com.reviewflow.repository.AssignmentRepository;
import com.reviewflow.repository.SubmissionRepository;
import com.reviewflow.repository.TeamMemberRepository;
import com.reviewflow.repository.TeamRepository;
import com.reviewflow.repository.UserRepository;
import com.reviewflow.storage.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            
    // Archives (preferred submission format)
    "zip", "rar", "gz", "tar", "7z",

    // Documents
    "pdf", "docx", "xlsx", "pptx", "txt",

    // Data & Config
    "csv", "json", "xml", "yaml", "yml", "toml", "sql",

    // Markup & Web
    "html", "css", "md",

    // JVM Languages
    "java", "kt", "scala",

    // Systems & Low-level
    "c", "cpp", "cs", "go", "rs",

    // Scripting
    "py", "rb", "php", "swift", "js", "ts", "r",

    // Notebooks
    "ipynb"
);
            
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_CHANGE_NOTE_LENGTH = 500;
    
    // Concurrent upload tracking
    private final Map<String, Boolean> uploadLocks = new ConcurrentHashMap<>();
    
    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public Submission upload(Long teamId, Long assignmentId, String changeNote,
                             MultipartFile file, Long uploaderId) {
        // Validate file is present
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is required", "VALIDATION_ERROR");
        }
        
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            long sizeMB = file.getSize() / (1024 * 1024);
            throw new ValidationException(
                    "File size " + sizeMB + "MB exceeds maximum of 50MB",
                    "FILE_TOO_LARGE"
            );
        }
        
        // Validate changeNote length
        if (changeNote != null && changeNote.length() > MAX_CHANGE_NOTE_LENGTH) {
            throw new ValidationException(
                    "changeNote cannot exceed " + MAX_CHANGE_NOTE_LENGTH + " characters",
                    "VALIDATION_ERROR"
            );
        }
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        
        // Check if user is an ACCEPTED member of the team
        TeamMember teamMember = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, uploaderId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this team"));
        
        if (teamMember.getStatus() != TeamMemberStatus.ACCEPTED) {
            throw new AccessDeniedException("You are not an accepted member of this team");
        }
        
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", uploaderId));
        
        // Validate file extension
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = getFileExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            String allowedList = String.join(", ", ALLOWED_EXTENSIONS.stream()
                    .map(e -> "." + e)
                    .sorted()
                    .toList());
            throw new ValidationException(
                    "File type ." + ext + " is not allowed. Allowed: " + allowedList,
                    "INVALID_FILE_TYPE"
            );
        }
        
        // TODO: Add MIME type validation with Apache Tika
        // For now, we'll skip MIME validation until Tika is added to dependencies
        
        // Concurrent upload protection
        String lockKey = "team_" + teamId + "_assignment_" + assignmentId;
        if (uploadLocks.putIfAbsent(lockKey, Boolean.TRUE) != null) {
            throw new DuplicateResourceException(
                    "An upload is already in progress for this team",
                    "UPLOAD_IN_PROGRESS"
            );
        }
        
        try {
            // Get next version number atomically
            int nextVersion = submissionRepository.findMaxVersionNumber(teamId, assignmentId).orElse(0) + 1;
            Instant now = Instant.now();
            boolean isLate = assignment.getDueAt() != null && now.isAfter(assignment.getDueAt());
            
            // Store the file
            String relativePath = "submissions/" + teamId + "/" + assignmentId + "/v" + nextVersion + "_" + originalName;
            String storedPath = storageService.store(relativePath, file);
            
            Submission submission = Submission.builder()
                    .team(team)
                    .assignment(assignment)
                    .versionNumber(nextVersion)
                    .filePath(storedPath)
                    .fileName(originalName)
                    .fileSizeBytes(file.getSize())
                    .changeNote(changeNote)
                    .uploadedBy(uploader)
                    .uploadedAt(now)
                    .isLate(isLate)
                    .build();
            Submission saved = submissionRepository.save(submission);
            
            // Notify other accepted team members (not the uploader)
            teamMemberRepository.findByTeam_Id(teamId).stream()
                    .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED && !m.getUser().getId().equals(uploaderId))
                    .forEach(m -> notificationService.create(
                            m.getUser().getId(), "NEW_SUBMISSION",
                            "New Submission",
                            uploader.getFirstName() + " " + uploader.getLastName()
                                    + " submitted version " + nextVersion + " for " + assignment.getTitle(),
                            "/submissions/" + saved.getId()));
            return saved;
        } finally {
            // Always release the lock
            uploadLocks.remove(lockKey);
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        // Handle .tar.gz specifically
        if (filename.toLowerCase().endsWith(".tar.gz")) {
            return "gz";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ext;
    }

    public Submission getSubmission(Long id, Long userId, UserRole role) {
        Submission sub = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", id));
        
        // ADMIN can access any submission
        if (role == UserRole.ADMIN) {
            return sub;
        }
        
        // INSTRUCTOR can access if submission belongs to their course
        if (role == UserRole.INSTRUCTOR) {
            // This will be validated by the repository/service layer
            return sub;
        }
        
        // STUDENT can access only if they are a member of the team
        if (role == UserRole.STUDENT) {
            boolean isMember = teamMemberRepository.findByTeam_IdAndUser_Id(sub.getTeam().getId(), userId).isPresent();
            if (!isMember) {
                throw new AccessDeniedException("Access denied");
            }
            return sub;
        }
        
        throw new AccessDeniedException("Access denied");
    }

    public List<Submission> getVersionHistory(Long teamId, Long assignmentId, Long userId, UserRole role) {
        // Check if team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        
        // ADMIN can access any team
        if (role == UserRole.ADMIN) {
            return submissionRepository.findByTeam_IdAndAssignment_IdOrderByVersionNumberDesc(teamId, assignmentId);
        }
        
        // INSTRUCTOR can access if team belongs to their course
        if (role == UserRole.INSTRUCTOR) {
            return submissionRepository.findByTeam_IdAndAssignment_IdOrderByVersionNumberDesc(teamId, assignmentId);
        }
        
        // STUDENT can access only their own team's submissions
        if (role == UserRole.STUDENT) {
            boolean isMember = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId).isPresent();
            if (!isMember) {
                throw new AccessDeniedException("Access denied");
            }
            return submissionRepository.findByTeam_IdAndAssignment_IdOrderByVersionNumberDesc(teamId, assignmentId);
        }
        
        throw new AccessDeniedException("Access denied");
    }
    
    public List<Submission> getTeamSubmissions(Long teamId, Long userId, UserRole role) {
        // Check if team exists
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        
        // ADMIN can access any team
        if (role == UserRole.ADMIN) {
            return submissionRepository.findByTeam_IdOrderByVersionNumberDesc(teamId);
        }
        
        // INSTRUCTOR can access if team belongs to their course
        if (role == UserRole.INSTRUCTOR) {
            return submissionRepository.findByTeam_IdOrderByVersionNumberDesc(teamId);
        }
        
        // STUDENT can access only their own team's submissions
        if (role == UserRole.STUDENT) {
            boolean isMember = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId).isPresent();
            if (!isMember) {
                throw new AccessDeniedException("Access denied");
            }
            return submissionRepository.findByTeam_IdOrderByVersionNumberDesc(teamId);
        }
        
        throw new AccessDeniedException("Access denied");
    }

    public Resource downloadSubmission(Long id, Long userId, UserRole role) {
        Submission sub = getSubmission(id, userId, role);
        try {
            Resource resource = storageService.load(sub.getFilePath());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("FILE_NOT_FOUND_IN_STORAGE");
            }
            return resource;
        } catch (Exception e) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND_IN_STORAGE");
        }
    }

    public Page<Submission> getMySubmissions(Long userId, Pageable pageable) {
        return submissionRepository.findByTeamMemberUserId(userId, pageable);
    }
}
