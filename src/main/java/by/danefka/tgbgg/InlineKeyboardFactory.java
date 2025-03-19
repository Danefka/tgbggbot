package by.danefka.tgbgg;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InlineKeyboardFactory {
    public static InlineKeyboardMarkup mainMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        ArrayList<InlineKeyboardButton> line1 = new ArrayList<>();
        InlineKeyboardButton showGamesButton = newButton("\uD83D\uDC40 Посмотреть партии", "view_plays:0");
        line1.add(showGamesButton);
        InlineKeyboardButton addGameButton = newButton("Добавить партию \uD83D\uDCDD", "add_play");
        line1.add(addGameButton);
        keyboard.add(line1);

        ArrayList<InlineKeyboardButton> line2 = new ArrayList<>();
        InlineKeyboardButton friendsButton = newButton("Друзья", "friends");
        line2.add(friendsButton);
        keyboard.add(line2);

        ArrayList<InlineKeyboardButton> line3 = new ArrayList<>();
        InlineKeyboardButton findGameButton = newButton("\uD83D\uDD0D Найти игру", "search");
        line3.add(findGameButton);
        keyboard.add(line3);


        markup.setKeyboard(keyboard);

        return markup;
    }

    public static InlineKeyboardMarkup searchMenu(List<BoardGame> boardGames) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (BoardGame game : boardGames) {
            InlineKeyboardButton gameButton = newButton(game.getTitle(), "get_game:" + game.getId());
            keyboard.add(Collections.singletonList(gameButton));
        }


        keyboard.add(Collections.singletonList(mainMenuButton()));

        markup.setKeyboard(keyboard);
        return markup;
    }

    public static InlineKeyboardMarkup addMenu(List<BoardGame> boardGames) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (BoardGame game : boardGames) {
            InlineKeyboardButton gameButton = newButton(game.getTitle(), "add_play:" + game.getId());
            keyboard.add(Collections.singletonList(gameButton));
        }

        keyboard.add(Collections.singletonList(mainMenuButton()));

        markup.setKeyboard(keyboard);

        return markup;
    }

    public static InlineKeyboardMarkup friendMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> line1 = new ArrayList<>();
        line1.add(newButton("Добавить друзей", "add_friend"));
        line1.add(newButton("Удалить из друзей", "delete_friend"));

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(newButton("Изменить имя друга", "edit_friend"));
        keyboard.add(line1);

        List<InlineKeyboardButton> line3 = new ArrayList<>();
        line3.add(newButton("Заявки в друзья", "friends_requests"));
        keyboard.add(line1);

        keyboard.add(Collections.singletonList(mainMenuButton()));
        markup.setKeyboard(keyboard);
        return markup;
    }


    public static InlineKeyboardButton newButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }


    public static InlineKeyboardButton mainMenuButton() {
        return newButton("Вернуться в меню", "main_menu");
    }

    public static InlineKeyboardMarkup requestMarkup(long requestId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> line1 = new ArrayList<>();
        line1.add(newButton("Принять запрос", "accept_friend_request:" + requestId));
        line1.add(newButton("Отклонить запрос", "deny_friend_request:" + requestId));
        keyboard.add(line1);

        keyboard.add(Collections.singletonList(mainMenuButton()));
        markup.setKeyboard(keyboard);
        return markup;
    }
}
