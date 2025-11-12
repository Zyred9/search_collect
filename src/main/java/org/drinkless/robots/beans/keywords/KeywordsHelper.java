package org.drinkless.robots.beans.keywords;

import cn.hutool.core.util.StrUtil;
import toolgood.words.StringSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class KeywordsHelper {

    private static final Map<Long, Map<String, String>> MAP_SEARCH = new HashMap<>(128);
    private static final StringSearch SEARCH = new StringSearch();


    public static void add (List<String> kws) {
        SEARCH.SetKeywords(kws);
    }
    public static String illegal(String k) {
        if (StrUtil.isNotBlank(k)) {
            return SEARCH.FindFirst(k);
        }
        return null;
    }

    public static void addKeywords (Long chatId, String key, String content) {
        if (MAP_SEARCH.containsKey(chatId)) {
            Map<String, String> keywords = MAP_SEARCH.get(chatId);
            List<String> keys = new ArrayList<>(keywords.keySet());
            keys.add(key);
            KeywordsHelper.add(keys);
            keywords.put(key, content);
        } else {
            Map<String, String> map = new HashMap<>(128);
            map.put(key, content);
            MAP_SEARCH.put(chatId, map);
            KeywordsHelper.add(List.of(key));
        }
    }

    public static String getContent (Long chatId, String key) {
        String illegal = KeywordsHelper.illegal(key);
        if (StrUtil.isNotBlank(illegal)) {
            return MAP_SEARCH.get(chatId).get(key);
        }
        return null;
    }

}
