package by.danefka.tgbgg;


import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import by.danefka.tgbgg.Enums.UserState;
import by.danefka.tgbgg.Utils.Pair;

public class DatabaseManager {
    private static DatabaseManager instance;

    private String URL;
    private String USER;
    private String PASSWORD;

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private DatabaseManager() {
        loadDatabaseConfig();
    }

    private void loadDatabaseConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Не удалось найти файл конфигурации.");
                return;
            }

            Properties prop = new Properties();
            prop.load(input);

            this.URL = prop.getProperty("db.url");
            this.USER = prop.getProperty("db.user");
            this.PASSWORD = prop.getProperty("db.password");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void saveGame(long userId, String username, String gameName) {
        String sql = "INSERT INTO plays (user_id, username, game_name) VALUES (?, ?, ?)";

        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, gameName);
            preparedStatement.executeUpdate();
            System.out.println("Игра успешно сохранена: " + gameName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public List<String> getGames(long userId, long offset) {
        String sql = "SELECT game_name, played_at FROM plays WHERE user_id = ? ORDER BY played_at  LIMIT ? OFFSET ?";
        List<String> plays = new ArrayList<>();

        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, 5 + 1);
            preparedStatement.setLong(3, offset);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String gameName = resultSet.getString("game_name");
                    Timestamp playedAt = resultSet.getTimestamp("played_at");

                    String formattedDate = new SimpleDateFormat("dd.MM.yyyy").format(playedAt);

                    plays.add(String.format("%s - %s", gameName, formattedDate));
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return plays;
    }

    public int getBotLastMessageId(long chatId) {
        String sql = "SELECT bot_last_message_id FROM chat_status WHERE chat_id = ?";
        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, chatId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Integer.parseInt(resultSet.getString("bot_last_message_id"));

                } else {
                    throw new SQLException("Database doesn't contains messageId for user:" + chatId);
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return 0;
    }


    public UserState getUserState(long chatId) {
        String sql = "SELECT user_state FROM chat_status WHERE chat_id = ?";
        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, chatId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return UserState.valueOf(resultSet.getString("user_state"));
                } else {
                    throw new SQLException("Database doesn't contains userState for user:" + chatId);
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return null;
    }

    public void setBotLastMessageId(long chatId, long messageId) {
        String sql = "INSERT INTO chat_status (chat_id, bot_last_message_id) VALUES (?, ?) ON CONFLICT (chat_id) DO UPDATE SET bot_last_message_id = EXCLUDED.bot_last_message_id";

        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, chatId);
            preparedStatement.setLong(2, messageId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении bot_last_message_id: " + e.getMessage());
        }
    }

    public void setUserState(long chatId, UserState userState) {
        String sql = "INSERT INTO chat_status (chat_id, user_state) VALUES (?, ?) " +
                     "ON CONFLICT (chat_id) DO UPDATE SET user_state = EXCLUDED.user_state";

        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, chatId);
            if (userState != null) {
                preparedStatement.setObject(2, userState, Types.OTHER);
            } else {
                preparedStatement.setNull(2, Types.OTHER);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении user_state: " + e.getMessage());
        }
    }

    public List<Pair<Integer, String>> getFriends(Long userId) {
        String sql = "SELECT friend_id, friend_name FROM friends WHERE user_id = ?";
        List<Pair<Integer, String>> friends = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int friendId = resultSet.getInt("friend_id");
                    String friendName = resultSet.getString("friend_name");
                    friends.add(new Pair<>(friendId, friendName));
                }
            }


        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return friends;
    }


    public void registerUser(Long userId, String username) {
        String sql = "INSERT INTO users (user_id, username) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при регистрации пользователя: " + e.getMessage());
        }
    }

    public long getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("user_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске user_id по username: " + e.getMessage());
        }
        return -1;
    }

    public String getUsernameByUserId(Long userId) {
        String sql = "SELECT username FROM users WHERE user_id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске user_id по username: " + e.getMessage());
        }
        return null;
    }

    public long createFriendRequest(Long userId, Long receiverId) {
        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) " +
                     "VALUES (?, ?, 'pending') ON CONFLICT (sender_id, receiver_id) DO NOTHING RETURNING request_id";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, receiverId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("request_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при создании запроса в друзья: " + e.getMessage());
        }
        return -1;
    }

    public long acceptFriendRequest(long requestId) {
        String getRequestSql = "SELECT sender_id, receiver_id FROM friend_requests WHERE request_id = ? AND status = 'pending'";
        String insertFriendSql = "INSERT INTO friends (user_id, friend_id, friend_name) VALUES (?, ?, ?)";
        String deleteRequestSql = "DELETE FROM friend_requests WHERE request_id = ?";

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement getRequestStmt = connection.prepareStatement(getRequestSql)) {
                getRequestStmt.setLong(1, requestId);
                try (ResultSet resultSet = getRequestStmt.executeQuery()) {
                    if (resultSet.next()) {
                        long senderId = resultSet.getLong("sender_id");
                        long receiverId = resultSet.getLong("receiver_id");

                        String senderName = getUsernameByUserId(senderId);
                        String receiverName = getUsernameByUserId(receiverId);

                        try (PreparedStatement insertStmt = connection.prepareStatement(insertFriendSql)) {
                            // Добавляем запись для обоих пользователей
                            insertStmt.setLong(1, senderId);
                            insertStmt.setLong(2, receiverId);
                            insertStmt.setString(3, receiverName);
                            insertStmt.executeUpdate();

                            insertStmt.setLong(1, receiverId);
                            insertStmt.setLong(2, senderId);
                            insertStmt.setString(3, senderName);
                            insertStmt.executeUpdate();
                        }

                        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteRequestSql)) {
                            deleteStmt.setLong(1, requestId);
                            deleteStmt.executeUpdate();
                        }

                        connection.commit();
                        return senderId;
                    }
                }
            } catch (SQLException e) {
                connection.rollback();
                System.err.println("Ошибка при принятии запроса в друзья: " + e.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка соединения с базой данных: " + e.getMessage());
        }
        return -1;
    }

    public boolean isFriends(long firstUserId, long secondUserId) {
        String sql = "SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?";

        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setLong(1, firstUserId);
            preparedStatement.setLong(2, secondUserId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next(); // Если есть хотя бы одна запись, значит, пользователи друзья
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке дружбы: " + e.getMessage());
        }
        return false;
    }
}

