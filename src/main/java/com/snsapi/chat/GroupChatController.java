package com.snsapi.chat;


import com.snsapi.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/groupMessages")
public class GroupChatController {

    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
    }

    @PostMapping("/create")
    public GroupChat createGroup(@RequestParam String name, @RequestParam Integer userId) {
        GroupChat groupChat = groupChatService.createGroup(name);
        groupChatService.addMember(groupChat.getId(), userId);
        return groupChat;
    }

    @PostMapping("/{groupId}/addMember/{userId}")
    public GroupChat addMember(@PathVariable Integer groupId, @PathVariable Integer userId) {
        return groupChatService.addMember(groupId, userId);
    }

    @PostMapping("/addMessage")
    public ResponseEntity<GroupChatMessage> sendMessage(@RequestBody GroupChatMessage groupChatMessage) {
        try {
            GroupChatMessage savedMessage = groupChatService.saveMessage(groupChatMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/delete/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Integer id) {
        groupChatService.deleteMessage(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/messages")
    public List<GroupChatMessage> getMessages(@PathVariable Integer groupId) {
        return groupChatService.getMessages(groupId);
    }

    @GetMapping("/{groupId}/members")
    public Set<UserGroupChatRes> getMembers(@PathVariable Integer groupId) {
        return groupChatService.getMembers(groupId);
    }

    @GetMapping("/allUsers")
    public List<UserGroupChatRes> getAllUsers() {
        return groupChatService.getAllUsers();
    }


    @GetMapping("/{userId}/groups")
    public ResponseEntity<List<GroupChat>> getUserGroups(@PathVariable Integer userId) {
        List<GroupChat> groups = groupChatService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }

    @DeleteMapping("/{groupId}/removeMember/{userId}")
    public ResponseEntity<GroupChat> removeMember(@PathVariable Integer groupId, @PathVariable Integer userId) {
        try {
            GroupChat updatedGroupChat = groupChatService.removeMember(groupId, userId);
            return ResponseEntity.ok(updatedGroupChat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }


}
