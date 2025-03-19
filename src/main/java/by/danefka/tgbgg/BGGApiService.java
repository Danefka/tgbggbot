package by.danefka.tgbgg;

import by.danefka.tgbgg.Utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BGGApiService {
    private static final String SEARCH_URL = "https://boardgamegeek.com/xmlapi2/search?query=";
    private static final String THING_URL = "https://boardgamegeek.com/xmlapi2/thing?id=";

    public static List<BoardGame> searchGame(String gameTitle) {
        ArrayList<BoardGame> boardGames = new ArrayList<>();

        try {
            String encodedTitle = URLEncoder.encode(gameTitle, StandardCharsets.UTF_8);
            String url = SEARCH_URL + encodedTitle;

            Document doc = Jsoup.connect(url).get();
            Elements items = doc.select("item");

            for (Element item : items) {
                long id = Long.parseLong(item.attr("id"));
                String name = item.selectFirst("name").attr("value");

                boardGames.add(new BoardGame(name, id));
            }

        } catch (Exception e) {
            System.err.println("Error searching games: " + e.getMessage());
        }
        return boardGames;
    }


    public static BoardGame getGameById(long id) {
        try {
            String url = THING_URL + id + "&stats=1";
            Document doc = Jsoup.connect(url).get();
            Element item = doc.selectFirst("item");

            if (item != null) {
                // Название игры
                String gameTitle = item.selectFirst("name[type=primary]").attr("value");

                // Описание игры
                String description = item.selectFirst("description").text();

                // Разбиваем описание по предложениям
                String[] descriptionParts = description.split("\\. ");
                description = String.join(".\n", descriptionParts);
                description = StringUtils.cleanText(description);

                // Ссылка на изображение
                String imageUrl = item.selectFirst("image").text();

                // Средняя оценка
                double averageRating = 0.0;
                Element ratings = item.selectFirst("statistics ratings");
                if (ratings != null) {
                    averageRating = Double.parseDouble(ratings.selectFirst("average").attr("value"));
                }

                // Мировой рейтинг
                int worldRank = -1;
                Elements ranks = ratings.select("ranks rank");
                for (Element rank : ranks) {
                    if ("boardgame".equals(rank.attr("name"))) {
                        worldRank = Integer.parseInt(rank.attr("value"));
                        break;
                    }
                }

                // Количество игроков
                int minPlayers = Integer.parseInt(item.selectFirst("minplayers").attr("value"));
                int maxPlayers = Integer.parseInt(item.selectFirst("maxplayers").attr("value"));

                // Оптимальное число игроков
                int bestPlayers = (minPlayers + maxPlayers) / 2; // По умолчанию среднее
                Elements suggestedPlayers = item.select("poll[name=suggested_numplayers] results");

                for (Element player : suggestedPlayers) {
                    Elements results = player.select("result");
                    int bestVotes = 0;
                    int bestChoice = bestPlayers;

                    for (Element result : results) {
                        String value = result.attr("value");
                        int votes = Integer.parseInt(result.attr("numvotes"));

                        if ("Best".equals(result.attr("level")) && votes > bestVotes) {
                            bestVotes = votes;
                            bestChoice = Integer.parseInt(value);
                        }
                    }

                    bestPlayers = bestChoice;
                }

                return new BoardGame(id,gameTitle, description, imageUrl, worldRank, averageRating, minPlayers, maxPlayers, bestPlayers);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при получении данных об игре", e);
        }
        return null;
    }

}
