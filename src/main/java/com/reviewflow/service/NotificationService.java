package com.reviewflow.service;

import com.reviewflow.exception.AccessDeniedException;
import com.reviewflow.exception.ResourceNotFoundException;
import com.reviewflow.model.entity.Notification;
import com.reviewflow.model.entity.User;
import com.reviewflow.repository.NotificationRepository;
import com.reviewflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification create(Long userId, String type, String title, String message, String actionUrl) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        return notificationRepository.save(n);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        // Check if notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to access this notification");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        
        // Check if notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to delete this notification");
        }
        
        notificationRepository.deleteById(id);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    public Page<Notification> getNotifications(Long userId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        }
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }
}
