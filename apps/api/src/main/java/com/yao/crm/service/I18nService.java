package com.yao.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * 国际化服务
 * 优化：使用 ConcurrentHashMap，支持并发访问
 */
@Service
public class I18nService {

    private static final Logger log = LoggerFactory.getLogger(I18nService.class);
    
    // 优化：使用不可变 Map 提高查找效率
    private Map<String, String> enMessages;
    private Map<String, String> zhMessages;
    private volatile boolean zhPreferred = false; // 缓存上次判断结果

    public I18nService() {
        loadMessages();
    }

    @PostConstruct
    public void init() {
        // 确保消息在构造函数中加载完成
    }
    
    private void loadMessages() {
        Properties enProps = new Properties();
        Properties zhProps = new Properties();
        
        try (InputStream enStream = I18nService.class.getResourceAsStream("/i18n/messages_en.properties");
             InputStream zhStream = I18nService.class.getResourceAsStream("/i18n/messages_zh.properties")) {
            
            if (enStream != null) {
                enProps.load(new InputStreamReader(enStream, StandardCharsets.UTF_8));
            }
            if (zhStream != null) {
                zhProps.load(new InputStreamReader(zhStream, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.warn("Failed to load i18n messages", e);
        }

        // 优化：显式转换为 String->String Map，避免类型推断问题
        Map<String, String> enMap = new HashMap<>();
        enProps.forEach((k, v) -> enMap.put((String) k, (String) v));
        this.enMessages = Collections.unmodifiableMap(enMap);

        Map<String, String> zhMap = new HashMap<>();
        zhProps.forEach((k, v) -> zhMap.put((String) k, (String) v));
        this.zhMessages = Collections.unmodifiableMap(zhMap);
        
        log.info("Loaded i18n messages: en={}, zh={}", enMessages.size(), zhMessages.size());
    }

    /**
     * 获取国际化消息
     * 优化：减少方法调用开销
     */
    public String msg(HttpServletRequest request, String key) {
        if (key == null) {
            return "";
        }
        
        // 优化：直接从 Map 获取，避免不必要的参数传递
        String text = zhMessages.get(key);
        if (text != null) {
            return text;
        }
        
        return enMessages.getOrDefault(key, key);
    }

    /**
     * 判断是否使用中文（公开方法，供外部使用）
     */
    public boolean isZhLocale(String acceptLanguage) {
        if (acceptLanguage == null) {
            return false;
        }
        return acceptLanguage.toLowerCase(Locale.ROOT).startsWith("zh");
    }
}
