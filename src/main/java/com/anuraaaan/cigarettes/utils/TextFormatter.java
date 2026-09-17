package com.anuraaaan.cigarettes.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFormatter {

    public static String colorize(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("&#([a-fA-F0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder builder = new StringBuilder("§x");
            for (char c : hex.toCharArray()) builder.append('§').append(c);
            text = text.replace("&#" + hex, builder.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorize(List<String> lines) {
        if (lines == null) return null;
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    public static String setPlaceholdersString(String text, String... placeholders) {
        if (text == null) return null;

        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("The number of placeholder arguments must be even");
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            text = text.replace(placeholders[i], placeholders[i + 1]);
        }

        return text;
    }

    public static String formatDouble(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
