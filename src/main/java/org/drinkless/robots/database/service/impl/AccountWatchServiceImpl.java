package org.drinkless.robots.database.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.drinkless.robots.database.entity.AccountWatch;
import org.drinkless.robots.database.mapper.AccountWatchMapper;
import org.drinkless.robots.database.service.AccountWatchService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountWatchServiceImpl extends ServiceImpl<AccountWatchMapper, AccountWatch> implements AccountWatchService {

    @Override
    public void upsertWatch(AccountWatch watch) {
        AccountWatch exists = this.getOne(Wrappers.<AccountWatch>lambdaQuery()
                .eq(AccountWatch::getPhone, watch.getPhone())
                .eq(AccountWatch::getChatId, watch.getChatId()));
        if (exists == null) {
            watch.setCreatedAt(LocalDateTime.now());
            watch.setUpdatedAt(LocalDateTime.now());
            this.save(watch);
            return;
        }
        exists.setChatType(watch.getChatType())
                .setChatUsername(watch.getChatUsername())
                .setChatTitle(watch.getChatTitle())
                .setJoinSource(watch.getJoinSource())
                .setInviteLink(watch.getInviteLink())
                .setWatchEnabled(watch.getWatchEnabled() == null ? exists.getWatchEnabled() : watch.getWatchEnabled())
                .setUpdatedAt(LocalDateTime.now());
        this.updateById(exists);
        return;
    }

    @Override
    public List<AccountWatch> findEnabledByPhone(String phoneNumber) {
        return this.list(Wrappers.<AccountWatch>lambdaQuery()
                .eq(AccountWatch::getPhone, phoneNumber)
                .eq(AccountWatch::getWatchEnabled, 1));
    }

    @Override
    public void updateLastMessage(String phoneNumber, long chatId, long lastMessageId) {
        AccountWatch watch = this.getOne(Wrappers.<AccountWatch>lambdaQuery()
                .eq(AccountWatch::getPhone, phoneNumber)
                .eq(AccountWatch::getChatId, chatId));
        if (watch != null && (watch.getLastMessageId() == null || lastMessageId > watch.getLastMessageId())) {
            watch.setLastMessageId(lastMessageId)
                    .setLastEventTime(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now());
            this.updateById(watch);
        }
    }
}
