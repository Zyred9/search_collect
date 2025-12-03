package org.drinkless.robots.database.service;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.beans.view.base.PageResult;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 搜索服务接口
 *
 * @author zyred
 * @since 1.0
 */
public interface SearchService {

    /**
     * 批量保存搜索文档到 Elasticsearch
     *
     * @param searchBeans 搜索文档列表
     */
    void batchSave(List<SearchBean> searchBeans);

    /**
     * 保存单个搜索文档
     *
     * @param searchBean 搜索文档
     */
    void save(SearchBean searchBean);

    /**
     * 解析并保存 TXT 文件中的搜索结果
     * <p>
     * 功能：
     * <ul>
     *   <li>校验文件格式（仅支持 .txt）</li>
     *   <li>读取文件内容（UTF-8 编码）</li>
     *   <li>解析为 SearchBean 列表</li>
     *   <li>批量保存到 Elasticsearch</li>
     * </ul>
     *
     * @param file TXT 文件
     * @return 解析成功的记录数
     * @throws Exception 文件读取或解析失败时抛出异常
     */
    int parseAndSaveTxtFile(MultipartFile file) throws Exception;

    boolean exists(long id);

    /**
     * 根据 chatId 获取当前群组/频道的最新审核状态
     *
     * @param chatId 群组/频道 chatId
     * @return 最新一条记录的审核状态；不存在则返回 null
     */
    AuditStatusEnum getLatestAuditStatusByChatId(Long chatId);

    /**
     * ES分页查询，按收集时间倒序
     */
    PageResult<SearchBean> pageSearch(int pageNum, int pageSize, String keyword, SourceTypeEnum type);

    /**
     * 批量审核：通过或拒绝
     */
    void batchAudit(List<String> ids, AuditStatusEnum status, String remark);

    /**
     * 按群组/频道chatId批量审核：同步更新关联数据
     */
    void batchAuditByChatIds(java.util.List<Long> chatIds, AuditStatusEnum status, String remark);
}
