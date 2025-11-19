package org.drinkless.robots.database.service.impl;


import org.drinkless.robots.beans.td.ClientManager;
import org.drinkless.robots.database.enums.AccountStatus;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.TdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author zyred
 * @since 2025/11/11 15:55
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TdServiceImpl implements TdService {

    private final AccountService accountService;
    private final ClientManager clientManager;

    @Override
    public boolean login(String phone, String pwd) {
        try {
            this.accountService.selectAccount(phone, pwd);
            this.clientManager.startAccountLogin(phone);
            return true;
        } catch (Exception ex) {
            this.accountService.updateStatus(phone, AccountStatus.LOGGED_IN);
            return false;
        }
    }

    @Override
    public boolean code(String phone, String code) {
        try {
            this.accountService.selectAccount(phone, "");
            this.clientManager.inputCodeForAccount(phone, code);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void history(String link, Long chatId, int count) {
        this.clientManager.fetchHistoryFromLink(link, count);
    }

}
