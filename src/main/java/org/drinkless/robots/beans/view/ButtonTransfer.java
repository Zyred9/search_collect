package org.drinkless.robots.beans.view;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Getter
@Setter
public class ButtonTransfer {

   // {"keyboard":[[{"text":"24小时客服","url":"https://t.me/devrobots"},{"text":"24小时客服","url":"https://t.me/devrobots"}],[{"text":"24小时客服","url":"https://t.me/devrobots"}]]}
    private String text;
    private String url;

}
