package by.danefka.tgbgg.Utils;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    static public String[] splitBySentences(String text, int maxLength) {
        List<String> parts = new ArrayList<>();
        String[] sentences = text.split("(?<=\\.\\s)"); // Разбиваем по точке и пробелу

        StringBuilder currentPart = new StringBuilder();
        for (String sentence : sentences) {
            if (currentPart.length() + sentence.length() > maxLength) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }
            currentPart.append(sentence);
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts.toArray(new String[0]);
    }

    public static String cleanText(String text) {
        String decodedText = Jsoup.parse(text).text();

        decodedText = decodedText.replaceAll("\\s{2,}", "\n").trim();

        return decodedText;
    }

}
