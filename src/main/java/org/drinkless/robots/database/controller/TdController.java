package org.drinkless.robots.database.controller;


import org.drinkless.robots.beans.view.base.Result;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.database.service.TdService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/login")
    public Result<Void> login (@RequestParam("phone") String phone,
                               @RequestParam(value = "pwd", required = false) String pwd) {
        boolean login = this.tdService.login(phone, pwd);
        return login ? Result.success() : Result.error();
    }

    @GetMapping("/code")
    public Result<Void> code (@RequestParam("phone") String phone,
                              @RequestParam("code") String code) {
        boolean login = this.tdService.code(phone, code);
        return login ? Result.success() : Result.error();
    }

    /**
     * 拉取群组历史消息（支持公开群组和私密群组）
     * <pre>
     * 1. 公开群组: 传 link，chatId 可不传
     * 2. 私密群组: 传 chatId（负数），link 可不传
     * 3. 优先级: chatId > link
     * </pre>
     */
    @GetMapping("/history")
    public Result<String> history (@RequestParam(value = "link", required = false) String link,
                                   @RequestParam(value = "chatId", required = false) Long chatId,
                                   @RequestParam(value = "count", required = false, defaultValue = "3000") int count) {
        try {
            String message = this.tdService.history(link, chatId, count);
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

}
