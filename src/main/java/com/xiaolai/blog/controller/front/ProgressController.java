package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ProgressDTO;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.entity.UserProgress;
import com.xiaolai.blog.mapper.UserMapper;
import com.xiaolai.blog.mapper.UserProgressMapper;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学习进度键值存取（刷题打卡、四级看课等），登录用户可用。
 * 值由前端生成（JSON 字符串），服务端只做存取与归属隔离。
 */
@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final UserProgressMapper progressMapper;
    private final UserMapper userMapper;

    public ProgressController(UserProgressMapper progressMapper, UserMapper userMapper) {
        this.progressMapper = progressMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/{key}")
    public Result<Map<String, String>> get(@PathVariable String key) {
        UserProgress row = ownedRow(key);
        return Result.success(Map.of("value", row == null ? "" : row.getValue() == null ? "" : row.getValue()));
    }

    @PutMapping("/{key}")
    public Result<Void> put(@PathVariable String key, @Valid @RequestBody ProgressDTO dto) {
        Long userId = currentUserId();
        UserProgress row = progressMapper.selectOne(new LambdaQueryWrapper<UserProgress>()
                .eq(UserProgress::getUserId, userId)
                .eq(UserProgress::getProgKey, key));
        if (row == null) {
            row = new UserProgress();
            row.setUserId(userId);
            row.setProgKey(key);
            row.setValue(dto.getValue());
            progressMapper.insert(row);
        } else {
            row.setValue(dto.getValue());
            progressMapper.updateById(row);
        }
        return Result.success();
    }

    private UserProgress ownedRow(String key) {
        Long userId = currentUserId();
        return progressMapper.selectOne(new LambdaQueryWrapper<UserProgress>()
                .eq(UserProgress::getUserId, userId)
                .eq(UserProgress::getProgKey, key));
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);
        return user == null ? -1L : user.getId();
    }
}
