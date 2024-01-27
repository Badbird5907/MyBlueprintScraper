package dev.badbird.scraper.util;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;


public class BlockResolver {
    @SneakyThrows
    public static void main(String[] args) {
        File file = new File("test.txt");
        String data = new String(Files.readAllBytes(file.toPath()));
        // select the id tabs-Program_Requirements
        Elements table = Jsoup.parse(data).select("#tabs-Program_Requirements > div:nth-child(1) > div.large-6.medium-12.small-12.columns.end > div > div > div > div > div > div:nth-child(3) > table");
        System.out.println(table.text());
    }
}
