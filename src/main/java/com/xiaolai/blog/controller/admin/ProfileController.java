package com.xiaolai.blog.controller.admin;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ChangePasswordDTO;
import com.xiaolai.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            return Result.badRequest("New password must differ from the old one");
        }
        userService.changePassword(dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }
}
