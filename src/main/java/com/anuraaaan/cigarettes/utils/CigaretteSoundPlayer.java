package com.anuraaaan.cigarettes.utils;


import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.cigarette.ConfiguredCigarette;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.SoundCategory;

public class CigaretteSoundPlayer {

    private static final Cigarettes plugin = Cigarettes.getPlugin();

    // проигрывает звук
    public static void playSound(ConfiguredCigarette configuredCigarette, Location location, String key) {
        // получает звук по ключу
        String sound = configuredCigarette.getSounds().get(key);
        // проверяет есть ли звук
        if (sound == null || sound.isEmpty() || sound.equalsIgnoreCase("null")) return;
        // создаётся массив строк с разделёнными значениями звука по запятой
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(sound),',');
        // если длина равна 0, то убираются пробелы
        if (split.length == 0) split = StringUtils.split(sound, ' ');
        // если равен 0, то возвращается null
        if (split.length == 0) return;
        // создаётся строка со звуком из конфига
        String soundType = split[0];
        // проверяется наличие звука в строке
        if (soundType == null) return;
        // создаются переменные громкости и питча с начальными значениями
        float volume = 1f;
        float pitch = 1f;

        try {
            // устанавливаются значения из конфига
            if (split.length > 1) volume = Float.parseFloat(split[1]);
            if (split.length > 2) pitch = Float.parseFloat(split[2]);
        } catch (NumberFormatException e) {
        plugin.getLogger().warning("Указано не число. Проверьте весь конфиг");
    }
        // проигрывается звук
        location.getWorld().playSound(
                location,
                soundType,
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
    }
}
