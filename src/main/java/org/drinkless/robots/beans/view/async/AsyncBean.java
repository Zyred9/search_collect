package org.drinkless.robots.beans.view.async;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.drinkless.robots.database.entity.Included;

/**
 *
 *
 * @author zyred
 * @since 2025/11/13 15:45
 */
@Setter
@Getter
@Accessors(chain = true)
public class AsyncBean {

    /** 群组id，主要是更新获取资源数据量 **/
    private Included chat;



    public static AsyncBean buildChat(Included chat) {
        return new AsyncBean()
                .setChat(chat);
    }
}
