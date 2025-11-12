package org.drinkless.robots.helper;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.drinkless.robots.beans.view.ButtonTransfer;
import org.drinkless.robots.beans.view.KeyboardTransfer;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class KeyboardHelper {

    public static InlineKeyboardMarkup buildPacketKeyboard(Boolean packet, int query) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(
                                buttonText(packet == null ? "是" :  packet ? "✅是" : "是", ""),
                                buttonText(packet == null ? "否" : !packet ? "✅否" : "否", "")
                        ),
                        row(buttonText("\uD83D\uDD1C 下一步", "privacy#packet#confirm#"+query), cancelButton())
                )).build();
    }

    public static InlineKeyboardMarkup buildPageOfKeyboard(Page<Object> pages) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();
        rows.add(row);

        int idx = 0, max = 3;
        List<Object> records = pages.getRecords();
        for (Object addr : records) {
            if (idx == max) {
                row = new InlineKeyboardRow();
                rows.add(row);
                idx = 0;
            }
            row.add(buttonText("", ""));
            idx ++;
        }

        if (pages.hasNext() || pages.hasPrevious()) {
            InlineKeyboardRow pageRow = new InlineKeyboardRow();
            if (pages.hasPrevious()) {
                pageRow.add(buttonText("上一页", ""));
            }
            if (pages.hasNext()) {
                pageRow.add(buttonText("下一页", ""));
            }
            rows.add(pageRow);
        }

        InlineKeyboardRow back = new InlineKeyboardRow();
        back.add(buttonText("返回上一级", ""));
        back.add(cancelButton());
        rows.add(back);

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }


    public static InlineKeyboardMarkup buildListOfKeyboard(List<Object> address) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow r = new InlineKeyboardRow();
        rows.add(r);
        int idx = 0;
        for (Object addr : address) {
            if (idx == 4) {
                r = new InlineKeyboardRow();
                rows.add(r);
                idx = 0;
            }
            r.add(buttonText("", ""));
            idx ++;
        }
        rows.add(new InlineKeyboardRow(cancelButton()));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static ReplyKeyboard buildStartReplyKeyboard() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(List.of(
                        new KeyboardRow(List.of(
                                KeyboardButton.builder().text("开始").build(),
                                KeyboardButton.builder().text("帮助").build(),
                                KeyboardButton.builder().text("我的").build())
                        )
                )).build();
    }

    public static InlineKeyboardMarkup keyboard (String json) {
        InlineKeyboardMarkup markup = null;
        if (StrUtil.isNotBlank(json)) {
            KeyboardTransfer transfer = JSONUtil.toBean(json, KeyboardTransfer.class);
            if (Objects.nonNull(transfer)) {
                List<InlineKeyboardRow> rows = new ArrayList<>(transfer.getKeyboard().size());
                for (List<ButtonTransfer> buttonTransfers : transfer.getKeyboard()) {
                    InlineKeyboardRow row = new InlineKeyboardRow();
                    for (ButtonTransfer buttonTransfer : buttonTransfers) {
                        row.add(InlineKeyboardButton.builder().text(buttonTransfer.getText()).url(buttonTransfer.getUrl()).build());
                    }
                    rows.add(row);
                }
                markup = InlineKeyboardMarkup.builder().keyboard(rows).build();
            }
        }
        return markup;
    }

    /*
        设置广告#-1002344866985#15:30
        &内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容内容
        &24小时客服#https://t.me/|24小时客服#https://t.me/$24小时客服#https://t.me/
    */
    public static String parseKeyboard(String keyboardCommand) {
        String keyboardJson = "";
        if (StrUtil.isNotBlank(keyboardCommand)) {
            // &24小时客服#https://t.me/|24小时客服#https://t.me/$24小时客服#https://t.me/
            List<String> keyboardLines = StrUtil.split(keyboardCommand, "$");
            List<InlineKeyboardRow> rows = new ArrayList<>(keyboardLines.size());
            for (String keyboardLine : keyboardLines) {
                // 24小时客服#https://t.me/|24小时客服#https://t.me/
                List<String> row = StrUtil.split(keyboardLine, "|");
                InlineKeyboardRow keyboardRow = new InlineKeyboardRow();
                for (String buttonLine : row) {
                    List<String> buttons = StrUtil.split(buttonLine, "#");
                    keyboardRow.add(InlineKeyboardButton.builder().text(buttons.get(0)).url(buttons.get(1)).build());
                }
                rows.add(keyboardRow);
            }
            InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                    .keyboard(rows)
                    .build();
            keyboardJson = JSONUtil.toJsonStr(markup);
        }
        return keyboardJson;
    }

    public static InlineKeyboardButton cancelButton () {
        return InlineKeyboardButton.builder().text("❌取消").callbackData("delete").build();
    }

    public static InlineKeyboardRow row (InlineKeyboardButton ... buttons) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.addAll(Arrays.asList(buttons));
        return row;
    }

    public static InlineKeyboardRow rowChosen (String name) {
        return new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(name)
                .switchInlineQueryChosenChat(SwitchInlineQueryChosenChat.builder()
                        .allowGroupChats(true)
                        .allowUserChats(true)
                        .allowChannelChats(true)
                        .build())
                .build());
    }

    public static InlineKeyboardButton chosenButton (String name, String defaultVal) {
        return InlineKeyboardButton.builder()
                .text(name)
                .switchInlineQueryChosenChat(SwitchInlineQueryChosenChat.builder()
                        .allowGroupChats(true)
                        .allowUserChats(true)
                        .allowChannelChats(true)
                        .build())
                .switchInlineQuery(defaultVal)
                .build();
    }

    public static InlineKeyboardRow row (String[] names, String[] callbacks) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        for (int i = 0; i < names.length; i++) {
            row.add(buttonText(names[i], callbacks[i]));
        }
        return row;
    }

    public static InlineKeyboardButton buttonUrl (String name, String url) {
        return InlineKeyboardButton.builder().url(url).text(name).build();
    }

     public static InlineKeyboardButton buttonText (String name, String callback) {
        return InlineKeyboardButton.builder().text(name).callbackData(callback).build();
    }

    public static String[] arr (String ... k) {
        return k;
    }


}
