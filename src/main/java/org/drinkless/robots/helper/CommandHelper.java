package org.drinkless.robots.helper;

import cn.hutool.core.io.FileUtil;
import org.drinkless.robots.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Slf4j
public class CommandHelper {

    public static boolean processorCommand (String port) {
        try {
            String openText = Constants.OPEN_TEXT;
            File tempFile = FileUtil.createTempFile("open", ".sh", true);
            FileUtil.writeString(openText, tempFile, Charset.defaultCharset());

            String[] command = {"sudo", tempFile.getAbsolutePath(), port};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            FileUtil.del(tempFile);
            return exitCode == 0;
        } catch (Exception e) {
            log.error("[初始化失败 -》 processorCommand]", e);
            return false;
        }
    }

    public static String getAddr () {
        try {
            String command = "curl https://ipinfo.io/ip";
            return exec(command);
        } catch (Exception e) {
            return null;
        }
    }

    public static String ping(String ping) {
        String command = "ping -c 5 " + ping;
        try {
            return exec(command);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull
    private static String exec(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        process.waitFor();
        return sb.toString();
    }
}
