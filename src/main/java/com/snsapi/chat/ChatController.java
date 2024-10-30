package com.snsapi.chat;

import com.snsapi.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/messages")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return chatService.getAllUsers();
    }

    @GetMapping("/history")
    public List<ChatMessage> getChatHistory(@RequestParam Integer senderId, @RequestParam Integer receiverId) {
        return chatService.getChatMessages(senderId, receiverId);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Integer id) {
        chatService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }
}