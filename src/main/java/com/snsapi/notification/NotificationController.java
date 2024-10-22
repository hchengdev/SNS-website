package com.snsapi.notification;

import com.snsapi.user.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationDTO>> getNotifications(@PathVariable Integer userId) {
        List<Notification> notifications = notificationService.getNotificationsForUser(userId);

        // Chuyển đổi danh sách Notification thành danh sách NotificationDTO
        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(notificationDTOs);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id) {
        // Xử lý trường hợp không tìm thấy thông báo
        if (!notificationService.notificationExists(id)) {
            return ResponseEntity.notFound().build(); // Trả về 404 Not Found
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok().build(); // Trả về 200 OK
    }

    // Phương thức chuyển đổi từ Notification sang NotificationDTO
    private NotificationDTO convertToDTO(Notification notification) {
        UserDTO senderDTO = UserDTO.builder()
                .id(notification.getSender().getId())
                .name(notification.getSender().getName())
                .profilePicture(notification.getSender().getProfilePicture())
                .build();

        UserDTO recipientDTO = UserDTO.builder()
                .id(notification.getRecipient().getId())
                .name(notification.getRecipient().getName())
                .profilePicture(notification.getRecipient().getProfilePicture())
                .build();

        return NotificationDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .sender(senderDTO)
                .recipient(recipientDTO)
                .build();
    }
}
