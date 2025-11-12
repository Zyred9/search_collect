package org.drinkless.robots.beans.cache;

import org.drinkless.robots.config.Constants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class CommonCache {

    private static final Set<Long> ADMIN_SET = new HashSet<>(128);

    public static void addAdmin (List<Long> chatIds) {
        ADMIN_SET.addAll(chatIds);
    }
    public static boolean isAdmin (Long chatId) {
        return ADMIN_SET.contains(chatId);
    }
    public static void delAdmin (List<Long> chatIds) {
        chatIds.forEach(ADMIN_SET::remove);
    }
    public static Set<Long> getAdminSet() {
        return ADMIN_SET;
    }

    private static final Set<Long> VIP_SET = new HashSet<>(128);

    public static void addVip (List<Long> chatIds) {
        VIP_SET.addAll(chatIds);
    }
    public static boolean isVip (Long chatId) {
        return VIP_SET.contains(chatId);
    }
    public static void delVip (List<Long> chatIds) {
        chatIds.forEach(VIP_SET::remove);
    }

    private static final Set<String> ADDR_SET = new HashSet<>(128);

    public static void addr (List<String> chatIds) {
        ADDR_SET.addAll(chatIds);
    }
    public static boolean isAddr (String chatId) {
        return ADDR_SET.contains(chatId);
    }
    public static void delAddr (List<String> chatIds) {
        chatIds.forEach(ADDR_SET::remove);
    }

    private static final Set<Long> CHEATER_SET = new HashSet<>(128);

    public static void addBatchCheater (List<Long> chatIds) {
        CHEATER_SET.addAll(chatIds);
    }
    public static void addCheater (List<Long> chatIds) {
        CHEATER_SET.addAll(chatIds);
    }
    public static boolean isCheater (Long chatId) {
        return CHEATER_SET.contains(chatId);
    }
    public static void delCheater (List<Long> chatIds) {
        chatIds.forEach(CHEATER_SET::remove);
    }

    private static final List<String> BUTTON_SET = new ArrayList<>(128);

    public static void button (List<String> buttons) {
        BUTTON_SET.clear();
        BUTTON_SET.addAll(buttons);
    }

    public static List<String> getButtonSet() {
        return BUTTON_SET;
    }


    private static final Map<String, Integer> MOCK_REDIS = new ConcurrentHashMap<>(128);

    public static void setVal (String pre, Long userId) {
        String key = pre + userId;
        MOCK_REDIS.put(key, 1);
    }

    public static boolean hasVal (String pre, Long userId) {
        String key = pre + userId;
        return MOCK_REDIS.containsKey(key);
    }

    public static void remove (String pre, Long userId) {
        String key = pre + userId;
        MOCK_REDIS.remove(key);
    }


    private static final Map<String, Long> USER_MAP = new ConcurrentHashMap<>(128);
    static {
        USER_MAP.put(Constants.VAL_1, 7874756166L);
        USER_MAP.put(Constants.VAL_2, 7653000728L);
        USER_MAP.put(Constants.VAL_3, 8147560039L);
    }
    public static Map<String, Long> getUser () {
        return USER_MAP;
    }
}
