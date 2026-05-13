package com.anuraaaan.cigarettes.cigarette;

import com.anuraaaan.cigarettes.Cigarettes;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CigaretteRegistry {
    // получает плагин
    private static final Cigarettes plugin = Cigarettes.getPlugin();
    // аннотация, которая создаёт геттер
    @Getter
    // создаётся мапа с обычными сигаретами
    private static final Map<String, ItemStack> unlitCigarettes = new HashMap<>();
    @Getter
    // создаётся мапа с подожжёнными сигаретами
    private static final Map<String, ConfiguredCigarette> litCigarettes = new HashMap<>();
    // поле для файла
    private File file;
    // поле для конфигурации
    private FileConfiguration configuration;
    // обозначение ключа для pdc сигареты с id сигареты
    private static final NamespacedKey KEY_ID = new NamespacedKey(plugin, "cigarette_id");

    // при загрузке плагина вызывается метод load()
    public CigaretteRegistry() {
        load();
    }
    // метод, который загружает конфигурацию при загрузке плагина
    private void load() {
        // получает путь до файла конфигурации и название файла
        file = new File(plugin.getDataFolder(), "cigarettes.yml");
        // логирует путь получение пути до конфига и существует ли файл
        plugin.getLogger().info("Путь к конфигу: " + file.getAbsolutePath());
        plugin.getLogger().info("Файл существует: " + file.exists());
        // если файл не создан, то создаёт без замены
        if (!file.exists()) {
            plugin.getLogger().warning("Конфигурация не найдена, копирую версию по умолчанию...");
            plugin.saveResource("cigarettes.yml", false);
        }
        // применяется к полю конфиг конфигурация yaml
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }
    // обновляет мапы с предметами
    private void update() {
        // очищает мапы
        unlitCigarettes.clear();
        litCigarettes.clear();
        // получает путь до места, где хранятся все сигареты
        ConfigurationSection section = configuration.getConfigurationSection("cigarettes");
        // если не находит, то ничего
        if (section == null) return;
        // цикл по заполнению мап
        for (String path : section.getKeys(false)) {
            // задаёт имя и лор
            String name = configuration.getString("cigarettes." + path + ".name");
            List<String> lore = configuration.getStringList("cigarettes." + path + ".lore");

            // получение базового предмета и проверка на соответствие материалам из ваниллы
            String baseItem = configuration.getString("cigarettes." + path + ".base_item", "stick").toUpperCase();
            Material material;
            // проверка на правильность заполнения поля для предмета
            try {
                material = Material.valueOf(baseItem);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Не существует такого предмета " + baseItem);
                continue;
            }
            // берётся item model для незажженной сигареты
            String itemModelStart = configuration.getString("cigarettes." + path + ".item_model_start");

            // берётся items models для зажжённой сигареты и создаётся мапа с: процент - модель предмета
            TreeMap<Integer, String> itemModels = new TreeMap<>();

            //берётся путь до того блока с моделями
            ConfigurationSection itemSection = configuration.getConfigurationSection("cigarettes." + path + ".item_models");

            // цикл по установке в мапу значений
            if (itemSection == null) continue;
            for (String key : itemSection.getKeys(false)) {

                // захватывается модель
                String model = itemSection.getString(key);

                try {
                    // парсится ключ в число и заносится в мапу
                    int percent = Integer.parseInt(key);
                    itemModels.put(percent, model);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неправильный ключ item_models: " + key);
                }
            }

            // берётся максимальный стак для незажженной сигареты
            int maxStack = configuration.getInt("cigarettes." + path + ".max_stack");

            // нужно ли огниво для поджигания
            boolean requiresIgnition = configuration.getBoolean("cigarettes." + path + ".requires_ignition");

            // берётся время тления и на сколько уменьшается за одну затяжку
            long burnDuration = configuration.getLong("cigarettes." + path + ".burn_duration") * 1000;
            long burnTimePerPuff = configuration.getLong("cigarettes." + path + ".burn_time_per_puff") * 1000;
            // берётся лист эффектов и партиклов
            List<String> potionEffects = configuration.getStringList("cigarettes." + path + ".effects");
            List<String> particles = configuration.getStringList("cigarettes." + path + ".particles");
            // берётся мапа для звуков, где ключ - значение звука
            Map<String, String> sounds = new HashMap<>();

            //берётся путь до блока со звуками
            ConfigurationSection soundSection = configuration.getConfigurationSection("cigarettes." + path + ".sounds");

            // цикл по установке в мапу значений
            if (soundSection == null) continue;
            for (String key : soundSection.getKeys(false)) {

                // захватывается значение звука
                String sound = soundSection.getString(key);

                try {
                    // ключ заносится в мапу
                    sounds.put(key, sound);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неправильный ключ sounds: " + key);
                }
            }



            // создаётся не горящая сигарета:
            ItemStack itemStackCigarettes = new ItemStack(material);

            ItemMeta itemMetaCigarettes = itemStackCigarettes.getItemMeta();
            if (itemMetaCigarettes == null) continue;

            // устанавливается имя предмета
            itemMetaCigarettes.setItemName(name);

            // устанавливается описание предмета
            if (!lore.isEmpty()) {
                itemMetaCigarettes.setLore(lore);
            }
            // устанавливается модель предмета
            if (itemModelStart != null) {
                itemMetaCigarettes.setItemModel(NamespacedKey.fromString(itemModelStart));
            } else {
                plugin.getLogger().warning("Отсутствует конфигурация: cigarettes." + path + ".item_model_start");
            }

            // устанавливается максимальное кол-во стака
            itemMetaCigarettes.setMaxStackSize(maxStack);
            // получает pdc
            PersistentDataContainer pdcCigarettes = itemMetaCigarettes.getPersistentDataContainer();
            // устанавливается id предмета
            pdcCigarettes.set(KEY_ID, PersistentDataType.STRING, path);

            // устанавливается предмету мета
            itemStackCigarettes.setItemMeta(itemMetaCigarettes);
            // записывается в лист
            unlitCigarettes.put(path, itemStackCigarettes);



            // создаётся горящая сигарета:
            ItemStack itemStackLitCigarettes = new ItemStack(material);

            // даётся анимация предмету через поедание
            Consumable consumable = Consumable.consumable()
                    .consumeSeconds(0.4f)
                    .animation(ItemUseAnimation.EAT)
                    .sound(Key.key("q:q")) // ставится звук-заглушка
                    .hasConsumeParticles(false).build();

            // применяется предмету
            itemStackLitCigarettes.setData(DataComponentTypes.CONSUMABLE, consumable);

            // создаётся перезарядка с группой перезарядки
            UseCooldown cooldown = UseCooldown.useCooldown(2.0f).cooldownGroup(Key.key("cigarettes")).build();

            // применяется перезарядка предмету
            itemStackLitCigarettes.setData(DataComponentTypes.USE_COOLDOWN, cooldown);
            // получает мету
            ItemMeta itemMetaLitCigarettes = itemStackLitCigarettes.getItemMeta();

            // устанавливается имя предмета
            itemMetaLitCigarettes.setItemName(name);

            // устанавливается описание предмета
            itemMetaLitCigarettes.setLore(lore);

            // устанавливается максимальное кол-во стака
            itemMetaLitCigarettes.setMaxStackSize(1);
            // получает pdc предмета
            PersistentDataContainer pdcLitCigarettes = itemMetaLitCigarettes.getPersistentDataContainer();
            // устанавливается id сигарете
            pdcLitCigarettes.set(KEY_ID, PersistentDataType.STRING, path);

            // применяется мета к предмету
            itemStackLitCigarettes.setItemMeta(itemMetaLitCigarettes);
            // сохраняется в мапу горящих сигарет
            litCigarettes.put(path, new ConfiguredCigarette(
                    itemStackLitCigarettes, itemModels, requiresIgnition,
                    burnDuration, burnTimePerPuff, potionEffects, particles, sounds));
        }
    }
    // получает id предмета
    public static String getId(ItemStack item) {
        // проверяет есть ли предмет или есть ли мета
        if (item == null || !item.hasItemMeta()) return null;
        // получает мету предмета
        ItemMeta itemMeta = item.getItemMeta();
        // возвращает id предмета
        return itemMeta.getPersistentDataContainer().get(KEY_ID, PersistentDataType.STRING);
    }
}
