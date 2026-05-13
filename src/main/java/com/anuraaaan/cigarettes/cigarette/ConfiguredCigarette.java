package com.anuraaaan.cigarettes.cigarette;

import com.anuraaaan.cigarettes.Cigarettes;
import com.destroystokyo.paper.ParticleBuilder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

@SuppressWarnings("unused")
@Getter
public final class ConfiguredCigarette {
    private static final Cigarettes plugin = Cigarettes.getPlugin();
    // enum со всеми допустимым партиклами
    private static final Set<Particle> ALLOWED_PARTICLES = EnumSet.of(
            Particle.SMOKE,
            Particle.LARGE_SMOKE,
            Particle.WHITE_SMOKE,
            Particle.CAMPFIRE_SIGNAL_SMOKE,
            Particle.CAMPFIRE_COSY_SMOKE,
            Particle.ASH,
            Particle.CLOUD,
            Particle.FLAME,
            Particle.SMALL_FLAME,
            Particle.DUST
    );
    // поля описывающие предмет
    private final ItemStack itemStack;
    private final TreeMap<Integer, String> itemModels;
    private final boolean requiresIgnition;
    private final long burnDuration;
    private final long burnTimePerPuff;
    private final List<String> effects;
    private final List<String> particles;
    private final Map<String, String> sounds;

    private List<PotionEffect> effectCache;
    private List<ParsedParticle> particleCache;
    // конструктор для заполнения полей
    public ConfiguredCigarette(ItemStack itemStack, TreeMap<Integer, String> itemModels, boolean requiresIgnition,
                               long burnDuration, long burnTimePerPuff, List<String> effects, List<String> particles, Map<String, String> sounds) {

        this.itemStack = itemStack;
        this.itemModels = itemModels;
        this.requiresIgnition = requiresIgnition;
        this.burnDuration = burnDuration;
        this.burnTimePerPuff = burnTimePerPuff;
        this.effects = effects;
        this.particles = particles;
        this.sounds = sounds;
    }
    // получает эффекты
    public List<PotionEffect> getEffects() {
        // если кеш не пустой, то возвращает кэш без парсинга
        if (effectCache != null) return effectCache;
        // создаётся лист для запарсенных эффектов
        List<PotionEffect> temp = new ArrayList<>();
        for (String effect : effects) { // происходит цикл для парсинга эффектов по одиночку
            PotionEffect potionEffect = parseEffect(effect); // парсится эффект в другом методе
            // если эффект пустой, то пропускается
            if (potionEffect == null) continue;
            // добавляется корректный эффект в лист
            temp.add(potionEffect);
        }
        // возвращает кеш эффектов с установленными эффектами
        return effectCache = temp;
    }
    // парсится эффект
    private PotionEffect parseEffect(@Nullable String effect) {
        // если эффект пустой, то возвращает null
        if (effect == null || effect.isEmpty() || effect.equalsIgnoreCase("null")) return null;
        // создаётся массив строк с разделёнными значениями эффекта по запятой
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(effect), ',');
        // если длина равна 0, то убираются пробелы
        if(split.length == 0) split = StringUtils.split(effect, ' ');
        // если равен 0, то возвращается null
        if (split.length == 0) return null;
        // создаётся тип эффекта с типом из конфига
        PotionEffectType type = PotionEffectType.getByName(split[0]);
        // если тип пуст, то возвращается null
        if (type == null) return null;
        // создаются переменные времени продолжения и уровня с начальными значениями
        int duration = 400; // 20 секунд
        int amplifier = 0;

