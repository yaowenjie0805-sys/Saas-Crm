package com.yao.crm.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class I18nService {

    private final Map<String, String> en = new HashMap<String, String>();
    private final Map<String, String> zh = new HashMap<String, String>();

    public I18nService() {
        loadMessages("/i18n/messages_en.properties", en);
        loadMessages("/i18n/messages_zh.properties", zh);
    }

    private void loadMessages(String resourcePath, Map<String, String> targetMap) {
        Properties props = new Properties();
        InputStream inputStream = null;
        InputStreamReader reader = null;
        try {
            inputStream = I18nService.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                props.load(reader);
                for (String key : props.stringPropertyNames()) {
                    targetMap.put(key, props.getProperty(key));
                }
            }
        } catch (IOException e) {
            // Silently ignore - maps will remain empty
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public String msg(HttpServletRequest request, String key) {
        if (request != null && isZh(request.getHeader("Accept-Language"))) {
            String text = zh.get(key);
            if (text != null) {
                return text;
            }
        }
        String fallback = en.get(key);
        return fallback == null ? key : fallback;
    }

    private boolean isZh(String acceptLanguage) {
        if (acceptLanguage == null) {
            return false;
        }
        return acceptLanguage.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}
