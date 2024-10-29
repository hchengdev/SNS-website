package com.snsapi.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, Integer> {
    List<GroupChatMessage> findByGroupChatId(Integer groupId);
}
