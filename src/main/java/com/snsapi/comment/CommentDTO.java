package com.snsapi.comment;

import com.snsapi.like.LikeDTO;
import com.snsapi.user.UserDTO;
import com.snsapi.utils.DateConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Integer id;
    private Integer postId;
    private Integer userId;
    private String content;
    private String createdAt;
    private List<CommentDTO> replies;
    private LikeDTO likes;
    private UserDTO createdBy;

    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.postId = comment.getPost().getId();
        this.userId = comment.getUser().getId();
        this.content = comment.getContent();
        this.createdAt = DateConverter.localDateTimeToDateWithSlash(comment.getCreatedAt());

        if (comment.getReplies() != null) {
            this.replies = comment.getReplies().stream()
                    .map(CommentDTO::new)
                    .collect(Collectors.toList());
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(comment.getUser().getId());
        userDTO.setName(comment.getUser().getName());
        userDTO.setProfilePicture(comment.getUser().getProfilePicture());
        this.createdBy = userDTO;

        if (comment.getLikeUsers() != null) {
            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setLikeCount(comment.getLikeUsers().size());
            likeDTO.setLikeByUsers(comment.getLikeUsers().stream()
                    .map(user -> new UserDTO(user.getId(), user.getName(), user.getProfilePicture()))
                    .collect(Collectors.toList()));
            this.likes = likeDTO;
        }
    }
}
