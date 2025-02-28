package com.snsapi.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    @Query("SELECT COUNT(u) FROM Post p JOIN p.likeUsers u WHERE p.id = :postId")
    long countLikesByPostId(Integer postId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Post p JOIN p.likeUsers u " +
            "WHERE p.id = :postId AND u.id = :userId")
    boolean existsByPostIdAndUserId(Integer postId, Integer userId);

    @Query("SELECT p FROM Post p WHERE p.content LIKE %:content%")
    List<Post> findByContent(String content);
}
