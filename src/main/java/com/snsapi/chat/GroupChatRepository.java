package com.snsapi.chat;

import com.snsapi.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupChatRepository extends JpaRepository<GroupChat, Integer> {
    @Query("SELECT g FROM GroupChat g WHERE EXISTS (SELECT 1 FROM g.members m WHERE m.id = :userId)")
    List<GroupChat> findGroupsByUserId(@Param("userId") Integer userId);
    Optional<GroupChat> findByName(String name);
}
