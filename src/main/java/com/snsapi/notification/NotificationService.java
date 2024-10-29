package com.snsapi.notification;

import com.snsapi.comment.Comment;
import com.snsapi.post.Post;
import com.snsapi.user.User;
import com.snsapi.user.UserServices;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServices userServices;

    public void createNotification(User sender, User recipient, String message, Notification.NotificationType notificationType, Post post, Comment comment) {
        if (sender == null || recipient == null) {
            throw new IllegalArgumentException("Sender and recipient must not be null");
        }
        Notification notification = Notification.builder()
                .sender(sender)
                .recipient(recipient)
                .message(message)
                .notificationType(notificationType)
                .post(post)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .isRead(false) // Thêm thuộc tính isRead nếu có
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Integer userId) {
        List<Notification> notifications = notificationRepository.findByRecipientId(userId);
        if (notifications.isEmpty()) {
            throw new EntityNotFoundException("No notifications found for user with ID: " + userId);
        }
        return notifications;
    }

    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.markAsRead(); // Giả sử phương thức này cập nhật trạng thái
        notificationRepository.save(notification);
    }

    public boolean notificationExists(Integer id) {
        return notificationRepository.existsById(id);
    }
}

