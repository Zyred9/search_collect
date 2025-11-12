package org.drinkless.robots.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.drinkless.robots.database.entity.Config;
import org.drinkless.robots.database.mapper.ConfigMapper;
import org.drinkless.robots.database.service.ConfigService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {
}
