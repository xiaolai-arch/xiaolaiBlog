package com.xiaolai.blog.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.dto.ProgressDTO;
import com.xiaolai.blog.entity.User;
import com.xiaolai.blog.entity.UserProgress;
import com.xiaolai.blog.mapper.UserMapper;
import com.xiaolai.blog.mapper.UserProgressMapper;
import com.xiaolai.blog.service.ProgressMerger;
import com.xiaolai.blog.common.exception.BlogException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 学习进度键值存取（刷题打卡、四级看课等），登录用户可用。
 * 值由前端生成（JSON 字符串），服务端只做存取与归属隔离。
 */
@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {
    private static final Set<String> ALLOWED_KEYS =
            Set.of("leetcode", "cet4", "agent", "lc-log", "study:log");

    private final UserProgressMapper progressMapper;
    private final UserMapper userMapper;
    private final ProgressMerger merger;

    public ProgressController(UserProgressMapper progressMapper, UserMapper userMapper, ProgressMerger merger) {
        this.progressMapper = progressMapper;
        this.userMapper = userMapper;
        this.merger = merger;
    }

    @GetMapping("/{key}")
    public Result<Map<String, String>> get(@PathVariable String key) {
        validateKey(key);
        UserProgress row = ownedRow(key);
        return Result.success(Map.of("value", row == null ? "" : row.getValue() == null ? "" : row.getValue()));
    }

    @PutMapping("/{key}")
    @Transactional
    public Result<Map<String, String>> put(@PathVariable String key, @Valid @RequestBody ProgressDTO dto) {
        validateKey(key);
        Long userId = currentUserId();
        if (userMapper.lockById(userId) == null) throw new BlogException(401, "请重新登录");
        progressMapper.ensureRow(userId, key);
        UserProgress row = progressMapper.lockRow(userId, key);
        row.setValue(merger.merge(key, row.getValue(), dto.getValue()));
        progressMapper.updateById(row);
        return Result.success(Map.of("value", row.getValue()));
    }

    private void validateKey(String key) {
        if (!ALLOWED_KEYS.contains(key)) throw new BlogException(422, "不支持的进度键");
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
        if (user == null) throw new BlogException(401, "请重新登录");
        return user.getId();
    }
}
