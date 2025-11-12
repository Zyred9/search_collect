package org.drinkless.robots.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.drinkless.robots.database.entity.AccountWatch;

import java.util.List;

public interface AccountWatchService extends IService<AccountWatch> {

    void upsertWatch(AccountWatch watch);

    List<AccountWatch> findEnabledByPhone(String phoneNumber);

    void updateLastMessage(String phoneNumber, long chatId, long lastMessageId);
}
