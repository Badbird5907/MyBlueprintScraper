package dev.badbird.scraper.gpt;

import com.google.gson.*;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import dev.badbird.scraper.objects.Program;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GPTFilter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String GPT_PROMPT = """
            Below is a list of universities/colleges in Canada that offer %s programs. Give me the first 20 schools that are well known.
            Only output the name, do not say anything else.
            
            %s
            """;
    @SneakyThrows
    public static void main(String[] args) {
        File tokenFile = new File("token.txt");
        System.out.println(tokenFile.getAbsolutePath());
        String token = new String(Files.readAllBytes(tokenFile.toPath()));

        List<Program> programs = new ArrayList<>();
        String json = new String(Files.readAllBytes(new File("programs.json").toPath()));
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        JsonArray arr = object.get("programs").getAsJsonArray();
        for (JsonElement element : arr) {
            JsonObject obj = element.getAsJsonObject();
            JsonArray requirements = obj.get("requirements").getAsJsonArray();
            if (requirements.isEmpty()) continue;
            programs.add(gson.fromJson(obj, Program.class));
        }
        System.out.println("Total programs (filtered for empty reqs): " + programs.size());
        System.out.println("Total programs (unfiltered): " + arr.size());

        String names = programs.stream().map(program -> program.getSchool().toLowerCase().trim())
                .distinct() // filter out duplicates
                // re-capitalize
                .map(GPTFilter::capatalizeFirstDeep)
                .reduce("", (a, b) -> a + b + "\n");
        String keywords = object.get("keywords").getAsString();
        String prompt = String.format(GPT_PROMPT, keywords, names);
        System.out.println(prompt);

        OpenAiService service = new OpenAiService(token);
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(message);
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(messages)
                .model("gpt-4")
                .build();
        String response = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
        System.out.println(response);
        File outputFile = new File("gpt_output.txt");
        if (!outputFile.exists())
            outputFile.createNewFile();
        Files.write(outputFile.toPath(), response.getBytes());

        String[] split = response.split("\n");
        List<Program> topPrograms = new ArrayList<>();
        for (String s : split) {
            if (s.isBlank()) continue;
            String school = s.trim();
            Program program = programs.stream().filter(p -> p.getSchool().trim().equalsIgnoreCase(school.trim())).findFirst().orElse(null);
            if (program == null) {
                System.out.println("Couldn't find program for school: " + school);
                continue;
            }
            topPrograms.add(program);
        }
        System.out.println("Top programs: " + topPrograms.size());
        File out = new File("top_programs.json");
        if (out.exists())
            out.delete();
        out.createNewFile();
        JsonObject topObject = new JsonObject();
        topObject.add("programs", gson.toJsonTree(topPrograms));
        topObject.add("allProgramsData", object);
        String newJson = gson.toJson(topObject);
        Files.write(out.toPath(), newJson.getBytes());
    }
    public static String capatalizeFirst(String in) {
        return in.substring(0, 1).toUpperCase() + in.substring(1).toLowerCase();
    }
    public static String capatalizeFirstDeep(String in) {
        StringBuilder sb = new StringBuilder();
        for (String s : in.split(" ")) {
            sb.append(capatalizeFirst(s)).append(" ");
        }
        return sb.toString().trim();
    }
}
