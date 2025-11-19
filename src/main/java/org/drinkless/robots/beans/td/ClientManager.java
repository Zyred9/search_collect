package org.drinkless.robots.beans.td;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.async.AsyncBean;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.config.BotProperties;
import org.drinkless.robots.config.SelfException;
import org.drinkless.robots.database.entity.Account;
import org.drinkless.robots.database.entity.AccountWatch;
import org.drinkless.robots.database.entity.Included;
import org.drinkless.robots.database.enums.AccountStatus;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.AccountWatchService;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.handlers.AsyncTaskHandler;
import org.drinkless.robots.helper.ContentFilter;
import org.drinkless.robots.helper.MessageConverter;
import org.drinkless.robots.helper.StrHelper;
import org.drinkless.robots.helper.ThreadHelper;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ClientManager {

    // ==================== TDLib 配置常量 ====================
    private static final int TD_API_ID = 26446149;
    private static final String TD_API_HASH = "923e9061ea87df0e3a79af95338f620d";
    private static final String TD_SYSTEM_LANGUAGE = "zh";
    private static final String TD_DEVICE_MODEL = "Desktop";
    private static final String TD_APP_VERSION = "1.0.1";
    
    // ==================== 历史消息补齐配置 ====================
    private static final int HISTORY_PAGE_SIZE = 200;
    private static final long QPS_SLEEP_MILLIS = 200;
    private static final int CHAT_HISTORY_TIMEOUT_SECONDS = 120;

    /* ============================== 依赖与状态 ============================== */

    @Resource private BotProperties properties;
    @Resource private AccountService accountService;
    @Resource private AccountWatchService accountWatchService;
    @Resource private SearchService searchService;

    // 每个小号的 TDLib Client 实例
    private final Map<String, Client> clientMap = new ConcurrentHashMap<>();
    // 小号是否已登录（ready）
    private final Map<String, Boolean> authStatusMap = new ConcurrentHashMap<>();
    // 开启实时日志打印的群/频道：手机号 -> 监听的 chatId 集合
    private final Map<String, Set<Long>> phoneToWatchedChatIds = new ConcurrentHashMap<>();
    // Supergroup 缓存：supergroupId -> Supergroup 对象（用于提取 username）
    private final Map<Long, TdApi.Supergroup> supergroupCache = new ConcurrentHashMap<>();

    /* ============================== 对外功能 API ============================== */

    /**
     * 创建并启动小号登录流程
     */
    public void startAccountLogin(String phone) {
        try {
            Client client = Client.create(
                    object -> onSubAccountUpdate(phone, object),
                    this::onUpdateException,
                    this::onDefaultException
            );
            this.clientMap.put(phone, client);
            this.authStatusMap.put(phone, false);
            log.info("[登录] {} 登录成功", phone);
        } catch (Exception e) {
            log.error("[登录] {} 登录失败, 原因：{}", phone, e.getMessage(), e);
            throw new SelfException("登录失败");
        }
    }

    /**
     * 输入验证码
     *
     * @param phone 小号手机号
     * @param code 验证码
     */
    public void inputCodeForAccount(String phone, String code) {
        Client client = clientMap.get(phone);
        if (client == null) {
            log.warn("[登录] 未找到小号 {} 的客户端实例", phone);
            return ;
        }

        client.send(new TdApi.CheckAuthenticationCode(code), result -> {
            if (result instanceof TdApi.Error error) {
                log.error("[登录] 小号 {} 验证码验证失败: {}", phone, error.message);
            } else {
                log.info("[登录] 小号 {} 验证成功", phone);
            }
        });
    }

    /* ============================== TDLib 回调处理 ============================== */

    private void onSubAccountUpdate(String phone, TdApi.Object object) {
        if (object instanceof TdApi.UpdateAuthorizationState updateAuthorizationState) {
            handleAuthorizationState(phone, updateAuthorizationState.authorizationState);
            return;
        }
        if (object instanceof TdApi.UpdateMessageContent updateMessageContent) {
            long chatId = updateMessageContent.chatId;
            if (shouldWatch(phone, chatId)) {
                String content = formatContent(updateMessageContent.newContent);
                log.info("[群历史-编辑] chatId={} msgId={} content={}", chatId, updateMessageContent.messageId, content);
            }
        }
    }

    private void handleAuthorizationState(String phone, TdApi.AuthorizationState authorizationState) {
        Client client = clientMap.get(phone);
        if (client == null) {
            log.error("[登录] 未找到小号 {} 的客户端实例", phone);
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateWaitTdlibParameters) {
            configureTdLibParameters(client, phone);
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            client.send(new TdApi.SetAuthenticationPhoneNumber(phone, null), result -> {
                if (result instanceof TdApi.Error error) {
                    log.error("[登录] 小号 {} 设置电话号码失败: {}", phone, error.message);
                    accountService.updateStatus(phone, AccountStatus.LOGIN_FAILED);
                }
            });
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateWaitCode) {
            this.accountService.updateStatus(phone, AccountStatus.NEED_CODE);
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateWaitPassword) {
            handleTwoFactorPassword(client, phone);
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            handleAuthorizedReady(client, phone);
            // 恢复监听与补齐历史
            recoverWatchesAndBackfill(client, phone);
            return;
        }
        if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
            authStatusMap.put(phone, false);
            clientMap.remove(phone);
            accountService.updateStatus(phone, AccountStatus.OFFLINE);
        }
    }

    /* ============================== 授权态子流程 ============================== */

    private void configureTdLibParameters(Client client, String phone) {
        String pn = StrUtil.removeAll(phone, "+");
        TdApi.SetTdlibParameters parameters = new TdApi.SetTdlibParameters();
        parameters.databaseDirectory = pn;
        parameters.useMessageDatabase = true;
        parameters.useSecretChats = true;
        parameters.apiId = TD_API_ID;
        parameters.apiHash = TD_API_HASH;
        parameters.systemLanguageCode = TD_SYSTEM_LANGUAGE;
        parameters.deviceModel = TD_DEVICE_MODEL;
        parameters.applicationVersion = TD_APP_VERSION;
        client.send(parameters, result -> {
            if (result instanceof TdApi.Error error) {
                log.error("[登录] 小号 {} 设置TDLib参数失败: {}", phone, error.message);
                accountService.updateStatus(phone, AccountStatus.LOGIN_FAILED);
            }
        });
        // 降低日志噪音
        client.send(new TdApi.SetLogVerbosityLevel(0), r -> {});
        client.send(new TdApi.SetLogTagVerbosityLevel("network", 0), r -> {});
        client.send(new TdApi.SetLogTagVerbosityLevel("store", 0), r -> {});
        client.send(new TdApi.SetLogTagVerbosityLevel("session", 0), r -> {});
        client.send(new TdApi.SetLogTagVerbosityLevel("client", 0), r -> {});
        client.send(new TdApi.SetLogTagVerbosityLevel("main", 0), r -> {});
    }

    private void handleTwoFactorPassword(Client client, String phone) {
        Account account = accountService.selectAccount(phone, "");
        if (StrUtil.isNotBlank(account.getPassword())) {
            client.send(new TdApi.CheckAuthenticationPassword(account.getPassword()), result -> {
                if (result instanceof TdApi.Error error) {
                    log.error("[登录] 小号 {} 密码验证失败: {}", phone, error.message);
                    accountService.updateStatus(phone, AccountStatus.LOGIN_FAILED);
                }
            });
        }
    }

    private void recoverWatchesAndBackfill(Client client, String phone) {
        try {
            for (AccountWatch w : accountWatchService.findEnabledByPhone(phone)) {
                startWatchChat(phone, w.getChatId());
                // 从断点向"更晚"补齐：使用 offset=-1 模式
                backfillFromLastMessageId(client, phone, w);
            }
        } catch (Exception e) {
            log.error("[恢复] 恢复监听失败 phone={} err={}", phone, e.getMessage());
        }
    }

    private void backfillFromLastMessageId(Client client, String phone, AccountWatch watch) {
        long chatId = watch.getChatId();
        long lastId = Objects.nonNull(watch.getLastMessageId()) ? watch.getLastMessageId() : 0L;
        
        if (lastId <= 0) {
            log.info("[恢复] 小号 {} 群组 {} 无断点记录，跳过历史补齐", phone, chatId);
            return;
        }
        
        log.info("[恢复] 小号 {} 开始补齐群组 {} 从消息ID {} 之后的历史", phone, chatId, lastId);
        
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 预先获取 Chat 信息（只获取一次）
                CompletableFuture<TdApi.Object> chatFuture = new CompletableFuture<>();
                client.send(new TdApi.GetChat(chatId), chatFuture::complete);
                TdApi.Chat chat = chatFuture.get(CHAT_HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS) instanceof TdApi.Chat c 
                    ? c : fallbackChat(chatId);
                
                // 2. 如果是超级群组，预先获取 Supergroup 信息（只获取一次）
                TdApi.Supergroup supergroup = null;
                if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
                    CompletableFuture<TdApi.Supergroup> sgFuture = new CompletableFuture<>();
                    fetchAndCacheSupergroup(client, supergroupType.supergroupId, sgFuture::complete);
                    supergroup = sgFuture.get(CHAT_HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                
                // 3. 分页获取历史消息
                boolean hasMore = true;
                long fromId = lastId;
                int totalFetched = 0;
                long maxMessageId = lastId;
                TdApi.Supergroup finalSupergroup = supergroup;
                
                while (hasMore) {
                    CompletableFuture<TdApi.Object> historyFuture = new CompletableFuture<>();
                    client.send(new TdApi.GetChatHistory(chatId, fromId, -1, HISTORY_PAGE_SIZE, false), historyFuture::complete);
                    
                    TdApi.Object obj = historyFuture.get(CHAT_HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (obj instanceof TdApi.Messages msgs) {
                        TdApi.Message[] arr = msgs.messages;
                        if (arr == null || arr.length == 0) {
                            log.info("[恢复] 小号 {} 群组 {} 历史补齐完成，共获取 {} 条新消息", phone, chatId, totalFetched);
                            break;
                        }
                        
                        // 收集本页需要保存的消息
                        java.util.List<SearchBean> batchList = new java.util.ArrayList<>();
                        for (TdApi.Message m : arr) {
                            if (m.id > lastId) {
                                SearchBean bean = MessageConverter.convertToSearchBean(m, chat, finalSupergroup);
                                if (Objects.nonNull(bean)) {
                                    batchList.add(bean);
                                    maxMessageId = Math.max(maxMessageId, m.id);
                                }
                            }
                        }
                        
                        // 批量保存消息（一次性保存整页）
                        if (!batchList.isEmpty()) {
                            searchService.batchSave(batchList);
                            totalFetched += batchList.size();
                        }
                        
                        // 批量更新断点（每页更新一次）
                        if (maxMessageId > lastId) {
                            accountWatchService.updateLastMessage(phone, chatId, maxMessageId);
                        }
                        
                        log.info("[恢复] 小号 {} 群组 {} 本页获取 {} 条新消息，累计 {}", phone, chatId, batchList.size(), totalFetched);
                        
                        fromId = arr[arr.length - 1].id;
                        Thread.sleep(QPS_SLEEP_MILLIS);
                    } else {
                        hasMore = false;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[恢复] 小号 {} 群组 {} 补齐被中断", phone, chatId);
            } catch (Exception e) {
                log.error("[恢复] 小号 {} 群组 {} 补齐异常", phone, chatId, e);
            }
        });
    }

    private void handleAuthorizedReady(Client client, String phone) {
        authStatusMap.put(phone, true);
        client.send(new TdApi.GetMe(), result -> {
            if (result instanceof TdApi.User user) {
                accountService.updateTelegramInfo(
                        phone,
                        user.id,
                        user.usernames != null && user.usernames.activeUsernames.length > 0 ? user.usernames.activeUsernames[0] : null,
                        user.firstName + (StrUtil.isNotBlank(user.lastName) ? " " + user.lastName : ""),
                        AccountStatus.LOGGED_IN
                );
                log.info("[登录] 小号 {} 登录成功", phone);
            } else {
                log.error("[登录] 小号 {} 获取用户信息失败", phone);
                accountService.updateStatus(phone, AccountStatus.LOGGED_IN);
            }
        });
    }

    /* ============================== 入群/历史回溯/监听 ============================== */


    private TdApi.Chat fallbackChat(long chatId) {
        TdApi.Chat c = new TdApi.Chat();
        c.id = chatId;
        c.title = "";
        c.type = new TdApi.ChatTypeSupergroup(0, false); // 占位，避免空指针
        return c;
    }


    /* ============================== 日志格式化与监听控制 ============================== */


    private void fetchAndCacheSupergroup(Client client, long supergroupId, java.util.function.Consumer<TdApi.Supergroup> callback) {
        // 先从缓存获取
        TdApi.Supergroup cached = supergroupCache.get(supergroupId);
        if (Objects.nonNull(cached)) {
            callback.accept(cached);
            return;
        }
        
        // 缓存未命中，调用 API 获取
        client.send(new TdApi.GetSupergroup(supergroupId), result -> {
            if (result instanceof TdApi.Supergroup supergroup) {
                // 缓存结果
                supergroupCache.put(supergroupId, supergroup);
                callback.accept(supergroup);
            } else {
                // 获取失败，传 null
                callback.accept(null);
            }
        });
    }

    /**
     * 格式化消息内容为可读字符串
     *
     * @param content TDLib消息内容对象
     * @return 格式化后的字符串
     */
    private String formatContent(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText mt) {
            return mt.text != null ? mt.text.text : "";
        }
        if (content instanceof TdApi.MessagePhoto mp) {
            return formatMediaContent("Photo", mp.caption);
        }
        if (content instanceof TdApi.MessageVideo mv) {
            return formatMediaContent("Video", mv.caption);
        }
        if (content instanceof TdApi.MessageDocument md) {
            return formatMediaContent("Document", md.caption);
        }
        if (content instanceof TdApi.MessageAudio ma) {
            return formatMediaContent("Audio", ma.caption);
        }
        if (content instanceof TdApi.MessageVoiceNote mvn) {
            return formatMediaContent("Voice", mvn.caption);
        }
        if (content instanceof TdApi.MessageAnimation man) {
            return formatMediaContent("Animation", man.caption);
        }
        if (content instanceof TdApi.MessageSticker ignored) {
            return "[Sticker]";
        }
        if (content instanceof TdApi.MessageContact ignored) {
            return "[Contact]";
        }
        if (content instanceof TdApi.MessageLocation ignored) {
            return "[Location]";
        }
        if (content instanceof TdApi.MessagePoll ignored) {
            return "[Poll]";
        }
        return "[" + content.getClass().getSimpleName() + "]";
    }

    /**
     * 格式化媒体消息内容（图片、视频、文档等）
     *
     * @param mediaType 媒体类型（Photo/Video/Document等）
     * @param caption 媒体说明文本
     * @return 格式化后的字符串
     */
    private String formatMediaContent(String mediaType, TdApi.FormattedText caption) {
        String captionText = (caption != null && caption.text != null) ? caption.text : "";
        return StrUtil.format("[{}] caption={}", mediaType, captionText);
    }

    private boolean shouldWatch(String phone, long chatId) {
        Set<Long> set = phoneToWatchedChatIds.get(phone);
        return set != null && set.contains(chatId);
    }

    private void startWatchChat(String phone, long chatId) {
        phoneToWatchedChatIds
                .computeIfAbsent(phone, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(chatId);
    }

    @SuppressWarnings("unused")
    private void stopWatchChat(String phone, long chatId) {
        Set<Long> set = phoneToWatchedChatIds.get(phone);
        if (set != null) {
            set.remove(chatId);
        }
    }

    /* ============================== 异常处理 ============================== */

    private void onUpdateException(Throwable e) {
        log.error("[TDLib] 更新异常: {}", e.getMessage(), e);
    }

    private void onDefaultException(Throwable e) {
        log.error("[TDLib] 默认异常: {}", e.getMessage(), e);
    }

    /* ============================== 工具方法 ============================== */

    /**
     * 将TDLib消息的Unix时间戳转换为LocalDateTime
     *
     * @param unixTimestamp Unix时间戳(秒)
     * @return LocalDateTime对象
     */
    private LocalDateTime convertUnixTimestampToLocalDateTime(int unixTimestamp) {
        return java.time.Instant.ofEpochSecond(unixTimestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime();
    }

    /* ============================== 拉取历史消息 API ============================== */

    public void fetchHistoryFromLink(String telegramLink, int count) {
        // 1. 解析链接获取 username
        String username = parseTelegramLink(telegramLink);
        if (StrUtil.isBlank(username)) {
            log.error("[拉取历史] 无效的 Telegram 链接: {}", telegramLink);
            return;
        }

        // 2. 选择一个可用的 Client
        Client client = selectAvailableClient();
        if (Objects.isNull(client)) {
            log.error("[拉取历史] 没有可用的已登录客户端");
            return;
        }

        // 3. 根据 username 搜索聊天
        log.info("[拉取历史] 开始查找聊天: {}", username);
        client.send(new TdApi.SearchPublicChat(username), result -> {
            if (result instanceof TdApi.Chat chat) {
                String phone = getPhoneByClient(client);
                fetchHistoryMessages(client, phone, chat, telegramLink, count);
            } else if (result instanceof TdApi.Error error) {
                log.error("[拉取历史] 查找聊天失败: {}", error.message);
            }
        });
    }

    /**
     * 分页拉取历史消息并保存到 Elasticsearch
     *
     * @param client TDLib 客户端
     * @param phone  客户端对应的手机号
     * @param chat   聊天对象
     * @param telegramLink Telegram链接
     * @param count  条数
     */
    private void fetchHistoryMessages(Client client, String phone, TdApi.Chat chat, String telegramLink, int count) {
        // 如果是超级群组,提前获取并缓存 Supergroup 信息
        if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
            fetchAndCacheSupergroup(client, supergroupType.supergroupId, a -> startFetchTask(client, phone, chat, telegramLink, count));
        } else {
            startFetchTask(client, phone, chat, telegramLink, count);
        }
    }

    /**
     * 启动拉取任务
     */
    private void startFetchTask(Client client, String phone, TdApi.Chat chat, String telegramLink, int count) {
        long chatId = chat.id;
        
        // 查询是否已有拉取记录
        AccountWatch watch = accountWatchService.getOne(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<AccountWatch>lambdaQuery()
                .eq(AccountWatch::getPhone, phone)
                .eq(AccountWatch::getChatId, chatId)
        );
        
        // 获取已处理的最大消息ID（用于去重）
        long lastProcessedMessageId;
        if (Objects.nonNull(watch) && Objects.nonNull(watch.getLastMessageId())) {
            lastProcessedMessageId = watch.getLastMessageId();
            log.info("[拉取历史] 检测到断点记录，已处理最大消息ID: {}", lastProcessedMessageId);
        } else {
            lastProcessedMessageId = 0;
        }

        CompletableFuture.runAsync(() -> fetchHistoryMessagesAsync(client, phone, chat, telegramLink, lastProcessedMessageId, count), ThreadHelper::execute);
    }


    private void fetchHistoryMessagesAsync(Client client, String phone, TdApi.Chat chat, String telegramLink, long lastProcessedMessageId, int count) {
        long chatId = chat.id;
        
        // ==================== 1. 开始拉取历史消息 ====================
        int totalFetched = 0;
        long fromMessageId = 0; // 总是从最新消息开始
        LocalDateTime groupCreationTime = null; // 群组创建时间(取最早消息的时间)
        java.util.List<SearchBean> searchBeans = new java.util.ArrayList<>();

        while (totalFetched < count) {
            CompletableFuture<TdApi.Object> future = new CompletableFuture<>();
            // 拉取一页历史消息（fromMessageId=0表示从最新开始，后续使用上一页最后一条消息ID）
            // offset=0表示向更早方向获取
            client.send(new TdApi.GetChatHistory(chatId, fromMessageId, 0, HISTORY_PAGE_SIZE, false), future::complete);
            try {
                TdApi.Object obj = future.get(CHAT_HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                if (obj instanceof TdApi.Messages msgs) {
                    TdApi.Message[] messages = msgs.messages;
                    
                    if (messages == null || messages.length == 0) {
                        log.info("[拉取历史] {} ({}) 历史消息已全部拉取，共 {} 条", chat.title, chatId, totalFetched);
                        break;
                    }

                    // 转换并收集消息
                    int filteredCount = 0; // 本页被过滤的消息数
                    for (TdApi.Message message : messages) {
                        // 记录最早消息的时间作为群组创建时间
                        LocalDateTime messageTime = convertUnixTimestampToLocalDateTime(message.date);
                        if (Objects.isNull(groupCreationTime) || messageTime.isBefore(groupCreationTime)) {
                            groupCreationTime = messageTime;
                        }
                        
                        // 如果消息ID小于等于已处理的最大ID，说明已经处理过了，停止拉取
                        if (lastProcessedMessageId > 0 && message.id <= lastProcessedMessageId) {
                            log.info("[拉取历史] {} ({}) 遇到已处理消息 ID {}，停止拉取", chat.title, chatId, message.id);
                            totalFetched = count; // 强制退出外层循环
                            break;
                        }
                        
                        if (totalFetched >= count) {
                            break;
                        }

                        // 如果是超级群组,获取 Supergroup 信息以提取 username
                        TdApi.Supergroup supergroup = null;
                        if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
                            supergroup = supergroupCache.get(supergroupType.supergroupId);
                        }

                        SearchBean searchBean = MessageConverter.convertToSearchBean(message, chat, supergroup);
                        
                        if (Objects.nonNull(searchBean)) {
                            // 内容过滤：检测诈骗机器人、敏感词、非中文内容
                            if (ContentFilter.shouldFilter(searchBean)) {
                                filteredCount++;
                                // 更新最后处理的消息 ID（即使被过滤也要记录，避免重复检测）
                                accountWatchService.updateLastMessage(phone, chatId, message.id);
                                continue; // 跳过被过滤的消息
                            }
                            
                            searchBeans.add(searchBean);
                            totalFetched++;
                        }

                        // 更新最后处理的消息 ID
                        accountWatchService.updateLastMessage(phone, chatId, message.id);
                    }

                    // 批量保存到 ES（每 50 条保存一次，减少 IO）
                    if (searchBeans.size() >= 50) {
                        searchService.batchSave(searchBeans);
                        searchBeans.clear();
                    }
                    
                    // 记录过滤统计
                    if (filteredCount > 0) {
                        log.info("[内容过滤] {} ({}) 本页过滤 {} 条消息", chat.title, chatId, filteredCount);
                    }
                    
                    // 如果遇到已处理的消息，退出循环
                    if (totalFetched >= count) {
                        break;
                    }
                    
                    // 更新下一页起始位置（使用本页最后一条消息ID）
                    fromMessageId = messages[messages.length - 1].id;

                    // QPS 控制：每页延迟 200ms
                    Thread.sleep(QPS_SLEEP_MILLIS);

                } else if (obj instanceof TdApi.Error error) {
                    log.error("[拉取历史] {} ({}) 拉取失败: {}", chat.title, chatId, error.message);
                    break;
                }

            } catch (Exception e) {
                log.error("[拉取历史] {} ({}) 拉取异常，已获取 {} 条", chat.title, chatId, totalFetched, e);
                break;
            }
        }

        // 保存剩余的消息
        if (!searchBeans.isEmpty()) {
            searchService.batchSave(searchBeans);
        }
        log.info("[拉取历史] {} ({}) 拉取任务完成，共保存 {} 条有效消息", chat.title, chatId, totalFetched);

        // ==================== 2. 保存群组/频道本身的记录 ====================
        // 如果没有获取到创建时间，使用当前时间
        LocalDateTime finalCreationTime = Objects.nonNull(groupCreationTime) ? groupCreationTime : LocalDateTime.now();
        Integer number = this.saveChannelOrGroupRecord(chat, telegramLink, finalCreationTime);

        AsyncBean asyncBean = AsyncBean.buildChat(
                new Included()
                        .setId(chatId)
                        .setSourceCount(totalFetched)
                        .setNumber(number)
                        .setIndexCreateTime(groupCreationTime)
        );
        AsyncTaskHandler.async(asyncBean);
    }

    /**
     * 选择一个可用的已登录 Client
     */
    private Client selectAvailableClient() {
        for (Map.Entry<String, Boolean> entry : authStatusMap.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                Client client = clientMap.get(entry.getKey());
                if (client != null) {
                    return client;
                }
            }
        }
        return null;
    }

    /**
     * 根据 Client 对象反查手机号
     */
    private String getPhoneByClient(Client targetClient) {
        for (Map.Entry<String, Client> entry : clientMap.entrySet()) {
            if (entry.getValue() == targetClient) {
                return entry.getKey();
            }
        }
        return "";
    }

    private String parseTelegramLink(String link) {
        if (StrUtil.isBlank(link)) {
            return "";
        }

        link = link.trim();

        // 处理 @username 格式
        if (link.startsWith("@")) {
            return link.substring(1);
        }

        // 处理 t.me/username 或 https://t.me/username
        if (link.contains("t.me/")) {
            String[] parts = link.split("t.me/");
            if (parts.length > 1) {
                return parts[1].split("[/?#]")[0]; // 去掉可能的路径参数
            }
        }
        return link;
    }

    private Integer saveChannelOrGroupRecord(TdApi.Chat chat, String telegramLink, LocalDateTime creationTime) {
        try {
            // 1. 判断类型：群组还是频道
            SourceTypeEnum type;
            TdApi.Supergroup supergroup = null;
            
            if (chat.type instanceof TdApi.ChatTypeSupergroup supergroupType) {
                supergroup = supergroupCache.get(supergroupType.supergroupId);
                if (Objects.nonNull(supergroup) && supergroup.isChannel) {
                    type = SourceTypeEnum.CHANNEL; // 频道
                } else {
                    type = SourceTypeEnum.GROUP; // 群组
                }
            } else {
                type = SourceTypeEnum.GROUP;
            }

            String subscribers = null; Integer number = null;
            if (Objects.nonNull(supergroup) && supergroup.memberCount > 0) {
                subscribers = StrHelper.formatMemberCount(supergroup.memberCount);
                number = supergroup.memberCount;
            }

            boolean isRestricted = false;
            if (Objects.nonNull(supergroup)) {
                if (supergroup.hasSensitiveContent) {
                    isRestricted = true;
                }
                if (StrUtil.isNotBlank(supergroup.restrictionReason)) {
                    isRestricted = true;
                }
            }
            SearchBean bean = new SearchBean()
                .setId(UUID.fastUUID().toString(true))
                .setType(type)
                .setSourceName(chat.title)
                .setSourceUrl(telegramLink)
                .setSubscribers(subscribers)
                .setCollectTime(LocalDateTime.now())
                .setMarked(isRestricted);

            // 如果获取到创建时间,可以打印日志或存储到其他字段
            if (Objects.nonNull(creationTime)) {
                log.info("[群组创建时间] {} 的创建时间约为: {}", chat.title, creationTime);
                // TODO: 如果需要存储创建时间,需要在SearchBean中添加对应字段
                // bean.setCreationTime(creationTime);
            }

            searchService.save(bean);
            return number;
        } catch (Exception e) {
            log.error("[群组记录] 保存失败: {}", chat.title, e);
            return null;
        }
    }
}
