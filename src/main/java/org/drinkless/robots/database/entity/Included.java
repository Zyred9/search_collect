package org.drinkless.robots.database.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 *
 *
 * @author zyred
 * @since 2025/11/2 15:37
 */
@Setter
@Getter
@Accessors(chain = true)
@TableName(value = "t_included", autoResultMap = true)
public class Included {

    public static final String INCLUDED_PREFIX_KEY = "data:included:";

    /** 频道id/群组id **/
    @TableId(type = IdType.INPUT)
    private Long id;
    /** 人数 **/
    public Integer number;
    /** 群组/频道创建时间 **/
    private LocalDateTime indexCreateTime;
    /** 已记录资源数：29 **/
    private Integer sourceCount;

}
