package org.drinkless.robots.database.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.drinkless.robots.database.enums.SourceTypeEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 *
 * @author admin
 * @since 2025/11/12 9:16
 */
@Setter
@Getter
@Accessors(chain = true)
@TableName("t_collect")
public class Collect {

    @TableId(type = IdType.INPUT)
    private String id;
    private SourceTypeEnum type;
    private String sourceName;
    private String sourceUrl;
    private String channelName;
    private String channelUsername;
    private String channelUrl;
    private String subscribers;
    private Long chatId;
    private Long messageId;
    private LocalDateTime collectTime;
    private Integer times;
    private Integer views;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    private Boolean marked;
    private Integer score;

}
