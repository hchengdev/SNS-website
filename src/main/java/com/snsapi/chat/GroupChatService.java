package com.snsapi.chat;

import com.snsapi.user.FindUserResponse;
import com.snsapi.user.User;
import com.snsapi.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupChatService {

    private final GroupChatRepository groupChatRepository;
    private final GroupChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public GroupChatService(GroupChatRepository groupChatRepository, GroupChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.groupChatRepository = groupChatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    public GroupChat createGroup(String name) {
        Optional<GroupChat> existingGroup = groupChatRepository.findByName(name);
        if (existingGroup.isPresent()) {
            GroupChat newGroupChat = new GroupChat();
            newGroupChat.setName(name);
            newGroupChat.setMembers(new HashSet<>());
            return groupChatRepository.save(newGroupChat);
        }

        GroupChat groupChat = new GroupChat();
        groupChat.setName(name);
        groupChat.setMembers(new HashSet<>());
        return groupChatRepository.save(groupChat);
    }


    @Transactional
    public GroupChat addMember(Integer groupId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupChat groupChat = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("GroupChat not found"));

        Set<User> currentMembers = new HashSet<>(groupChat.getMembers());
        if (!currentMembers.contains(user)) {
            currentMembers.add(user);
            groupChat.setMembers(currentMembers);
        } else {
            throw new IllegalArgumentException("User already exists in this group.");
        }

        return groupChatRepository.save(groupChat);
    }


    public GroupChatMessage saveMessage(GroupChatMessage groupChatMessage) {
        if (groupChatMessage.getSender() == null || groupChatMessage.getGroupChat() == null) {
            throw new IllegalArgumentException("Sender and GroupChat must not be null");
        }
        groupChatMessage.setTimestamp(LocalDateTime.now());

        return chatMessageRepository.save(groupChatMessage);
    }

    public List<GroupChatMessage> getMessages(Integer groupId) {
        return chatMessageRepository.findByGroupChatId(groupId);
    }

    public void deleteMessage(Integer id) {
        chatMessageRepository.deleteById(id);
    }

    private UserGroupChatRes convertToUserRequest(User user) {
        return UserGroupChatRes.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .build();
    }
    public Set<UserGroupChatRes> getMembers(Integer groupId) {
        return groupChatRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"))
                .getMembers().stream()
                .map(this::convertToUserRequest)
                .collect(Collectors.toSet());
    }

    public List<UserGroupChatRes> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserRequest)
                .collect(Collectors.toList());
    }

    public List<GroupChat> getGroupsByUserId(Integer userId) {
        return groupChatRepository.findGroupsByUserId(userId);
    }

    public boolean isUserInGroup(Integer groupId, Integer userId) {
        Set<UserGroupChatRes> members = getMembers(groupId);
        return members.stream().anyMatch(member -> member.getId().equals(userId));
    }

    @Transactional
    public GroupChat removeMember(Integer groupId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupChat groupChat = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("GroupChat not found"));

        Set<User> currentMembers = new HashSet<>(groupChat.getMembers());
        if (currentMembers.contains(user)) {
            currentMembers.remove(user);
            groupChat.setMembers(currentMembers);
        } else {
            throw new IllegalArgumentException("User is not a member of this group.");
        }

        return groupChatRepository.save(groupChat);
    }


}