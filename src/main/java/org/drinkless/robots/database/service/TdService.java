package org.drinkless.robots.database.service;


/**
 *
 *
 * @author zyred
 * @since 2025/11/11 15:55
 */
public interface TdService {

    boolean login(String phone, String pwd);

    boolean code(String phone, String code);

    boolean offline(String phone);

    String history(String link, String inviteLink, int count);
}
