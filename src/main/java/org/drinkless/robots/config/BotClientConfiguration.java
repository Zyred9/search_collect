package org.drinkless.robots.config;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.TelegramOkHttpClientFactory;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Locale;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Slf4j
@Configuration
public class BotClientConfiguration {

    private final BotProperties properties;

    public BotClientConfiguration(BotProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TelegramClient telegramClient(OkHttpClient client) {
        if (this.properties.isEnableBot()) {
            return new OkHttpTelegramClient(client, this.properties.getToken());
        }
        return new DefaultTelegramClient();
    }

    @Bean
    public OkHttpClient client () {
        if (!this.properties.isEnableProxy()) {
            return new TelegramOkHttpClientFactory.DefaultOkHttpClientCreator().get();
        }

        log.info("[Bot] OkHttpClient proxy, hostname: {}, port:{}, type: {}",
                this.properties.getProxyHostName(), this.properties.getProxyPort(), this.properties.getProxyType());

        if (StrUtil.equals(this.properties.getProxyType(),
                Proxy.Type.SOCKS.name().toLowerCase(Locale.ROOT))) {
            return this.buildSocksClient(this.properties.getProxyHostName(),
                    this.properties.getProxyPort());
        }

        else if (StrUtil.equals(this.properties.getProxyType(),
                Proxy.Type.HTTP.name().toLowerCase(Locale.ROOT))) {
            return this.buildHttpClient(this.properties.getProxyHostName(),
                    this.properties.getProxyPort(), null, null);
        }
        return null;
    }


    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(OkHttpClient okClient) {
        return new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> okClient);
    }

    public OkHttpClient buildHttpClient(String hostname, int port, String username, String password) {
        return new TelegramOkHttpClientFactory.HttpProxyOkHttpClientCreator(
                () -> new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port)),
                () -> (route, response) -> {
                    if (StrUtil.isAllBlank(username, password)) {
                        return null;
                    }

                    String credential = Credentials.basic(username, password);
                    return response
                            .request()
                            .newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                }
        ).get();
    }

    private OkHttpClient buildSocksClient(String hostname, int port) {
        return new TelegramOkHttpClientFactory.SocksProxyOkHttpClientCreator(
                () -> new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostname, port))
        ).get();
    }
}
