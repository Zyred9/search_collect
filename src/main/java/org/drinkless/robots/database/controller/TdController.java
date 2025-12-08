package org.drinkless.robots.database.controller;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.drinkless.robots.beans.view.base.PageResult;
import org.drinkless.robots.beans.view.base.Result;
import org.drinkless.robots.beans.view.search.AuditRequest;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.database.service.TdService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * TDLib 相关接口控制器
 * <p>
 * 功能：
 * <ul>
 *   <li>小号登录与验证码输入</li>
 *   <li>拉取 Telegram 群组/频道历史消息</li>
 * </ul>
 * </p>
 *
 * @author admin
 * @since 2025/11/11 20:44
 */
@RestController
@RequestMapping("/td")
public class TdController {

    @Resource private TdService tdService;
    @Resource private SearchService searchService;
    @Resource private AccountService accountService;

    /**
     * 分页查询账号列表
     * 支持按手机号、状态、用户名搜索
     */
    @GetMapping("/accounts")
    public Result<Page<Account>> getAccounts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "status", required = false) Integer status) {
        
        Page<Account> result = accountService.getAccountPage(page, size, phone, username, status);
        return Result.success(result);
    }

    @GetMapping("/login")
    public Result<Void> login (@RequestParam("phone") String phone,
                               @RequestParam("email") String email,
                               @RequestParam(value = "pwd", required = false) String pwd) {
        boolean login = this.tdService.login(phone, email, pwd);
        return login ? Result.success() : Result.error();
    }

    @GetMapping("/code")
    public Result<Void> code (@RequestParam("phone") String phone,
                              @RequestParam("code") String code) {
        boolean login = this.tdService.code(phone, code);
        return login ? Result.success() : Result.error();
    }

    @GetMapping("/offline")
    public Result<Void> offline(@RequestParam("phone") String phone) {
        try {
            boolean success = this.tdService.offline(phone);
            return success ? Result.success() : Result.error("下线失败");
        } catch (Exception e) {
            return Result.error("下线失败: " + e.getMessage());
        }
    }

    /**
     * 拉取群组/频道历史消息
     * 支持公开群组和私密群组
     */
    @GetMapping("/history")
    public Result<String> history (@RequestParam(value = "link", required = false) String link,
                                   @RequestParam(value = "inviteLink", required = false) String inviteLink,
                                   @RequestParam(value = "count", required = false, defaultValue = "3000") int count,
                                   @RequestParam(value = "weight", required = false, defaultValue = "0") int weight  // 权重：0.后台直接拉 1.加入群组/频道 2.加入并给管理员
    ) {
        if (StrUtil.isBlank(link) && StrUtil.isBlank(inviteLink)) {
            return Result.error("请提供 link 或 inviteLink 中的至少一个参数");
        }
        
        try {
            String message = this.tdService.history(link, inviteLink, count, weight);
            return Result.success(message != null ? message : "历史消息拉取任务已启动");
        } catch (Exception e) {
            return Result.error("拉取失败: " + e.getMessage());
        }
    }

    /**
     * 拉取群组/频道最新消息（增量）
     * 提交为异步任务，立即返回任务提交结果
     */
    @GetMapping("/latest")
    public Result<String> latest(@RequestParam("chatId") long chatId,
                                 @RequestParam("url") String url) {
        if (chatId == 0L) {
            return Result.error("chatId不能为空");
        }
        try {
            String message = this.tdService.latest(chatId, url);
            return Result.success(message != null ? message : "最新消息拉取任务已启动");
        } catch (Exception e) {
            return Result.error("最新消息拉取失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<String> uploadTxtFile(@RequestParam("file") MultipartFile file) {
        try {
            int count = searchService.parseAndSaveTxtFile(file);
            return Result.success(String.format("上传成功！共解析 %d 条记录并已保存到 Elasticsearch", count));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/search/page")
    public Result<PageResult<SearchBean>> pageSearch(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) SourceTypeEnum type
    ) {
        PageResult<SearchBean> result = this.searchService.pageSearch(pageNum, pageSize, keyword, type);
        return Result.success(result);
    }

    @PostMapping("/search/audit")
    public Result<Void> audit(@RequestBody AuditRequest req) {
        if (req == null || req.getOperation() == null || CollUtil.isEmpty(req.getIds())) {
            return Result.error(400, "参数不完整: 需要 operation 与 ids");
        }
        AuditStatusEnum op = req.getOperation();
        try {
            List<Long> chatIds = new ArrayList<>();
            for (String s : req.getIds()) {
                try {
                    chatIds.add(Long.valueOf(s));
                } catch (NumberFormatException nfe) {
                    return Result.error(400, "ids必须为chatId数字");
                }
            }
            // 将提交的 ids 视为群组/频道 chatId，按主体同步更新其关联数据
            this.searchService.batchAuditByChatIds(chatIds, op, req.getRemark());
            // 同时更新明确提交的文档ID，保证主体与指定记录一致
            this.searchService.batchAudit(req.getIds(), op, req.getRemark());
            return Result.success();
        } catch (Exception e) {
            return Result.error("审核失败: " + e.getMessage());
        }
    }

}
