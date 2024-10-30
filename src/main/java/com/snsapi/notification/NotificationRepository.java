package com.snsapi.notification;

import com.snsapi.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
//    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
//
//    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);
//
//    Integer countByRecipientAndIsReadFalse(User recipient);

    List<Notification> findByRecipientId(Integer recipientId);
}
