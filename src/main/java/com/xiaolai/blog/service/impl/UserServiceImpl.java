package com.xiaolai.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.mapper.UserMapper;
import com.xiaolai.blog.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userMapper.selectByUsername(auth.getName());
    }

    @Override
    public void updateProfile(User user) {
        User current = getCurrentUser();
        if (current == null) throw new BlogException(401, "Not authenticated");
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, current.getId())
                .set(User::getNickname, user.getNickname())
                .set(User::getEmail, user.getEmail())
                .set(User::getAvatar, user.getAvatar())
                .set(User::getBio, user.getBio())
        );
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        User current = getCurrentUser();
        if (current == null) throw new BlogException(401, "Not authenticated");
        if (!passwordEncoder.matches(oldPassword, current.getPassword())) {
            throw new BlogException(400, "Old password is incorrect");
        }
        current.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, current.getId())
                .set(User::getPassword, current.getPassword())
        );
    }
}