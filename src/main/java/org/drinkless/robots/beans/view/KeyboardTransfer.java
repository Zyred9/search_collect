package org.drinkless.robots.beans.view;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Setter
@Getter
public class KeyboardTransfer {

    // {"keyboard":[[{"text":"24小时客服","url":"https://t.me/devrobots"},{"text":"24小时客服","url":"https://t.me/devrobots"}],[{"text":"24小时客服","url":"https://t.me/devrobots"}]]}
    private List<List<ButtonTransfer>> keyboard;
}
