package com.hoangdinh.taphoa.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Cấu hình đọc từ app.properties trong chương trình; có thể đặt file app.properties cạnh file jar để ghi đè
 * (vd: trỏ sang SQL Server khác mà không cần build lại).
 */
public final class AppConfig {

    private static Properties props;

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String v = props().getProperty(key);
        return v == null || v.isBlank() ? defaultValue : v.trim();
    }

    /** Nạp lần đầu khi cần (không nạp trong khối static để lỗi thiếu file hiện đúng thông báo). */
    private static synchronized Properties props() {
        if (props == null) {
            props = load();
        }
        return props;
    }

    private static Properties load() {
        Properties p = new Properties();
        boolean found = false;
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                found = true;
            }
            Path external = Path.of("app.properties");
            if (Files.isRegularFile(external)) {
                try (Reader r = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                found = true;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được app.properties: " + e.getMessage(), e);
        }
        if (!found) {
            // app.properties không đưa lên git; mỗi máy tự tạo từ file mẫu
            throw new IllegalStateException("thiếu file cấu hình app.properties. Hãy sao chép "
                    + "src/main/resources/app.properties.example thành app.properties rồi chạy lại");
        }
        return p;
    }
}
