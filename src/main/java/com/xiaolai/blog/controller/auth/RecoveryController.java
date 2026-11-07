package com.xiaolai.blog.controller.auth;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.service.AccountRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class RecoveryController {
    private final AccountRecoveryService recovery;
    public RecoveryController(AccountRecoveryService recovery) { this.recovery=recovery; }
    public record ResetRequest(@NotBlank @Size(max=50) String username) {}
    public record Confirmation(@NotBlank @Size(max=64) String token,@Size(max=64) String password) {}
    @GetMapping("/recovery") public Result<?> capabilities() { return Result.success(java.util.Map.of("enabled",recovery.available())); }
    @PostMapping("/password/reset") public Result<?> reset(@Valid @RequestBody ResetRequest dto,jakarta.servlet.http.HttpServletRequest request) {
        recovery.requestReset(dto.username(),request);
        return Result.success("如果该账号已绑定验证邮箱，将收到找回邮件");
    }
    @PostMapping("/password/confirm") public Result<?> confirm(@Valid @RequestBody Confirmation dto) {
        recovery.confirm(dto.token(),"reset",dto.password()); return Result.success();
    }
    @PostMapping("/email/verify") public Result<?> verify(@Valid @RequestBody Confirmation dto) {
        recovery.confirm(dto.token(),"verify",null); return Result.success();
    }
}
