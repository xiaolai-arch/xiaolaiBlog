package com.xiaolai.blog.controller.admin;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.SiteConfig;
import com.xiaolai.blog.mapper.SiteConfigMapper;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("adminSiteConfigController")
@RequestMapping("/api/v1/admin/config")
public class SiteConfigController {

    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigController(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    @GetMapping
    public Result<Map<String, String>> getConfig() {
        List<SiteConfig> configs = siteConfigMapper.selectAll();
        Map<String, String> map = new HashMap<>();
        for (SiteConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return Result.success(map);
    }

    @PutMapping
    public Result<Void> updateConfig(@RequestBody Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            SiteConfig config = siteConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SiteConfig>()
                            .eq(SiteConfig::getConfigKey, entry.getKey()));
            if (config != null) {
                config.setConfigValue(entry.getValue());
                siteConfigMapper.updateById(config);
            } else {
                config = new SiteConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                siteConfigMapper.insert(config);
            }
        }
        return Result.success();
    }
}