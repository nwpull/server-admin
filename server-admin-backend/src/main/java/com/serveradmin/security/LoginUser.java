package com.serveradmin.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String role;

    private static final ThreadLocal<LoginUser> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(LoginUser loginUser) {
        CURRENT_USER.set(loginUser);
    }

    public static LoginUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void clearCurrentUser() {
        CURRENT_USER.remove();
    }

    public static Long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    public static String getCurrentUsername() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getCurrentRole() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }
}
