package org.drinkless.robots.database.repository;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch 搜索文档仓库
 * <p>
 * 提供对 SearchBean 的 CRUD 操作和全文检索能力
 * </p>
 *
 * @author zyred
 * @since 1.0
 */
@Repository
public interface SearchRepository extends ElasticsearchRepository<SearchBean, String> {

    Page<SearchBean> findByType(SourceTypeEnum type, Pageable pageable);

    Page<SearchBean> findByTypeAndSourceNameContainingIgnoreCase(SourceTypeEnum type, String keyword, Pageable pageable);

    Page<SearchBean> findByTypeIn(java.util.List<SourceTypeEnum> types, Pageable pageable);

    Page<SearchBean> findByTypeInAndSourceNameContainingIgnoreCase(java.util.List<SourceTypeEnum> types, String keyword, Pageable pageable);

}
