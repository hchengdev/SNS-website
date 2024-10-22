package com.snsapi.post;

import com.snsapi.comment.CommentDTO;
import com.snsapi.like.LikeDTO;
import com.snsapi.media.MediaDTO;
import com.snsapi.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private Integer id;
    private Integer userId;
    private String content;
    private Post.VisibilityEnum visibility;
    private List<MediaDTO> media;
    private LikeDTO likes;
    private List<CommentDTO> comments;
    private String createdAt;
    private String updatedAt;
    private UserDTO createdBy;
}
