package com.xiaolai.blog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.LoginDTO;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.mapper.UserMapper;
import com.xiaolai.blog.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            return Result.unauthorized("Invalid username or password");
        }

        // Check account lock
        if (user.getLockTime() != null && user.getLockTime().plusMinutes(30).isAfter(LocalDateTime.now())) {
            return Result.forbidden("Account is locked for 30 minutes due to multiple login failures");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            // Reset login attempts on success (only update changed fields)
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getLoginAttempts, 0)
                    .set(User::getLockTime, null)
            );

            String token = jwtTokenProvider.generateToken(dto.getUsername());
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("nickname", user.getNickname());
            userMap.put("avatar", user.getAvatar());
            return Result.success(Map.of("token", token, "user", userMap));
        } catch (BadCredentialsException e) {
            // Increment login attempts (only update changed fields)
            int attempts = (user.getLoginAttempts() == null ? 0 : user.getLoginAttempts()) + 1;
            LocalDateTime lockTime = attempts >= 5 ? LocalDateTime.now() : null;
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, user.getId())
                    .set(User::getLoginAttempts, attempts)
                    .set(lockTime != null, User::getLockTime, lockTime)
            );
            return Result.unauthorized("Invalid username or password");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}