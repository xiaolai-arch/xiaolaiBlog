package com.xiaolai.blog.controller.front;

import com.xiaolai.blog.common.response.Result;
import com.xiaolai.blog.entity.SiteConfig;
import com.xiaolai.blog.mapper.SiteConfigMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("frontSiteConfigController")
@RequestMapping("/api/v1/site")
public class SiteConfigController {

    private final SiteConfigMapper siteConfigMapper;

    public SiteConfigController(SiteConfigMapper siteConfigMapper) {
        this.siteConfigMapper = siteConfigMapper;
    }

    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        List<SiteConfig> configs = siteConfigMapper.selectAll();
        Map<String, String> map = new HashMap<>();
        for (SiteConfig config : configs) {
            if (!config.getConfigKey().startsWith("about_")) {
                map.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return Result.success(map);
    }

    @GetMapping("/about")
    public Result<Map<String, String>> about() {
        List<SiteConfig> configs = siteConfigMapper.selectAll();
        Map<String, String> map = new HashMap<>();
        for (SiteConfig config : configs) {
            if (config.getConfigKey().equals("about_content") || config.getConfigKey().equals("about_content_md")) {
                map.put(config.getConfigKey().replace("about_", ""), config.getConfigValue());
            }
        }
        return Result.success(map);
    }
}