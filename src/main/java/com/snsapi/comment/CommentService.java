package com.snsapi.comment;

import com.snsapi.like.LikeDTO;
import com.snsapi.notification.Notification;
import com.snsapi.notification.NotificationService;
import com.snsapi.post.Post;
import com.snsapi.post.PostRepository;
import com.snsapi.user.User;
import com.snsapi.user.UserDTO;
import com.snsapi.user.UserRepository;
import com.snsapi.user.UserServices;
import com.snsapi.utils.DateConverter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserServices userServices;
    private final NotificationService notificationService;

    public List<CommentDTO> getAllComments() {
        return commentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CommentDTO saveComment(Integer userId, Integer postId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Bài viết không tồn tại."));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        // Tạo thông báo cho người sở hữu bài viết
        String message = "Người dùng " + user.getName() + " đã bình luận: " + content;
        User postOwner = post.getUser(); // Lấy người sở hữu bài viết
        notificationService.createNotification(user, postOwner, message, Notification.NotificationType.COMMENT_POST, post, savedComment);

        // Chuyển đổi bình luận đã lưu thành DTO
        return convertToDTO(savedComment);
    }


    public CommentDTO updateComment(Integer commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));

        comment.setContent(content);

        Comment updateComment = commentRepository.save(comment);


        return convertToDTO(updateComment);
    }

    public void deleteComment(Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));
        commentRepository.delete(comment);
    }

    public CommentDTO saveReply(Integer userId, Integer postId, Integer parentCommentId, String content) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found"));

        Comment reply = new Comment();
        reply.setUser(user);
        reply.setPost(post);
        reply.setParentComment(parentComment);
        reply.setContent(content);

        Comment savedReply = commentRepository.save(reply);

        return new CommentDTO(savedReply);
    }

    public int countCommentsForPost(Integer postId) {
        return commentRepository.countByPostId(postId);
    }

    public void toggleLikeComment(Integer commentId, Integer userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại."));

        if (comment.getLikeUsers().contains(user)) {
            comment.getLikeUsers().remove(user);
        } else {
            comment.getLikeUsers().add(user);
        }

        commentRepository.save(comment);
    }

    public int countLikes(Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));
        return comment.getLikeUsers().size();
    }

    public List<User> getUsersWhoLikedComment(Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));
        return new ArrayList<>(comment.getLikeUsers());
    }

    public Comment findCommentById(Integer commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isPresent()) {
            return comment.get();
        } else {
            throw new RuntimeException("Comment not found with id " + commentId);
        }
    }


    private CommentDTO convertToDTO(Comment comment) {
        UserDTO userDTO = null;

        Optional<User> userOptional = userRepository.findById(comment.getUser().getId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            userDTO = new UserDTO(user.getId(), user.getName(), user.getProfilePicture());
        }

        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPost().getId());
        dto.setUserId(comment.getUser().getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(DateConverter.localDateTimeToDateWithSlash(comment.getCreatedAt()));
        dto.setReplies(comment.getReplies().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));

        dto.setCreatedBy(userDTO);

        List<CommentDTO> repliesDTO = comment.getReplies() != null ?
                comment.getReplies().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()) : new ArrayList<>();
        dto.setReplies(repliesDTO);

        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setLikeCount(comment.getLikeUsers().size());
        likeDTO.setLikeByUsers(comment.getLikeUsers().stream()
                .map(user -> {
                    UserDTO likeUserDTO = new UserDTO();
                    likeUserDTO.setId(user.getId());
                    likeUserDTO.setName(user.getName());
                    likeUserDTO.setProfilePicture(user.getProfilePicture());
                    return likeUserDTO;
                })
                .collect(Collectors.toList()));

        dto.setLikes(likeDTO);
        return dto;
    }

    public Comment findById(Integer commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Bình luận không tồn tại."));
    }
}
