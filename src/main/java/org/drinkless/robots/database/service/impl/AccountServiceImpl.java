package org.drinkless.robots.database.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.enums.AccountStatus;
import org.drinkless.robots.database.mapper.AccountMapper;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.helper.StrHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>
 * 小号管理服务实现类
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {


    @Override
    public Account selectAccount(String phone, String email, String pwd) {
        phone = StrHelper.phoneNumber(phone);
        Account account = this.baseMapper.selectById(phone);
        if (Objects.isNull(account)) {
            account = Account.buildDefault(phone, pwd);
            account.setEmail(email);
            this.baseMapper.insert(account);
        }
        return account;
    }

    @Override
    public void addAccount(String phone, String secondaryPassword) {
        phone = StrHelper.phoneNumber(phone);

        Account account = this.getOne(
                Wrappers.<Account>lambdaQuery()
                        .eq(Account::getPhone, phone)
        );
        if (account != null) {
            account.setPassword(secondaryPassword);
            account.setStatus(AccountStatus.INITIALIZING);
            this.updateById(account);
            return;
        }
        account = new Account()
                .setPhone(phone)
                .setPassword(secondaryPassword)
                .setStatus(AccountStatus.INITIALIZING)
                .setCreatedTime(LocalDateTime.now());
        this.save(account);
    }

    @Override
    public void updateStatus(String phone, AccountStatus status) {
        Account Account = this.selectAccount(phone, "", "");
        if (Account != null) {
            Account.setStatus(status);
            this.updateById(Account);
        }
    }

    @Override
    public void updateTelegramInfo(String phoneNumber, Long telegramId, String username, String nickname, AccountStatus status) {
        phoneNumber = StrHelper.phoneNumber(phoneNumber);

        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Account::getPhone, phoneNumber);

        Account Account = this.getOne(wrapper);
        if (Account != null) {
            Account.setUserId(telegramId)
                    .setUsername(username)
                    .setNickname(nickname)
                    .setStatus(status);
            this.updateById(Account);
        }

    }

    @Override
    public Page<Account> getAccountPage(int page, int size, String phone, String username, Integer status) {
        Page<Account> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Account> wrapper = Wrappers.lambdaQuery();
        
        // 手机号模糊查询
        if (StrUtil.isNotBlank(phone)) {
            wrapper.like(Account::getPhone, phone);
        }
        
        // 用户名模糊查询
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(Account::getUsername, username);
        }
        
        // 状态精确查询
        if (status != null) {
            wrapper.eq(Account::getStatus, AccountStatus.values()[status]);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Account::getCreatedTime);
        
        return this.page(pageParam, wrapper);
    }
}