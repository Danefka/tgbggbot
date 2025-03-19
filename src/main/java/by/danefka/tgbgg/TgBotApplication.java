package by.danefka.tgbgg;

import by.danefka.tgbgg.Enums.UserState;
import by.danefka.tgbgg.Utils.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@SpringBootApplication
public class TgBotApplication extends TelegramLongPollingBot {
    private String botUserName;
    private String botToken;

//    private static HashMap<String,Consumer<>> callbackHandler = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && !update.getMessage().getText().isEmpty()) {
            DatabaseManager databaseManager = new DatabaseManager();
            if (update.getMessage().getText().equals("/start")) {
                start(update);
            } else {
                UserState userState = databaseManager.getUserState(update.getMessage().getChatId());
                if (userState == null) {
                    return;
                }
                switch (userState) {
                    case addWaitingForGameTitle -> addPlayGetUserInput(update);
                    case searchWaitingForGameTitle -> searchGameGetSearchResult(update);
                    case addFriendWaitingForFriendUserName -> addFriendWithUserInput(update);
                }
                deleteMessage(update.getMessage().getChatId(), update.getMessage().getMessageId());

            }
        } else if (update.hasCallbackQuery() && !update.getCallbackQuery().getData().isEmpty()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            if (callbackQuery.getData().contains(":")) {
                String command = callbackQuery.getData().split(":")[0];
                switch (command) {
                    case "add_play":
                        addPlayByIdCallBackQuery(update);
                        break;
                    case "view_plays":
                        viewPlays(update);
                        break;
                    case "get_game":
                        sendGameByIdCallBackQuery(update);
                        break;
                    case "accept_friend_request":
                        acceptFriendRequest(update);
                        break;

                }
            } else {
                String command = callbackQuery.getData();
                switch (command) {
                    case "add_play":
                        addPlayCallBackQuery(update);
                        break;
                    case "main_menu":
                        mainMenu(update);
                        break;
                    case "search":
                        searchGameCallBackQuery(update);
                        break;
                    case "friends":
                        friendsMenu(update);
                        break;
                    case "add_friend":
                        addFriend(update);
                        break;
                }
            }
        }
    }

    private void acceptFriendRequest(Update update) {
        Long requestId = Long.parseLong(update.getCallbackQuery().getData().split(":")[1]);

        DatabaseManager databaseManager = new DatabaseManager();
        Long senderId = databaseManager.acceptFriendRequest(requestId);

        deleteMessage(senderId, databaseManager.getBotLastMessageId(senderId));
        sendMessage(senderId, "Пользователь: " + update.getCallbackQuery().getFrom().getUserName() + " принял ваш запрос в друзья", InlineKeyboardFactory.mainMenu());
        editMessage(update, "Вы приняли запрос пользователя: " + databaseManager.getUsernameByUserId(senderId) + " на добавление вас в друзья", InlineKeyboardFactory.mainMenu());
    }

    private void addFriendWithUserInput(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        Long friendId = databaseManager.getUserIdByUsername(update.getMessage().getText());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));

        if (friendId == null) {
            editMessage(update, "Извините, но чтобы вашего друга можно было добавить в друзья, он должен отправить мне сообщение.", markup);
            return;
        } else if (friendId == update.getMessage().getFrom().getId()) {
            editMessage(update, "Извините, но вы не можете добавить сами себя в друзья", markup);
            return;
        } else if (databaseManager.isFriends(update.getMessage().getFrom().getId(), friendId)) {
            editMessage(update, "Вы уже добавили " + update.getMessage().getText() + " в друзья", markup);
        }

        sendFriendRequestNotification(update,friendId);
        editMessage(update, "отправил пользователю: " + update.getMessage().getText() + " ваше приглашение в друзья", markup);
    }

    private void addFriend(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.setUserState(update.getCallbackQuery().getFrom().getId(), UserState.addFriendWaitingForFriendUserName);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));
        editMessage(update, "Введите имя вашего друга в телеграмме", markup);
    }

    private void friendsMenu(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        List<Pair<Integer, String>> friends = databaseManager.getFriends(update.getCallbackQuery().getFrom().getId());
        StringBuilder listOfFriends = new StringBuilder();
        if (friends.isEmpty()) {
            listOfFriends.append("Вы не ещё добавляли своих друзей, можете сделать это прямо сейчас");
            InlineKeyboardButton addFriendButton = InlineKeyboardFactory.newButton("Добавить друзей", "add_friend");
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(Collections.singletonList(addFriendButton));
            keyboard.add(Collections.singletonList(InlineKeyboardFactory.mainMenuButton()));
            markup.setKeyboard(keyboard);
            editMessage(update, listOfFriends.toString(), markup);
            return;
        }
        listOfFriends.append("*Список ваших друзей*\n\n");
        for (Pair<Integer, String> friend : friends) {
            listOfFriends.append(friend.getValue()).append("\n");
        }
        editMessage(update, listOfFriends.toString(), InlineKeyboardFactory.friendMenu());
    }


    private void start(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.registerUser(update.getMessage().getFrom().getId(), update.getMessage().getFrom().getUserName());
        sendMessage(update, "Добро пожаловать, чем могу вам помочь?", InlineKeyboardFactory.mainMenu());
    }

    private void mainMenu(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.setUserState(update.getCallbackQuery().getFrom().getId(), null);

        editMessage(update, "Главное меню", InlineKeyboardFactory.mainMenu());
    }


    private void sendGameByIdCallBackQuery(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            try {


                long gameId = Long.parseLong(callbackData.split(":")[1]);
                BoardGame game = BGGApiService.getGameById(gameId);

                if (game != null) {
                    DatabaseManager databaseManager = new DatabaseManager();
                    databaseManager.setUserState(chatId, null);
                    deleteMessage(chatId, databaseManager.getBotLastMessageId(chatId));

                    String gameInfo = String.format(
                            "*%s*\n\n" +
                            "🌍 *Мировой рэйтинг:* %s\n" +
                            "⭐ *Средняя оценка:* %s\n" +
                            "👥 *Количество игроков:* %s (лучше всего для %s)\n\n",
                            game.getTitle(),
                            game.getWorldRank(),
                            game.getAverageRating(),
                            game.getMinPlayers() + "-" + game.getMaxPlayers(),
                            game.getBestPlayers()
                    );

                    String description = game.getDescription();
                    String fullCaption = gameInfo + "*Описание:* \n" + description;


                    // Если описание влезает в подпись к фото (≤ 1000 символов), отправляем всё сразу
                    if (fullCaption.length() <= 1000) {

                        sendMessage(update, fullCaption, game.getImageUrl(), null);
                    } else {
                        // Если описание слишком длинное, отправляем только основную информацию в фото

                        sendMessage(update, gameInfo + "*Описание:*", game.getImageUrl(), null);

                        // Разбиваем длинное описание по предложениям и отправляем отдельными сообщениями
                        String[] parts = StringUtils.splitBySentences(description, 1000);
                        for (String part : parts) {
                            sendMessage(update, part, null);
                        }
                    }

                    sendMessage(update, "Чем ещё могу вам помочь?", InlineKeyboardFactory.mainMenu());
                }
            } catch (NumberFormatException e) {
                System.err.println("Ошибка парсинга ID игры: " + e.getMessage());
            }


        }
    }


    private void searchGameGetSearchResult(Update update) {
        String userInput = update.getMessage().getText();
        List<BoardGame> boardGames = BGGApiService.searchGame(userInput);

        if (boardGames.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));
            editMessage(update, "Ничего не найдено по запросу: " + userInput + ", попробуйте ещё раз", markup);
        } else {
            editMessage(update, "Результаты по запросу:" + userInput, InlineKeyboardFactory.searchMenu(boardGames));
        }
    }

    private void searchGameCallBackQuery(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.setUserState(update.getCallbackQuery().getFrom().getId(), UserState.searchWaitingForGameTitle);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));

        editMessage(update, "Введите название игры", markup);
    }

    private void addPlayCallBackQuery(Update update) {
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.setUserState(update.getCallbackQuery().getFrom().getId(), UserState.addWaitingForGameTitle);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));

        editMessage(update, "Введите название игры", markup);
    }

    private void viewPlays(Update update) {
        long userId = update.getCallbackQuery().getFrom().getId();
        long offset = Long.parseLong(update.getCallbackQuery().getData().split(":")[1]);

        DatabaseManager databaseManager = new DatabaseManager();
        List<String> games = databaseManager.getGames(userId, offset);

        StringBuilder stringBuilder = new StringBuilder("Партии сыгранные вами:\n");
        for (int i = 0; i < 5 && i < games.size(); i++) {
            stringBuilder.append(games.get(i)).append("\n");
        }


        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> arrows = new ArrayList<>();
        if (offset != 0) {
            InlineKeyboardButton leftArrow = new InlineKeyboardButton();
            leftArrow.setText("<-");
            leftArrow.setCallbackData("view_plays:" + (offset - 5));
            arrows.add(leftArrow);
        }

        if (games.size() == 6) {
            InlineKeyboardButton rightArrow = new InlineKeyboardButton();
            rightArrow.setText("->");
            rightArrow.setCallbackData("view_plays:" + (offset + 5));
            arrows.add(rightArrow);
        }
        if (games.isEmpty()) {
            stringBuilder = new StringBuilder("Вы не добавили ни одной партии");
            InlineKeyboardButton addGameButton = new InlineKeyboardButton();
            addGameButton.setText("Добавить партию \uD83D\uDCDD");
            addGameButton.setCallbackData("add_plays");
            rows.add(Collections.singletonList(addGameButton));
        }
        if (!arrows.isEmpty()) {
            rows.add(arrows);
        }
        rows.add(Collections.singletonList(InlineKeyboardFactory.mainMenuButton()));
        markup.setKeyboard(rows);

        editMessage(update, stringBuilder.toString(), markup);
    }


    private void addPlayGetUserInput(Update update) {
        String userInput = update.getMessage().getText();

        List<BoardGame> boardGames = BGGApiService.searchGame(userInput);


        if (boardGames.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(Collections.singletonList(Collections.singletonList(InlineKeyboardFactory.mainMenuButton())));
            editMessage(update, "Ничего не найдено по запросу: " + userInput + ", попробуйте ещё раз", markup);
        } else {
            editMessage(update, "Результаты по запросу:" + userInput, InlineKeyboardFactory.addMenu(boardGames));
        }


    }

    private void addPlayByIdCallBackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        long gameId = Long.parseLong(callbackQuery.getData().split(":")[1]);
        long userId = callbackQuery.getFrom().getId();


        String userName = callbackQuery.getFrom().getUserName();
        BoardGame boardGame = BGGApiService.getGameById(gameId);
        editMessage(update, "Добавил партию в " + boardGame.getTitle() + " в вашу статистику", InlineKeyboardFactory.mainMenu());


        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.saveGame(userId, userName, boardGame.getTitle());
        databaseManager.setUserState(userId, null);

    }

    private void sendFriendRequestNotification(Update update, Long receiverId) {
        String text = update.getMessage().getFrom().getUserName() + " хочет добавить вас в друзья.\n";
        DatabaseManager databaseManager = new DatabaseManager();
        long requestId = databaseManager.createFriendRequest(update.getMessage().getFrom().getId(), receiverId);
        InlineKeyboardMarkup markup = InlineKeyboardFactory.requestMarkup(requestId);
        deleteMessage(receiverId,databaseManager.getBotLastMessageId(receiverId));
        sendMessage(receiverId, text, markup);
    }


    public void sendMessage(Update update, String text, String photoUrl, InlineKeyboardMarkup markup) {
        long chatId;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            chatId = update.getCallbackQuery().getFrom().getId();
        }


        SendPhoto message = new SendPhoto();
        message.setChatId(chatId);
        message.setCaption(text);
        message.setReplyMarkup(markup);
        message.setPhoto(new InputFile(photoUrl));

        try {
            Message sentMessage = execute(message);
            int messageId = sentMessage.getMessageId();
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.setBotLastMessageId(chatId, messageId);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке фото: " + e.getMessage());
        }
    }

    public void sendMessage(Update update, String text, InlineKeyboardMarkup markup) {
        long chatId;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            chatId = update.getCallbackQuery().getFrom().getId();
        }

        sendMessage(chatId,text,markup);
    }

    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        if (markup != null) {
            message.setReplyMarkup(markup);
        }

        try {
            Message sentMessage = execute(message);
            int messageId = sentMessage.getMessageId();
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.setBotLastMessageId(chatId, messageId);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }


    }

    public void editMessage(Update update, String text, InlineKeyboardMarkup markup) {
        long chatId;

        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else {
            chatId = update.getCallbackQuery().getFrom().getId();
        }

        EditMessageText editMessageText = new EditMessageText();
        DatabaseManager databaseManager = new DatabaseManager();

        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(databaseManager.getBotLastMessageId(chatId));
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(markup);

        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при удалении сообщения: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }


    public TgBotApplication() {
        try (InputStream input = TgBotApplication.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Извините, не удалось найти файл конфигурации.");
                throw new RuntimeException("file application.properties is null");
            }

            Properties prop = new Properties();
            prop.load(input);

            this.botUserName = prop.getProperty("bot.userName");
            this.botToken = prop.getProperty("bot.token");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TgBotApplication());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
