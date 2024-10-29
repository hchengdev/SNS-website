package com.snsapi.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserGroupChatRes {
    Integer id;
    String name;
    String profilePicture;
}
