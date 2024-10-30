package com.snsapi.comment;

import com.snsapi.like.LikeDTO;
import com.snsapi.post.PostRepository;
import com.snsapi.user.User;
import com.snsapi.user.UserDTO;
import com.snsapi.user.UserServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class RestCommentController {

    private final CommentService commentService;
    private final PostRepository postRepository;
    private final UserServices userServices;

    private Optional<User> getUserFromPrincipal(Principal principal) {
        String email = principal.getName();
        return userServices.findByEmail(email);
    }

    @GetMapping
    public ResponseEntity<List<CommentDTO>> getAllComments() {
        List<CommentDTO> comments = commentService.getAllComments();
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Integer> countCommentsForPost(@PathVariable Integer postId) {
        int commentCount = commentService.countCommentsForPost(postId);
        return ResponseEntity.ok(commentCount);
    }

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody CommentDTO commentDTO) {
        if (commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung bình luận không được để trống.");
        }

        try {
            Integer userId = commentDTO.getUserId();
            Integer postId = commentDTO.getPostId();

            CommentDTO savedComment = commentService.saveComment(userId, postId, commentDTO.getContent());

            UserDTO userDTO = new UserDTO();
            userDTO.setId(savedComment.getUserId());
            userDTO.setName(savedComment.getCreatedBy().getName());
            userDTO.setProfilePicture(savedComment.getCreatedBy().getProfilePicture());

            savedComment.setCreatedBy(userDTO);

            return ResponseEntity.created(URI.create("/api/v1/comments/" + savedComment.getId())).body(savedComment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi lưu bình luận: " + e.getMessage());
        }
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentDTO> createReply(@PathVariable Integer commentId,
                                                  @RequestBody CommentDTO replyDTO,
                                                  Principal principal) {
        if (replyDTO.getContent() == null || replyDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        String email = principal.getName();
        Optional<User> user = userServices.findByEmail(email);

        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        CommentDTO savedReply = commentService.saveReply(user.get().getId(), replyDTO.getPostId(), commentId, replyDTO.getContent());

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReply);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Integer commentId,
                                           @RequestBody CommentDTO commentDTO,
                                           Principal principal) {
        Optional<User> user = getUserFromPrincipal(principal);
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        Comment existingComment = commentService.findCommentById(commentId);
        if (!existingComment.getUser().getId().equals(user.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền sửa bình luận này.");
        }

        if (commentDTO.getContent() == null || commentDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung bình luận không được để trống.");
        }

        CommentDTO updatedComment = commentService.updateComment(commentId, commentDTO.getContent());
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId, Principal principal) {
        Optional<User> user = getUserFromPrincipal(principal);
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        Comment existingComment = commentService.findCommentById(commentId);
        if (!existingComment.getUser().getId().equals(user.get().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xóa bình luận này.");
        }

        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}/likes")
    public ResponseEntity<?> toggleLikeComment(@PathVariable Integer commentId, Principal principal) {
        Optional<User> user = getUserFromPrincipal(principal);
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại.");
        }

        commentService.toggleLikeComment(commentId, user.get().getId());

        int likeCount = commentService.countLikes(commentId);
        List<User> likedUsers = commentService.getUsersWhoLikedComment(commentId);

        List<UserDTO> userDTOs = likedUsers.stream()
                .map(likedUser -> new UserDTO(likedUser.getId(), likedUser.getName(), likedUser.getProfilePicture()))
                .collect(Collectors.toList());

        LikeDTO response = new LikeDTO(likeCount, userDTOs);
        return ResponseEntity.ok(response);
    }
}
