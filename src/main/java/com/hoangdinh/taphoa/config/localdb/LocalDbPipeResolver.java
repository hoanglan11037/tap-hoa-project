package com.hoangdinh.taphoa.config.localdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Khởi động LocalDB instance (nếu đang tắt) và lấy đường dẫn named pipe bằng lệnh SqlLocalDB. */
public final class LocalDbPipeResolver {

    private LocalDbPipeResolver() {
    }

    public static String resolve(String instance) {
        try {
            run("SqlLocalDB", "start", instance);
            for (String line : run("SqlLocalDB", "info", instance).split("\\R")) {
                int i = line.indexOf("np:");
                if (i >= 0) {
                    return line.substring(i + 3).trim();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không chạy được lệnh SqlLocalDB: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("Không tìm thấy LocalDB instance \"" + instance + "\"");
    }

    private static String run(String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            for (String line; (line = r.readLine()) != null; ) {
                out.append(line).append('\n');
            }
        }
        p.waitFor();
        return out.toString();
    }
}
