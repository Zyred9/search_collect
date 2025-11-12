package org.drinkless.robots.beans.initializer;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.drinkless.robots.beans.td.ClientManager;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.enums.AccountStatus;
import org.drinkless.robots.database.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Slf4j
@Component
public class InitializerHandler {

    @Resource private AccountService accountService;
    @Resource private ClientManager clientManager;

     @PostConstruct
     public void initAccounts() {
         this.reLoginAccounts();
     }

    /**
     * 服务器重启后，重新登录之前已登录的账号
     * <p>
     * 查询数据库中状态为 LOGGED_IN 的账号，依次调用 ClientManager.startAccountLogin 重新登录。
     * 如果某个账号登录失败，不影响其他账号的登录流程。
     * </p>
     */
    private void reLoginAccounts() {
        try {
            List<Account> loggedInAccounts = accountService.list(
                    Wrappers.<Account>lambdaQuery()
                            .eq(Account::getStatus, AccountStatus.LOGGED_IN)
            );
            if (CollUtil.isEmpty(loggedInAccounts)) {
                log.info("[账号恢复] 未发现需要重新登录的账号");
                return;
            }
            log.info("[账号恢复] 发现 {} 个已登录账号，准备重新登录", loggedInAccounts.size());
            int successCount = 0;
            int failedCount = 0;
            for (Account account : loggedInAccounts) {
                String phone = account.getPhone();
                try {
                    log.info("[账号恢复] 正在重新登录账号: {} (用户名: {}, ID: {})", 
                            phone, account.getUsername(), account.getUserId());
                    clientManager.startAccountLogin(phone);
                    successCount++;
                    log.info("[账号恢复] 账号 {} 重新登录成功", phone);
                } catch (Exception e) {
                    failedCount++;
                    log.error("[账号恢复] 账号 {} 重新登录失败，错误: {}", phone, e.getMessage(), e);
                }
            }
            log.info("[账号恢复] 账号重新登录完成！总计: {}, 成功: {}, 失败: {}", 
                    loggedInAccounts.size(), successCount, failedCount);
        } catch (Exception e) {
            log.error("[账号恢复] 查询或处理账号时发生异常: {}", e.getMessage(), e);
        }
    }
}
