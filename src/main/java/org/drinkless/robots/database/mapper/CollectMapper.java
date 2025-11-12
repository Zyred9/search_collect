package org.drinkless.robots.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.drinkless.robots.database.entity.Collect;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 小号管理Mapper接口
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Mapper
@Repository
public interface CollectMapper extends BaseMapper<Collect> {
}