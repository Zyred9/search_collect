package org.drinkless.robots.database.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.enums.AccountStatus;

/**
 * <p>
 * 小号管理服务接口
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public interface AccountService extends IService<Account> {

    /**
     * 根据手机号查询小号
     */
    Account selectAccount(String phone, String pwd);


    /**
     * 添加新的小号
     */
    void addAccount(String phoneNumber, String secondaryPassword);

    /**
     * 更新小号状态
     */
    void updateStatus(String phoneNumber, AccountStatus status);

    /**
     * 更新小号的Telegram信息
     */
    void updateTelegramInfo(String phoneNumber, Long telegramId, String username, String nickname, AccountStatus status);

    /**
     * 分页查询账号列表
     * 支持按手机号、用户名、状态搜索
     */
    Page<Account> getAccountPage(int page, int size, String phone, String username, Integer status);

}