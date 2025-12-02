package org.drinkless.robots.database.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.beans.view.base.PageResult;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.drinkless.robots.database.repository.SearchRepository;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.helper.TxtFileParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 搜索服务实现类
 *
 * @author zyred
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(List<SearchBean> searchBeans) {
        if (CollUtil.isEmpty(searchBeans)) {
            return;
        }
        this.searchRepository.saveAll(searchBeans);
    }

    @Override
    public void save(SearchBean searchBean) {
        if (Objects.isNull(searchBean)) {
            return;
        }
        this.searchRepository.save(searchBean);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int parseAndSaveTxtFile(MultipartFile file) throws Exception {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 2. 校验文件格式
        String filename = file.getOriginalFilename();
        if (StrUtil.isBlank(filename) || !filename.toLowerCase().endsWith(".txt")) {
            throw new IllegalArgumentException("仅支持 .txt 格式文件");
        }

        log.info("[TXT解析] 开始解析文件: {}, 大小: {} bytes", filename, file.getSize());

        // 3. 读取文件内容（使用 UTF-8 编码）
        String content = IoUtil.read(file.getInputStream(), StandardCharsets.UTF_8);

        // 4. 解析 TXT 文件
        List<SearchBean> searchBeans = TxtFileParser.parse(content);

        if (CollUtil.isEmpty(searchBeans)) {
            log.warn("[TXT解析] 未能解析出任何有效记录，文件: {}", filename);
            throw new IllegalArgumentException("未能解析出任何有效记录，请检查文件格式");
        }

        log.info("[TXT解析] 解析完成，共 {} 条记录，准备保存到 ES", searchBeans.size());

        // 5. 批量保存到 Elasticsearch
        this.batchSave(searchBeans);

        log.info("[TXT解析] 文件处理完成: {}, 成功保存 {} 条记录", filename, searchBeans.size());

        return searchBeans.size();
    }

    @Override
    public boolean exists(long id) {
        return this.searchRepository.existsById(String.valueOf(id));
    }

    @Override
    public PageResult<SearchBean> pageSearch(int pageNum, int pageSize, String keyword, SourceTypeEnum type) {
        int pn = Math.max(pageNum, 1);
        int ps = Math.min(Math.max(pageSize, 1), 200);
        Pageable pageable = PageRequest.of(pn - 1, ps, Sort.by(Sort.Direction.DESC, "collectTime"));

        Page<SearchBean> page;
        if (StrUtil.isNotBlank(keyword) && Objects.nonNull(type)) {
            page = this.searchRepository.findByTypeAndSourceNameContainingIgnoreCase(type, keyword, pageable);
        } else if (StrUtil.isNotBlank(keyword)) {
            page = this.searchRepository.findBySourceNameContainingIgnoreCase(keyword, pageable);
        } else if (Objects.nonNull(type)) {
            page = this.searchRepository.findByType(type, pageable);
        } else {
            page = this.searchRepository.findAll(pageable);
        }

        return new PageResult<>(page.getContent(), page.getTotalElements(), pn, ps);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAudit(List<String> ids, AuditStatusEnum status, String remark) {
        if (CollUtil.isEmpty(ids) || Objects.isNull(status)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        List<UpdateQuery> updates = ids.stream().map(id -> {
            Document doc = Document.create();
            doc.put("auditStatus", status.name());
            if (StrUtil.isNotBlank(remark)) {
                doc.put("auditRemark", remark);
            }
            doc.put("auditedAt", now);
            return UpdateQuery.builder(id).withDocument(doc).build();
        }).collect(java.util.stream.Collectors.toList());

        this.elasticsearchOperations.bulkUpdate(updates, SearchBean.class);
    }
}
