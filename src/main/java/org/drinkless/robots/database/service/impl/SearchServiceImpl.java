package org.drinkless.robots.database.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.entity.Collect;
import org.drinkless.robots.database.repository.SearchRepository;
import org.drinkless.robots.database.service.CollectService;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.helper.TxtFileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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

    private final CollectService collectService;
    private final SearchRepository searchRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(List<SearchBean> searchBeans) {
        if (CollUtil.isEmpty(searchBeans)) {
            return;
        }
        try {
            this.searchRepository.saveAll(searchBeans);

            List<Collect> collects = BeanUtil.copyToList(searchBeans, Collect.class);
            this.collectService.saveBatch(collects);
            log.info("[ES保存] 批量保存 {} 条搜索文档成功", searchBeans.size());
        } catch (Exception e) {
            log.error("[ES保存] 批量保存失败，数量: {}", searchBeans.size(), e);
        }
    }

    @Override
    public void save(SearchBean searchBean) {
        if (Objects.isNull(searchBean)) {
            return;
        }
        try {
            this.searchRepository.save(searchBean);
            Collect collect = BeanUtil.copyProperties(searchBean, Collect.class);
            this.collectService.save(collect);
            log.debug("[ES保存] 保存文档成功 id={}", searchBean.getId());
        } catch (Exception e) {
            log.error("[ES保存] 保存文档失败 id={}", searchBean.getId(), e);
        }
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
    public long count(long id) {
        Optional<SearchBean> optional = this.searchRepository.findById(String.valueOf(id));
        return optional.stream().count();
    }
}
