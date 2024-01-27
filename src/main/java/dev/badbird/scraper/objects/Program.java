package dev.badbird.scraper.objects;

import dev.badbird.scraper.Main;
import dev.badbird.scraper.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Data
@AllArgsConstructor
public class Program {
    private String name;
    private String school;
    private String entityId;
    private List<String> requirements;

    public CompletableFuture<List<String>> loadRequirements(OkHttpClient client, String cookie) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        // https://core.myblueprint.ca/V5/Entity?entityId=P.22204&_=1706316891397
        String url = "https://core.myblueprint.ca/V5/Entity?entityId=" + entityId; // + "&_=" + System.currentTimeMillis();
        File cacheFile = new File(Main.cacheFolder, FileUtils.escape(url) + ".cache");
        Consumer<String> consumer = body -> {
            Elements table = Jsoup.parse(body).select("#tabs-Program_Requirements > div:nth-child(1) > div.large-6.medium-12.small-12.columns.end > div > div > div > div > div > div:nth-child(3) > table");
            // get all the classes, ignore checkmarks
            Elements trs = table.select("tr");
            requirements = new ArrayList<>();
            for (Element tr : trs) {
                Elements tds = tr.select("td");
                if (tds.size() != 2) continue;
                String requirement = tds.get(0).text();
                requirements.add(requirement);
            }
            System.out.println("Requirements for " + name + " @ " + school + ": " + requirements.size());
            for (String requirement : requirements) {
                System.out.println(" - " + requirement);
            }
            future.complete(requirements);
        };
        if (cacheFile.exists()) {
            try {
                consumer.accept(new String(Files.readAllBytes(cacheFile.toPath())));
            } catch (IOException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
            return future;
        }
        Request request = new Request.Builder()
                .url(url)
                .header("Cookie", ".AspNet.ApplicationCookie=" + cookie)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load requirements for " + name + " @ " + school + ": " + e.getMessage());
                future.complete(requirements);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String body = response.body().string();
                cacheFile.createNewFile();
                Files.write(cacheFile.toPath(), body.getBytes());
                consumer.accept(body);
            }
        });
        return future;
    }
}
