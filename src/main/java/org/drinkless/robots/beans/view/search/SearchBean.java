package org.drinkless.robots.beans.view.search;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
@Document(indexName = "search_index")
public class SearchBean {

    // ==================== 核心标识字段 ====================

    /** 文档唯一标识(Elasticsearch主键) **/
    @Id
    private String id;

    /** 内容类型(频道/群组/视频/图片/音频/文本/机器人/文件) **/
    @Field(type = FieldType.Keyword)
    private SourceTypeEnum type;

    // ==================== 资源基础信息 ====================

    /** 资源名称(核心搜索字段,使用标准分词器) **/
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String sourceName;

    /** 资源链接(Telegram消息链接) **/
    @Field(type = FieldType.Keyword)
    private String sourceUrl;


    // ==================== 频道/群组信息 ====================

    /** 频道/群组名称(发布者) **/
    @Field(type = FieldType.Keyword)
    private String channelName;

    /** 频道/群组用户名 **/
    @Field(type = FieldType.Keyword)
    private String channelUsername;

    /** 频道/群组链接 **/
    @Field(type = FieldType.Keyword)
    private String channelUrl;

    /** 订阅人数/成员数(10k、45等) **/
    @Field(type = FieldType.Keyword)
    private String subscribers;

    // ==================== 消息元数据 ====================
    /** 群组id **/
    @Field(type = FieldType.Long)
    private Long chatId;

    /** 消息id **/
    @Field(type = FieldType.Long)
    private Long messageId;

    /** 收集时间(查询根据此字段降序排序) **/
    @Field(type = FieldType.Long)
    private LocalDateTime collectTime;

    // ==================== 媒体资源特有字段 ====================

    /** 时间长度(视频和音频专用,单位:秒,用于排序) **/
    @Field(type = FieldType.Integer)
    private Integer times;

    /** 浏览量(视频、图片、音频) **/
    @Field(type = FieldType.Integer)
    private Integer views;

    // ==================== 标签与分类 ====================

    /** 标签/关键词列表 **/
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    // ==================== 内容审核与质量 ====================

    /** 是否被标记为内容 **/
    @Field(type = FieldType.Boolean)
    private Boolean marked;

}