        try {
            // устанавливаются значения переменным из конфига
            if (split.length > 1) duration = Integer.parseInt(split[1]) * 20;
            if (split.length > 2) amplifier = Integer.parseInt(split[2]);

        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Указано не число. Проверьте весь конфиг");
        }
        // возвращает эффект
        return new PotionEffect(type, duration, amplifier);
    }
    // получает партикл для парсинга
    public List<ParsedParticle> getParsedParticles() {
        // проверка на есть ли кеш партикла
        if (particleCache != null) return particleCache;
        // создаётся лист для запарсенных партиклов
        List<ParsedParticle> temp = new ArrayList<>();

        for (String particle : particles) { // происходит цикл для парсинга партиклов по одиночку
            ParsedParticle parsed = parseParticle(particle); // парсится партикл в другом методе
            // если партикл не пустой, то устанавливается
            if (parsed != null) temp.add(parsed);
        }
        // возвращает кеш партиклов с установленными эффектами
        return particleCache = temp;
    }

    // метод на спавн партиклов возе игрока
    public void spawnParticles(Location eyeLocation) {
        Vector direction = eyeLocation.getDirection();

        for (ParsedParticle particle : getParsedParticles()) { // цикл по спавну партиклов по одночке
            Location location = eyeLocation.clone().add(
                    direction.multiply(particle.particleDistance())).add(0, particle.addY(), 0);
            // устанавливается партиклу локация и спавнится
            particle.builder().location(location).spawn();
        }
    }
    // парсятся партиклы
    private ParsedParticle parseParticle(@Nullable String particle) {
        // если партикл пустой, то возвращает null
        if (particle == null || particle.isEmpty() || particle.equalsIgnoreCase("null")) return null;
        // создаётся массив строк с разделёнными значениями партикла по запятой
        String[] split = StringUtils.split(StringUtils.deleteWhitespace(particle),',');
        // если длина равна 0, то убираются пробелы
        if (split.length == 0) split = StringUtils.split(particle, ' ');
        // если равен 0, то возвращается null
        if (split.length == 0) return null;
        // создаётся тип партикла с типом из конфига
        Particle particleType;
        try {
            particleType = Particle.valueOf(split[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверно указана частица" + particle);
            return null;
        }
        // если партикл не из списка, то выжаётся ошибка и возвращается null
        if (!ALLOWED_PARTICLES.contains(particleType)) {
            plugin.getLogger().warning("Частица не разрешена" + particle);
            return null;
        }
        // создаются переменные с добавлением координат, кол-вом, с координатами разброса и скоростью
        double particleDistance = 0.5;
        double addY = -0.25;

        int count = 4;
        double offsetX = 0;
        double offsetY = 0;
        double offsetZ = 0;
        double extra = 0.4;

        try {
            // устанавливаются значения из конфига
            if (split.length > 1) particleDistance = Double.parseDouble(split[1]);
            if (split.length > 2) addY = Double.parseDouble(split[2]);

            if (split.length > 3) count = Integer.parseInt(split[3]);
            if (split.length > 4) offsetX = Double.parseDouble(split[4]);
            if (split.length > 5) offsetY = Double.parseDouble(split[5]);
            if (split.length > 6) offsetZ = Double.parseDouble(split[6]);
            if (split.length > 7) extra = Double.parseDouble(split[7]);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Ошибка числа в particle: " + particle);
        }

        // собирается изначальная версия particleBuilder
        ParticleBuilder builder = new ParticleBuilder(particleType)
                .count(count)
                .offset(offsetX,offsetY,offsetZ)
                .extra(extra);
        // если партикл типа dust, то ему добавляются ещё параметры цвета и размера
        if (particleType == Particle.DUST) {
            int r = 166;
            int g = 166;
            int b = 166;

            float size = 2;
            try {
                if (split.length > 8) r = Integer.parseInt(split[8]);
                if (split.length > 9) g = Integer.parseInt(split[9]);
                if (split.length > 10) b = Integer.parseInt(split[10]);
                if (split.length > 11) size = Float.parseFloat(split[11]);

            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Ошибка числа в particle: " + particle);
            }
            try {
                builder.data(new Particle.DustOptions(Color.fromRGB(r, g, b), size));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректный RGB цвет: " + particle);
            }
        }
        // возвращает парсеный партикл
        return new ParsedParticle(
                builder,
                particleDistance,
                addY
        );
    }

}
