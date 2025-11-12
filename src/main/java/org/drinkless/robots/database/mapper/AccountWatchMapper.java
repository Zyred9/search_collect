package org.drinkless.robots.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.drinkless.robots.database.entity.AccountWatch;
import org.springframework.stereotype.Repository;


@Mapper
@Repository
public interface AccountWatchMapper extends BaseMapper<AccountWatch> {
}
