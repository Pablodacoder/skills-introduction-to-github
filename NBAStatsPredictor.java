import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.json.JSONArray;

public class NBAStatsPredictor {

    private static final String API_URL = "https://www.balldontlie.io/api/v1/players?search=";
    private static final String STATS_URL = "https://www.balldontlie.io/api/v1/season_averages?season=2023&player_ids[]=";

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter player name: ");
        String playerName = input.nextLine();

        int playerId = fetchPlayerId(playerName);

        if (playerId != -1) {
            JSONObject playerStats = fetchPlayerStats(playerId);

            if (playerStats != null) {
                double predictedPoints = predictStat(playerStats, "pts");
                double predictedAssists = predictStat(playerStats, "ast");
                double predictedRebounds = predictStat(playerStats, "reb");

                System.out.println("Predicted stats for " + playerName + ":");
                System.out.println("Points: " + predictedPoints);
                System.out.println("Assists: " + predictedAssists);
                System.out.println("Rebounds: " + predictedRebounds);
            } else {
                System.out.println("Failed to fetch stats.");
            }
        } else {
            System.out.println("Player not found.");
        }
        input.close();
    }

    private static int fetchPlayerId(String playerName) {
        try {
            URL url = new URL(API_URL + playerName.replace(" ", "%20"));
            HttpURLConnection conn = makeRequest(url);

            if (conn.getResponseCode() != 200) {
                return -1;
            }

            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            JSONObject jsonResponse = new JSONObject(response);
            JSONArray data = jsonResponse.getJSONArray("data");

            if (data.length() > 0) {
                return data.getJSONObject(0).getInt("id");
            } else {
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static JSONObject fetchPlayerStats(int playerId) {
        try {
            URL url = new URL(STATS_URL + playerId);
            HttpURLConnection conn = makeRequest(url);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            JSONObject jsonResponse = new JSONObject(response);
            JSONArray data = jsonResponse.getJSONArray("data");

            return data.length() > 0 ? data.getJSONObject(0) : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double predictStat(JSONObject stats, String key) {
        double value = stats.optDouble(key, 0.0);
        return value * (1 + Math.random() * 0.1); // Random increase between 0-10%
    }

    private static HttpURLConnection makeRequest(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int attempts = 0;
        while (attempts < 3) {
            try {
                if (conn.getResponseCode() == 200) {
                    return conn;
                }
            } catch (IOException e) {
                attempts++;
                System.out.println("Retrying API request... Attempt " + (attempts + 1));
                try {
                    if (conn.getResponseCode() == 200) {
                        return conn;
                    }
                } catch (IOException e) {
                    attempts++;
                    System.out.println("Retrying API request... Attempt " + (attempts + 1));
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            return conn;
        }
    }