package by.danefka.tgbgg;

public class BoardGame {
    private long id;
    private String title;
    private String imageUrl;
    private String description;
    private int worldRank;
    private double averageRating;
    private int minPlayers;
    private int maxPlayers;
    private int bestPlayers;

    public BoardGame(String title,long id,String description, String imageUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.id = id;
    }

    public BoardGame(String title, long id) {
        this.title = title;
        this.id = id;
    }

    public BoardGame(long id, String title, String description, String imageUrl,
                     int worldRank, double averageRating, int minPlayers, int maxPlayers, int bestPlayers) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.worldRank = worldRank;
        this.averageRating = averageRating;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.bestPlayers = bestPlayers;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public int getWorldRank() { return worldRank; }
    public double getAverageRating() { return averageRating; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getBestPlayers() { return bestPlayers; }
}

