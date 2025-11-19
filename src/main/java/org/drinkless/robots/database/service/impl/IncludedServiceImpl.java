package org.drinkless.robots.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.database.entity.Included;
import org.drinkless.robots.database.mapper.IncludedMapper;
import org.drinkless.robots.database.service.IncludedService;
import org.drinkless.robots.helper.RedisHelper;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 收录服务实现类
 *
 * @author admin
 * @since v 0.0.1
 */
@Slf4j
@Service
public class IncludedServiceImpl extends ServiceImpl<IncludedMapper, Included> implements IncludedService {

    @Override
    public void updateChat(Included chat) {
        RedisHelper.delete(Included.INCLUDED_PREFIX_KEY + chat.getId());

        Included included = this.baseMapper.selectById(chat.getId());
        if (Objects.nonNull(included)) {
            included.setNumber(chat.getNumber());
            included.setSourceCount(chat.getSourceCount());
            included.setIndexCreateTime(chat.getIndexCreateTime());

            this.baseMapper.updateById(included);
        }
    }
}
