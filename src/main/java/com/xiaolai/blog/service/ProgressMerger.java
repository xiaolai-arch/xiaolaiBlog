package com.xiaolai.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaolai.blog.common.exception.BlogException;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

@Component
public class ProgressMerger {
    private final ObjectMapper json;
    public ProgressMerger(ObjectMapper json) { this.json = json; }

    public String merge(String key, String stored, String incoming) {
        if (key.equals("study:log")) {
            validateStudyLog(stored);
            validateStudyLog(incoming);
            return incoming;
        }
        if (!Set.of("leetcode", "cet4", "agent", "lc-log").contains(key)) {
            throw new BlogException(422, "不支持的进度键");
        }
        try {
            JsonNode left = json.readTree(stored == null || stored.isBlank() ? "[]" : stored);
            JsonNode right = json.readTree(incoming);
            String result;
            if (key.equals("lc-log")) {
                Map<String, JsonNode> entries = new TreeMap<>();
                for (JsonNode source : new JsonNode[]{left, right}) {
                    if (!source.isArray()) throw new IllegalArgumentException();
                    for (JsonNode entry : source) {
                        if (!entry.path("pid").isTextual() || !entry.path("date").asText().matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
                        entries.put(entry.path("pid").asText() + "@" + entry.path("date").asText(), entry);
                    }
                }
                result = json.writeValueAsString(entries.values());
            } else {
                Map<String, JsonNode> items = new TreeMap<>();
                collect(left, items);
                collect(right, items);
                ObjectNode root = json.createObjectNode().put("version", 2);
                root.set("items", json.valueToTree(items));
                result = json.writeValueAsString(root);
            }
            if (result.length() > 200000) throw new BlogException(422, "合并后的进度内容超过 200KB，请先备份记录");
            return result;
        } catch (BlogException e) { throw e; }
        catch (Exception e) { throw new BlogException(422, "学习进度格式不正确"); }
    }

    private void validateStudyLog(String raw) {
        try {
            JsonNode value = json.readTree(raw == null || raw.isBlank() ? "[]" : raw);
            if (!value.isArray()) throw new IllegalArgumentException();
            for (JsonNode entry : value) {
                if (!entry.path("d").asText().matches("\\d{4}-\\d{2}-\\d{2}")
                        || !entry.path("k").isTextual() || entry.path("k").asText().isBlank()
                        || (entry.has("s") && (!entry.path("s").isIntegralNumber() || entry.path("s").asLong() < 0))) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            throw new BlogException(422, "学习日志格式不正确");
        }
    }

    private void collect(JsonNode source, Map<String, JsonNode> items) {
        if (source.isArray()) {
            for (JsonNode id : source) {
                if (!id.isValueNode() || id.isNull()) throw new IllegalArgumentException();
                items.putIfAbsent(id.asText(), json.createObjectNode().put("done", true).put("at", 0));
            }
            return;
        }
        if (source.path("version").asInt() != 2 || !source.path("items").isObject()) throw new IllegalArgumentException();
        source.path("items").fields().forEachRemaining(entry -> {
            JsonNode event = entry.getValue();
            if (!event.path("done").isBoolean() || !event.path("at").isIntegralNumber()
                    || event.path("at").asLong() < 0 || event.path("at").asLong() > 9007199254740991L) throw new IllegalArgumentException();
            JsonNode old = items.get(entry.getKey());
            if (old == null || event.path("at").asLong() > old.path("at").asLong()
                    || (event.path("at").asLong() == old.path("at").asLong() && !event.path("done").asBoolean())) {
                items.put(entry.getKey(), event);
            }
        });
    }
}
