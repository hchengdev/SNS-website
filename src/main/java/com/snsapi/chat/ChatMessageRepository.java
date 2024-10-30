package com.snsapi.chat;

import com.snsapi.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findBySenderAndReceiver(User sender, User receiver);
    List<ChatMessage> findBySenderOrReceiver(User user1, User user2);
}
