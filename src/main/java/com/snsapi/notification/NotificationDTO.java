package com.snsapi.notification;

import com.snsapi.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Integer id;
    private String message;
    private UserDTO sender;
    private UserDTO recipient;
}
