package org.drinkless.robots.database.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.td.ClientManager;
import org.drinkless.robots.database.enums.AccountStatus;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.TdService;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author admin
 * @since 2025/11/11 15:55
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TdServiceImpl implements TdService {

    private final AccountService accountService;
    private final ClientManager clientManager;

    @Override
    public boolean login(String phone, String email, String pwd) {
        try {
            this.accountService.selectAccount(phone, email, pwd);
            this.clientManager.startAccountLogin(phone);
            return true;
        } catch (Exception ex) {
            log.error("[登录] 账号 {} 登录失败", phone, ex);
            this.accountService.updateStatus(phone, AccountStatus.LOGIN_FAILED);
            return false;
        }
    }

    @Override
    public boolean code(String phone, String code) {
        try {
            this.accountService.selectAccount(phone, "", "");
            this.clientManager.inputCodeForAccount(phone, code);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean offline(String phone) {
        try {
            this.clientManager.closeClient(phone);
            // 主动更新数据库状态为下线
            this.accountService.updateStatus(phone, AccountStatus.OFFLINE);
            return true;
        } catch (Exception ex) {
            log.error("[下线] 账号 {} 下线失败", phone, ex);
            return false;
        }
    }

    @Override
    public String history(String link, String inviteLink, int count, int weight) {
        return this.clientManager.fetchHistoryFromLink(link, inviteLink, count, weight);
    }

    @Override
    public String latest(long chatId, String url) {
        return this.clientManager.fetchLatestMessages(chatId, url);
    }

}
