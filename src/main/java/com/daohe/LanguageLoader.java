package com.daohe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LanguageLoader {
    private static final Logger LOGGER = LogManager.getLogger(AutumnMod.MODID);
    private static final Map<String, Map<String, String>> LANGUAGE_MAPS = new HashMap<>();
    private static String currentLang = "en";
    private static final Map<String, String> LANG_MAPPING = new HashMap<String, String>() {{
        put("en", "en_US");
        put("cn", "zh_CN");
    }};

    static {
        loadLanguage("en_US");
        loadLanguage("zh_CN");
    }

    // 加载语言文件
    private static void loadLanguage(String lang) {
        try {
            String path = "/assets/" + AutumnMod.MODID + "/lang/" + lang + ".lang";
            InputStream is = LanguageLoader.class.getResourceAsStream(path);
            if (is == null) {
                LOGGER.error("Language file not found: {}", path);
                return;
            }
            Properties props = new Properties();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
            Map<String, String> translations = new HashMap<>();
            props.forEach((key, value) -> translations.put((String) key, (String) value));
            LANGUAGE_MAPS.put(lang, translations);
            is.close();
            LOGGER.info("Loaded language: {}", lang);
        } catch (Exception e) {
            LOGGER.error("Failed to load language file: {}", lang, e);
        }
    }

    // 设置当前语言
    public static void setLanguage(String lang) {
        if (LANG_MAPPING.containsKey(lang)) {
            currentLang = lang;
            LOGGER.info("Switched to language: {} (mapped to {})", lang, LANG_MAPPING.get(lang));
        } else {
            LOGGER.warn("Language not supported: {}", lang);
        }
    }

    // 获取当前语言
    public static String getCurrentLanguage() {
        return currentLang;
    }

    // 获取当前语言文件名称
    public static String getCurrentLanguageFile() {
        return LANG_MAPPING.getOrDefault(currentLang, "en_US");
    }

    // 格式化
    public static String format(String key, Object... args) {
        String fileLang = LANG_MAPPING.getOrDefault(currentLang, "en_US");
        Map<String, String> translations = LANGUAGE_MAPS.getOrDefault(fileLang, LANGUAGE_MAPS.get("en_US"));
        String template = translations.getOrDefault(key, key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            LOGGER.error("Error formatting translation for key: {}", key, e);
            return template;
        }
    }
}