package com.xiaolai.blog.controller.auth;

import com.xiaolai.blog.common.exception.BlogException;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ChangePasswordDTO;
import com.xiaolai.blog.mapper.UserMapper;
import com.xiaolai.blog.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final UserService users;
    private final UserMapper mapper;
    private final JdbcTemplate db;
    private final com.xiaolai.blog.service.AccountRecoveryService recovery;
    public AccountController(UserService users, UserMapper mapper, JdbcTemplate db, com.xiaolai.blog.service.AccountRecoveryService recovery) {
        this.users=users; this.mapper=mapper; this.db=db; this.recovery=recovery;
    }
    public record Profile(@NotBlank @Size(max=50) String nickname, @Size(max=500) String bio) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}

    @GetMapping
    public Result<?> me(Authentication auth) {
        var user=mapper.selectByUsername(auth.getName());
        var result=new java.util.HashMap<String,Object>();
        result.put("id",user.getId()); result.put("username",user.getUsername()); result.put("role",user.getRole());
        result.put("nickname",user.getNickname()); result.put("bio",user.getBio());
        var emails=db.queryForList("SELECT email,verified FROM account_email WHERE user_id=?",user.getId());
        result.put("email",emails.isEmpty()?"":emails.getFirst().get("email"));
        result.put("emailVerified",!emails.isEmpty() && ((Number)emails.getFirst().get("verified")).intValue()==1);
        result.put("recoveryEnabled",recovery.available());
        return Result.success(result);
    }

    @PutMapping
    public Result<?> profile(@Valid @RequestBody Profile dto,Authentication auth) {
        db.update("UPDATE `user` SET nickname=?,bio=? WHERE username=?",dto.nickname().trim(),dto.bio(),auth.getName());
        return me(auth);
    }

    @PutMapping("/password")
    public Result<?> password(@Valid @RequestBody ChangePasswordDTO dto) {
        if (dto.getOldPassword().equals(dto.getNewPassword())) throw new BlogException(422,"新密码应与旧密码不同");
        users.changePassword(dto.getOldPassword(),dto.getNewPassword());
        return Result.success();
    }

    @PostMapping("/email")
    public Result<?> bindEmail(@Valid @RequestBody EmailRequest dto,Authentication auth,jakarta.servlet.http.HttpServletRequest request) {
        recovery.verifyEmail(mapper.selectByUsername(auth.getName()).getId(),dto.email(),request);
        return Result.success("验证邮件已发送，请在 30 分钟内打开链接");
    }
}
