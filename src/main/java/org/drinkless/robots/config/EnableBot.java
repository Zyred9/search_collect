package org.drinkless.robots.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用 sparta，用于 springboot 启动类上
 * 主要工作：通过 @Import 注解初始化核心自动配置文件
 *
 * @author admin
 * @since v 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableConfigurationProperties(BotProperties.class)
public @interface EnableBot {

}
