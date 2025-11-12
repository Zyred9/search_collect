package org.drinkless.robots.database.service;

import org.drinkless.robots.beans.view.search.SearchBean;
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
}
