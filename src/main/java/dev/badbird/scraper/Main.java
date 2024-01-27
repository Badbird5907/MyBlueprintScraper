package dev.badbird.scraper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.badbird.scraper.objects.Program;
import dev.badbird.scraper.objects.Provinces;
import dev.badbird.scraper.util.FileUtils;
import dev.badbird.scraper.util.QueryBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final String AUTH_COOKIE_NAME = ".AspNet.ApplicationCookie";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static File cacheFolder = new File("cache");

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        cacheFolder.mkdirs();
        // https://core.myblueprint.ca/V5/Program/DoSearch?filters%5BPlan%5D=&filters%5BCategory%5D=&filters%5BProvince%5D=%2C1&filters%5BCity%5D=&filters%5BInstitution%5D=&filters%5BProgramType%5D=&filters%5BProgramEduReq%5D=&filters%5BLanguage%5D=en-CA&filters%5BKeyword%5D=Computer%2520Science&filters%5BPreFilterEntityId%5D=&page=0&pagesize=15&_=1706322099004
        String listUrl = "https://core.myblueprint.ca/V5/Program/DoSearch?";
        List<Provinces> provinces = Arrays.asList(Provinces.values());
        String keywords = "Computer Science";
        QueryBuilder listQuery = new QueryBuilder();
        listQuery.add("filters[Keyword]", keywords);
        listQuery.add("filters[Province]", provinces.stream()
                .map(Provinces::getUrlParam)
                .reduce("", (a, b) -> a + b));
        listQuery.add("filters[Language]", "en-CA");
        listQuery.add("page", 0);
        listQuery.add("pagesize", 1500);
        // listQuery.add("pagesize", 15);
        // listQuery.add("_", System.currentTimeMillis());

        String cookie = new String(Files.readAllBytes(new File("cookie.txt").toPath()));

        String url = listUrl + listQuery.build(true);
        File cacheFile = new File(cacheFolder, FileUtils.escape(url) + ".cache");
        System.out.println(url + " / " + cacheFile.getName());
        String listResponse;
        if (cacheFile.exists()) {
            listResponse = new String(Files.readAllBytes(cacheFile.toPath()));
        } else {
            Request listRequest = new Request.Builder()
                    .url(url)
                    .header("Cookie", AUTH_COOKIE_NAME + "=" + cookie)
                    .get()
                    .build();
            listResponse = client.newCall(listRequest).execute().body().string();
            cacheFile.createNewFile();
            Files.write(cacheFile.toPath(), listResponse.getBytes());
        }
        System.out.println(listResponse);
        String[] programsArr = extractProgramsBlocks(listResponse);
        System.out.println("Found " + programsArr.length + " programs.");
        List<Program> programs = new ArrayList<>();
        for (String program : programsArr) {
            programs.add(generateProgram(program));
        }

        List<CompletableFuture<List<String>>> futures = programs.stream().map(p -> p.loadRequirements(client, cookie)).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long end = System.currentTimeMillis();
        File out = new File("programs.json");
        if (out.exists())
            out.delete();
        out.createNewFile();
        JsonObject object = new JsonObject();
        object.addProperty("start", start);
        object.addProperty("end", end);
        object.addProperty("timeMS", end - start);
        object.addProperty("keywords", keywords);
        object.add("programs", gson.toJsonTree(programs));
        Files.write(out.toPath(), gson.toJson(object).getBytes());
        /*
        for (int i = 0; i < 5; i++) {
            CompletableFuture<List<String>> future = programs.get(i).loadRequirements(client, cookie);
            future.join();
        }
         */
    }

    private static String[] extractProgramsBlocks(String input) {
        String[] split = input.split("<div class=\"cardItem  small-6 medium-6 large-4 cardView columns end\" data-rank=\"0\">"); // holy shit
        List<String> list = new ArrayList<>();
        for (String s : split) {
            String[] lines = s.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains("<div id=\"info\" class=\"info hasSubtitle\">")) {
                    String[] lines1 = Arrays.copyOfRange(lines, i, i + 22); // grab the next 22 lines which is the end of this block
                    list.add(String.join("\n", lines1));
                }
            }
        }
        return list.toArray(new String[0]);
    }

    private static final Pattern ENTITY_ID_PROGRAM = Pattern.compile("data-entityid=\"(.*?)\""),
            KENDO_TIP_PATTERN = Pattern.compile("<span class=\"kendo-tip\" .*>(.*?)</span>");

    private static Program generateProgram(String in) {
        /*
        <div id="info" class="info hasSubtitle">
                    <div class="entityCardTitle cardDotdotdot">
                        <span class="kendo-tip" title="Computer Science">Computer Science</span>
                    </div>

                        <div class="entityCardSubtitle">
                            <span class="kendo-tip" title="Royal Military College of Canada">Royal Military College of Canada</span>
                        </div>

                    <div class="entityInlineInfo">
                            <div class="inlineInfo">
                                        <span class="title">Province</span>
                                        <span class="value">Ontario</span>
                                        <span class="title">Tuition</span>
                                        <span class="value"><span class='amt'>$6,215</span> <span class='rng'>for 1 year(s)</span></span>
                                        <span class="title">Program Length</span>
                                        <span class="value">4 year(s)</span>
                                                            </div>

                    </div>
                    <a href="#entityCardBoxContext" class="entityCardBoxContext" data-popout data-entityid="P.22204"></a>
                </div>
         */
        Matcher idMatcher = ENTITY_ID_PROGRAM.matcher(in);
        String entityId;
        if (idMatcher.find()) {
            entityId = idMatcher.group(1);
        } else {
            return null; // not a valid program
        }
        // first is title, second is school
        Matcher kendoTipMatcher = KENDO_TIP_PATTERN.matcher(in);
        String title = null, school = null;
        int i = 0;
        while (kendoTipMatcher.find()) {
            if (i == 0) {
                title = Jsoup.clean(kendoTipMatcher.group(1), Safelist.none());
            } else if (i == 1) {
                school = Jsoup.clean(kendoTipMatcher.group(1), Safelist.none());
            } else {
                System.out.println("Found more than 2 kendo tips in program block!");
                System.out.println(in);
            }
            i++;
        }
        return new Program(title, school, entityId, new ArrayList<>());
    }

}