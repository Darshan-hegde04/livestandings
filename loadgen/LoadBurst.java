import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Sunday burst (Java).
 *
 * Models the Sunday load:
 *   - 200 matches, one league
 *   - arrive in clumps of 30 (within a few seconds each)
 *   - ~12% corrections (same match id, new scores) after the burst
 *
 * Run: java loadgen/LoadBurst.java
 */
public class LoadBurst {

    static final String BASE = "http://localhost:8080";
    static final String TOKEN = "dev-organiser-token";
    static final String LEAGUE = "sunday-league";
    static final int MATCHES = 200;
    static final int CLUMP = 30;        // clumps of 20–30
    static final long PAUSE_MS = 1000;  // gap between clumps
    static final int CORRECTIONS = 24;  // ~12%
    static final String[] TEAMS = {
            "Alpha", "Beta", "Gamma", "Delta", "Epsilon",
            "Zeta", "Eta", "Theta", "Iota", "Kappa"
    };

    record Job(String matchId, String home, String away, int homeScore, int awayScore) {}

    public static void main(String[] args) throws Exception {
        String url = BASE + "/api/leagues/" + LEAGUE + "/results";
        HttpClient http = HttpClient.newHttpClient();
        long stamp = System.currentTimeMillis() / 1000;

        List<Job> jobs = new ArrayList<>(MATCHES);
        for (int i = 0; i < MATCHES; i++) {
            String home = TEAMS[i % TEAMS.length];
            String away = TEAMS[(i + 3) % TEAMS.length];
            jobs.add(new Job(
                    "b-" + stamp + "-" + String.format("%04d", i),
                    home,
                    away,
                    i % 4,
                    (i * 3) % 4
            ));
        }

        System.out.printf("league=%s matches=%d clump=%d corrections=%d%n",
                LEAGUE, MATCHES, CLUMP, CORRECTIONS);
        System.out.println("submitting clumps through REST → Kafka…");

        for (int start = 0; start < MATCHES; start += CLUMP) {
            List<Job> batch = jobs.subList(start, Math.min(start + CLUMP, MATCHES));
            System.out.printf("  clump %d-%d (%d in parallel)%n",
                    start + 1, start + batch.size(), batch.size());
            try (var pool = Executors.newFixedThreadPool(batch.size())) {
                List<Future<?>> futs = new ArrayList<>();
                for (Job j : batch) {
                    String body = """
                            {"matchId":"%s","homeTeam":"%s","awayTeam":"%s","homeScore":%d,"awayScore":%d}
                            """.formatted(j.matchId(), j.home(), j.away(), j.homeScore(), j.awayScore()).trim();
                    futs.add(pool.submit(() -> send(http, "POST", url, body)));
                }
                for (Future<?> f : futs) {
                    f.get();
                }
            }
            if (start + CLUMP < MATCHES) {
                Thread.sleep(PAUSE_MS);
            }
        }

        System.out.println("waiting before corrections…");
        Thread.sleep(2000);
        System.out.println("correcting " + CORRECTIONS + " (same match id, new scores)…");
        try (var pool = Executors.newFixedThreadPool(CLUMP)) {
            List<Future<?>> futs = new ArrayList<>();
            for (int i = 0; i < CORRECTIONS; i++) {
                Job j = jobs.get(i);
                String body = """
                        {"homeTeam":"%s","awayTeam":"%s","homeScore":%d,"awayScore":%d}
                        """.formatted(j.home(), j.away(), (i + 1) % 5, i % 3).trim();
                futs.add(pool.submit(() -> send(http, "PUT", url + "/" + j.matchId(), body)));
            }
            for (Future<?> f : futs) {
                f.get();
            }
        }

        System.out.println("done — watch http://localhost:3000 (league=" + LEAGUE + ")");
    }

    static void send(HttpClient http, String method, String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Organiser-Token", TOKEN)
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException(method + " " + url + " -> HTTP " + resp.statusCode());
        }
    }
}
