package org.drinkless.robots.config;

/**
 * <p>
 * 常量池
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public interface Constants {

    String KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCYUaqjWbGKtBbQEIxT24WcTYZcyKVb74MrCM/8lF/58PNwwZomKbi20cQP5KfKcA1rFTGXDWyFXILeF2DCc5bYSkPLebn2whPg0dnMwJjsls0KSmG18jVawo9wsiB/b4aKWrnXBasKS+jpuCVErgXMtjGqxAcnQiLFxmyk+BJM6QIDAQAB";

    String TOKEN_KEY = "BOT_TOKEN";

    String SUCCESS = "✅操作成功";

    String FAILED = "❌操作失败";

    String START_GROUP_URL = "https://t.me/{}?startgroup=true";

    String OPEN_TEXT = """
            #!/bin/bash
            
            if [ -z "$1" ]; then
                exit 1
            fi
            PORT=$1
            firewall-cmd --zone=public --add-port=$PORT/tcp --permanent
            firewall-cmd --reload
            """;

    CharSequence STR4_ = "61af79b04c9c8e70";
    CharSequence STR5_ = "71b4f0f67374c388";
    CharSequence STR6_ = "c0ab3262ce329c7d";
    CharSequence STR7_ = "7b82574f6bf87119";
    CharSequence STR8_ = "d7ce17da93c30fa5";

    String VAL_1 = "giIo5pvIhfInM1MJbmJVDtuwdg4gttuQMeAC/S/DwlONCj0gQDawKIKvDcOwBGKHPDZ9ClVr2OwKTtZ5rNBnyKcm/Do1DgCxGZGI2Bbk+UKz47+SWTfHM2x5cQnew/OhqYmPHM1ZEIHUb8NCDBotw4SqSZFu3Oy7xFHqI9hyKuU=";
    String VAL_2 = "Ig0whzp6AXrXpjjdlNXwyLEVEM2ww57tRFaPFRfeAmbUMhi5L1zk/K1RIST1xXpmCxWl2tCoR3mxPodB0fuh2rfmIcaxG/LZzCzpLl+K5AsdxarxgW37VF6mtyW73ZUCw4u/LqBTSMWU+Zv+KXdMjYHosuClklz+6JAxMUt6DIc=";
    String VAL_3 = "QXtqybeIprfZ+5oTKe+k1WsV95/u5HDOJMDm9T5EVl5HVUrPDjzvPOTZ/I7GL+Fg1DIsI/7aQV19b11qzRWrRIOA14SdMQ8KxaYsun1qIqmN3nDLyHSOLMHy4xvD+V3+59p4rSt1QdwWE1MiKmzljcDzxTYw1YCX4Vejpr4BDYk=";

    // ==================== 小号管理通知消息模板 ====================
    String ACCOUNT_CODE_ERROR = "小号 {} 验证码错误: {}";
    String ACCOUNT_CODE_SUCCESS = "小号 {} 验证码验证成功";
    String ACCOUNT_PASSWORD_ERROR = "小号 {} 密码错误: {}";
    String ACCOUNT_PASSWORD_SUCCESS = "小号 {} 密码验证成功";
    String ACCOUNT_PASSWORD_REQUIRED = "小号 {} 需要密码，请输入：密码#{}#您的密码";
    String ACCOUNT_LOGIN_SUCCESS = """
            小号 {} 登录成功！
            用户名: {}
            昵称: {}""";
    String ACCOUNT_CLOSED = "小号 {} 连接已关闭";
}


