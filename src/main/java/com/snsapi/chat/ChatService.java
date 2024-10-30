package com.snsapi.chat;

import com.snsapi.user.User;
import com.snsapi.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<ChatMessage> getChatMessages(Integer senderId, Integer receiverId) {
        User sender = userRepository.findById(senderId).orElse(null);
        User receiver = userRepository.findById(receiverId).orElse(null);
        return chatMessageRepository.findBySenderAndReceiver(sender, receiver);
    }

    public void sendMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        chatMessageRepository.save(message);
    }
    public void deleteMessage(Integer id) {
        chatMessageRepository.deleteById(id);
    }
}