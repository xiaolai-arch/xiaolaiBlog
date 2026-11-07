package com.xiaolai.blog.controller.auth;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.mapper.UserMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.xiaolai.blog.security.LoginRateLimiter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/// 访客注册：学习工具（笔记/算法/四级/Agent）的普通账号，角色固定 user，与管理员隔离
@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final com.xiaolai.blog.security.JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiter registrationLimiter = new LoginRateLimiter();

    public RegisterController(UserMapper userMapper,
                              PasswordEncoder passwordEncoder,
                              com.xiaolai.blog.security.JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public static class RegisterDTO {
        @NotBlank
        @Size(min = 3, max = 30, message = "用户名 3-30 个字符")
        private String username;
        @NotBlank
        @Size(min = 8, max = 64, message = "密码需要 8-64 位")
        private String password;
        @Size(max = 50)
        private String nickname;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        if (!registrationLimiter.tryAcquire(LoginRateLimiter.clientIp(request))) {
            return Result.error(429, "注册过于频繁，请稍后再试");
        }
        String username = dto.getUsername().trim();
        if (username.length() < 3 || username.length() > 30) return Result.badRequest("用户名 3-30 个字符");
        if (!username.matches("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+")) {
            return Result.badRequest("用户名只能包含中文、字母、数字、下划线或中划线");
        }
        if (dto.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return Result.badRequest("密码的 UTF-8 编码不能超过 72 字节");
        }
        if (userMapper.selectByUsername(username) != null) {
            return Result.badRequest("用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname((dto.getNickname() == null || dto.getNickname().isBlank())
                ? username : dto.getNickname().trim());
        user.setRole("user");
        user.setTokenVersion(0L);
        user.setLoginAttempts(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            return Result.badRequest("用户名已被占用");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getTokenVersion());
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());
        userMap.put("role", user.getRole());
        return Result.success(Map.of("token", token, "user", userMap));
    }
}
