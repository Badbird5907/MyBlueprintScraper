package dev.badbird.scraper.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QueryBuilder {
    private Map<String, String> params;

    public QueryBuilder(String input) {
        params = new HashMap<>();
        if (input != null && !input.isEmpty()) {
            String[] split = input.split("&");
            for (String s : split) {
                String[] split1 = s.split("=");
                params.put(split1[0], split1[1]);
            }
        }
    }

    public QueryBuilder() {
        this("");
    }

    public QueryBuilder add(String key, Object value) {
        params.put(key, value.toString());
        return this;
    }

    public QueryBuilder remove(String key) {
        params.remove(key);
        return this;
    }

    public String build(boolean urlEncode) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.append((urlEncode ? URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) : entry.getKey()))
                    .append("=")
                    .append(urlEncode ? URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8) : entry.getValue())
                    .append("&");
        }
        return builder.toString();
    }
}
