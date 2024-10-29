
package com.snsapi.post;

import com.snsapi.comment.Comment;
import com.snsapi.comment.CommentDTO;
import com.snsapi.comment.CommentRepository;
import com.snsapi.like.LikeDTO;
import com.snsapi.media.Media;
import com.snsapi.media.MediaRepository;
import com.snsapi.user.User;
import com.snsapi.user.UserDTO;
import com.snsapi.user.UserRepository;
import com.snsapi.user.UserService;
import com.snsapi.utils.DateConverter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    @Value("${upload.image}")
    private String fileUpload;

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post save(Integer userId, String content, Post.VisibilityEnum visibility, MultipartFile[] files) {
        Post post = new Post();
        post.setUser(userService.findById(userId));
        post.setContent(content);
        post.setVisibility(visibility);

        post = postRepository.save(post);

        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Media media = new Media();
                    media.setPost(post);
                    media.setMediaType(saveFile(file));
                    media.setFileName(file.getOriginalFilename());
                    mediaRepository.save(media);
                }
            }
        }
        return post;
    }

    public Post updatePost(Integer postId, String content, Post.VisibilityEnum visibility, MultipartFile file) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại."));

        post.setContent(content);

        if (visibility != null) {
            post.setVisibility(visibility);
        }

        if (file != null && !file.isEmpty()) {
            Media media = new Media();
            media.setFileName(saveFile(file));
            media.setMediaType(file.getContentType());
            media.setPost(post);
            mediaRepository.save(media);
            post.addMedia(media);
        }

        return postRepository.save(post);
    }

    public void deletePost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại."));
        postRepository.delete(post);
    }

    private String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }
        try {
            Path path = Paths.get(fileUpload + File.separator + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path);
            return file.getOriginalFilename();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Post> searchPostByContent(String content) {
        return postRepository.findByContent(content);
    }

    public void toggleLikePost(Integer postId, Integer userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Bài viết không tồn tại."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại."));

        if (post.getLikeUsers().contains(user)) {
            post.getLikeUsers().remove(user);
        } else {
            post.getLikeUsers().add(user);
        }

        postRepository.save(post);
    }

    public int countLikes(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết không tồn tại!"));
        return post.getLikeUsers().size();
    }

    public List<User> getUsersWhoLikedPost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Bài viết không tồn tại."));
        return new ArrayList<>(post.getLikeUsers());
    }

    public CommentDTO convertToCommentDTO(Comment comment) {
        UserDTO userDTO = null;

        Optional<User> userOptional = userRepository.findById(comment.getUser().getId());
        if (userOptional.isPresent()){
            User user = userOptional.get();
            userDTO = new UserDTO(user.getId(), user.getName(), user.getProfilePicture());
        }
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setId(comment.getId());
        commentDTO.setPostId(comment.getPost() != null ? comment.getPost().getId() : null);
        commentDTO.setUserId(comment.getUser() != null ? comment.getUser().getId() : null);
        commentDTO.setContent(comment.getContent());
        commentDTO.setCreatedAt(DateConverter.localDateTimeToDateWithSlash(comment.getCreatedAt()));

        commentDTO.setCreatedBy(userDTO);

        List<CommentDTO> repliesDTO = comment.getReplies() != null ?
                comment.getReplies().stream()
                        .map(this::convertToCommentDTO)
                        .collect(Collectors.toList()) : new ArrayList<>();
        commentDTO.setReplies(repliesDTO);

        LikeDTO likeDTO = new LikeDTO();
        int likeCount = comment.getLikeUsers() != null ? comment.getLikeUsers().size() : 0;
        likeDTO.setLikeCount(likeCount);

        List<UserDTO> likeByUsers = comment.getLikeUsers() != null ?
                comment.getLikeUsers().stream()
                        .map(user -> {
                            UserDTO userDTO1 = new UserDTO();
                            userDTO1.setId(user.getId());
                            userDTO1.setName(user.getName());
                            userDTO1.setProfilePicture(user.getProfilePicture());
                            return userDTO1;
                        })
                        .collect(Collectors.toList()) : new ArrayList<>();

        likeDTO.setLikeByUsers(likeByUsers);
        commentDTO.setLikes(likeDTO);
        return commentDTO;
    }

    public Post findById(Integer postId) {
        return postRepository.findById(postId)
               .orElseThrow(() -> new EntityNotFoundException("Bài viết không tồn tại."));
    }
}
