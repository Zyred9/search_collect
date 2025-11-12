package org.drinkless.robots.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.drinkless.robots.database.enums.WatchTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("t_account_watch")
public class AccountWatch {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long chatId;
    private String phone;
    private WatchTypeEnum chatType;       //
    private String chatUsername;   // 公开用户名(可空)
    private String chatTitle;      // 群/频道标题(可空)
    private String joinSource;     // INVITE_LINK / PUBLIC
    private String inviteLink;     // 邀请链接(可空)

    private Boolean watchEnabled;  // 1 开启, 0 关闭
    private Long lastMessageId;    // 已处理到的最后消息ID

    private LocalDateTime lastEventTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
