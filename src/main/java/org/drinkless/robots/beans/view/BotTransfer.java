package org.drinkless.robots.beans.view;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.drinkless.robots.helper.StrHelper;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Setter
@Getter
@Accessors(chain = true)
public class BotTransfer {

    private Long botId;
    private String botName;
    private String botToken;
    private String backgroundId;

    private String mysqlHost;
    private String mysqlPort;
    private String mysqlUser;
    private String mysqlPassword;
    private String mysqlDatabase;
    private String addr;

    public String buildText () {
        String text = """
                *主键*：`{}`
                *名字*：@{}
                *令牌*: `{}`
                *地址*：`{}`
                *数据库*：`{}` `{}`
                `{}`
                """;
        return StrUtil.format(text, this.botId, StrHelper.specialChar(this.botName), this.botToken, this.addr,
                this.mysqlUser, this.mysqlPassword, this.mysqlHost);
    }
}
