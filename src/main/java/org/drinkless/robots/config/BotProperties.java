package org.drinkless.robots.config;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bots.config")
public class BotProperties {

    private boolean logs;

    /** 代理 **/
    private boolean enableBot = true;
    private boolean enableProxy;
    private String proxyType = "";
    private Integer proxyPort = 0;
    private String proxyHostName = "";
    /** 代理 **/
    private String botUsername;
    private Long backgroundGroupId;
    private Map<String, String> tokens;


    public String getToken () {
        return this.tokens.get(Constants.TOKEN_KEY);
    }

    public boolean fromBackground (Long chatId) {
        return this.backgroundGroupId.equals(chatId);
    }

    public void setBotUsername(String botUsername) {
        if (StrUtil.contains(botUsername, "_")) {
            botUsername = StrUtil.replace(botUsername, "_", "\\_");
        }
        this.botUsername = botUsername;
    }

    public String getApiKey() {
        return "";
    }
}
