package dev.badbird.scraper.util;

public class FileUtils {
    public static String escape(String in) {
        return in.replace("/", "_").replace("\\", "_").replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_").replace(" ", "_").replace("!", "_");
    }
}
