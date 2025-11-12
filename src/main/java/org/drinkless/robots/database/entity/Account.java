package org.drinkless.robots.database.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.drinkless.robots.database.enums.AccountStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 小号管理实体类
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Setter
@Getter
@Accessors(chain = true)
@TableName("t_accounts")
public class Account {
    
    @TableId(type = IdType.INPUT)
    private String phone;

    private Long userId;
    private String username;
    private String nickname;

    private String password;
    private AccountStatus status;
    
    private LocalDateTime createdTime;


    public static Account buildDefault (String phone, String pwd) {
        return new Account()
                .setPhone(phone)
                .setPassword(pwd)
                .setStatus(AccountStatus.INITIALIZING)
                .setCreatedTime(LocalDateTime.now());
    }
}