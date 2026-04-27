package com.xiaolai.blog.service;

import com.xiaolai.blog.entity.User;

public interface UserService {
    User getCurrentUser();
    void updateProfile(User user);
    void changePassword(String oldPassword, String newPassword);
}