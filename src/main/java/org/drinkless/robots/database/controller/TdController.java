package org.drinkless.robots.database.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.drinkless.robots.beans.view.base.Result;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.database.service.TdService;
import org.drinkless.robots.beans.view.base.PageResult;
import org.drinkless.robots.beans.view.search.AuditRequest;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;

import javax.annotation.Resource;

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
 * @author zyred
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

    /**
     * 拉取公开群组/频道历史消息
     * 仅支持公开群组（通过 @username 或 t.me/username 访问）
     * <pre>
     * 参数说明:
     * - link: 公开群组链接 (必填，支持 @username 或 t.me/username)
     * - count: 拉取消息数量，默认 3000
     * 
     * 使用场景:
     * 仅支持公开群组和频道的历史消息拉取
     * </pre>
     */
    /**
     * 账号下线接口
     * 关闭 TDLib 客户端连接，状态更新为 OFFLINE
     */
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
     * <pre>
     * 参数说明:
     * - link: 公开群组链接 (支持 @username 或 t.me/username)
     * - inviteLink: 私密群组邀请链接 (格式: t.me/+xxxxx 或 t.me/joinchat/xxxxx)
     * - count: 拉取消息数量，默认 3000
     * 
     * 使用场景:
     * 1. 公开群组：只传 link
     * 2. 私密群组：传 inviteLink（优先级更高）
     * </pre>
     */
    @GetMapping("/history")
    public Result<String> history (@RequestParam(value = "link", required = false) String link,
                                   @RequestParam(value = "inviteLink", required = false) String inviteLink,
                                   @RequestParam(value = "count", required = false, defaultValue = "3000") int count) {
        if (StrUtil.isBlank(link) && StrUtil.isBlank(inviteLink)) {
            return Result.error("请提供 link 或 inviteLink 中的至少一个参数");
        }
        
        try {
            String message = this.tdService.history(link, inviteLink, count);
            return Result.success(message != null ? message : "历史消息拉取任务已启动");
        } catch (Exception e) {
            return Result.error("拉取失败: " + e.getMessage());
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

    /**
     * ES分页查询接口
     *
     * 功能：分页查询 Elasticsearch 索引 `search_index` 的文档，按 `collectTime` 倒序
     * 入参：
     * - pageNum: 当前页码(默认1)
     * - pageSize: 每页大小(默认10，最大200)
     * - keyword: 关键词(可选，匹配 sourceName)
     * - type: 内容类型(可选，枚举值 SourceTypeEnum)
     * 返回：分页数据列表、总记录数、当前页码、每页大小
     */
    @GetMapping("/search/page")
    public Result<PageResult<org.drinkless.robots.beans.view.search.SearchBean>> pageSearch(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) SourceTypeEnum type
    ) {
        PageResult<org.drinkless.robots.beans.view.search.SearchBean> result = this.searchService.pageSearch(pageNum, pageSize, keyword, type);
        return Result.success(result);
    }

    /**
     * 批量审核接口
     *
     * 功能：对 `search_index` 文档进行批量审核，通过或拒绝
     * 入参：
     * - Authorization: 请求头，必须存在
     * - body: { operation: APPROVED/REJECTED, ids: ["id1","id2"...], remark: "可选备注" }
     * 返回：操作结果
     */
    @PostMapping("/search/audit")
    public Result<Void> audit(
            @RequestBody AuditRequest req
    ) {
        if (req == null || req.getOperation() == null || cn.hutool.core.collection.CollUtil.isEmpty(req.getIds())) {
            return Result.error(400, "参数不完整: 需要 operation 与 ids");
        }
        AuditStatusEnum op = req.getOperation();
        this.searchService.batchAudit(req.getIds(), op, req.getRemark());
        return Result.success();
    }

}
