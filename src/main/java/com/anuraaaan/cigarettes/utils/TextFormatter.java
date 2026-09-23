package com.anuraaaan.cigarettes.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Locale;

public final class TextFormatter {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private TextFormatter() {
    }

    public static Component colorize(String text) {
        return text == null ? Component.empty() : SERIALIZER.deserialize(text);
    }

    public static List<Component> colorize(List<String> lines) {
        if (lines == null) {
            return null;
        }

        return lines.stream().map(TextFormatter::colorize).toList();
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
