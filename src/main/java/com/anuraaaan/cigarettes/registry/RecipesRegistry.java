package com.anuraaaan.cigarettes.registry;

import com.anuraaaan.cigarettes.Cigarettes;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipesRegistry {

    private final Cigarettes plugin;

    public RecipesRegistry(Cigarettes plugin) {
        this.plugin = plugin;

        reload();
    }

    public void reload() {
        loadFromConfig();
    }


    private void loadFromConfig() {
        File recipesFolder = new File(plugin.getDataFolder(), "recipes");
        if (!recipesFolder.exists()) {
            recipesFolder.mkdirs();
        }
        File[] files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            plugin.saveResource("recipes/recipes.yml", false);
            files = recipesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            plugin.getLogger().warning("Recipe files were not found. Copying the default recipes.yml");
        }

        for (File file : files) {

            FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

            ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("recipes");
            if (configurationSection == null) continue;
            for (String path : configurationSection.getKeys(false)) {

                boolean enabled = configurationSection.getBoolean("recipes." + path + ".enabled", true);
                if (!enabled) continue;

                NamespacedKey keyRecipe = new NamespacedKey(plugin, path);

                boolean shaped = fileConfiguration.getBoolean("recipes." + path + ".shaped");

                Map<Character, String> ingredientsShaped = new HashMap<>();
                Map<String, Integer> ingredientsShapeless = new HashMap<>();

                ConfigurationSection ingredientsSection = fileConfiguration.getConfigurationSection("recipes." + path + ".ingredients");
                if (ingredientsSection == null) continue;

                for (String key : ingredientsSection.getKeys(false)) {

                    if (shaped) {
                        String ingredient = ingredientsSection.getString(key);

                        if (key.length() != 1) {
                            plugin.getLogger().warning("Ingredient key must be a single character: " + key);
                            continue;
                        }
                        char keyChar = key.charAt(0);
                        ingredientsShaped.put(keyChar, ingredient);

                    } else {

                        try {
                            int ingredientCount =  ingredientsSection.getInt(key);
                            ingredientsShapeless.put(key, ingredientCount);
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Invalid ingredients key: " + key);
                        }
                    }
                }

                List<String> pattern = List.of();
                if (shaped) {
                    pattern = fileConfiguration.getStringList("recipes." + path + ".pattern");
                }

                String result = fileConfiguration.getString("recipes." + path + ".result");
                if (result == null) continue;

                int count = fileConfiguration.getInt("recipes." + path + ".count");
                if (count <= 0) {
                    count = 1;
                }

                ItemStack resultItemStack = plugin.getItemParser().parseItemNoIA(result);
                if (resultItemStack == null) {
                    plugin.getLogger().warning(
                            "Recipe " + path + " skipped: result item " + result + " is unavailable");
                    continue;
                }
                resultItemStack.setAmount(count);

                if (shaped) {
                    if (pattern.isEmpty() || pattern.size() > 3) {
                        plugin.getLogger().warning("Recipe " + path + " skipped: invalid pattern");
                        continue;
                    }
                    ShapedRecipe shapedRecipe = new ShapedRecipe(keyRecipe, resultItemStack);

                    shapedRecipe.shape(pattern.toArray(new String[0]));
                    boolean valid = true;
                    for (char key : ingredientsShaped.keySet()) {
                        ItemStack ingredientItemStack = plugin.getItemParser().parseItemNoIA(ingredientsShaped.get(key));

                        if (ingredientItemStack == null) {
                            plugin.getLogger().warning("Recipe " + path + " skipped: unknown ingredient "
                                    + ingredientsShaped.get(key));
                            valid = false;
                            break;
                        }

                        shapedRecipe.setIngredient(key, new RecipeChoice.ExactChoice(ingredientItemStack));
                    }
                    if (!valid) continue;
                    Bukkit.removeRecipe(keyRecipe);
                    Bukkit.addRecipe(shapedRecipe);
                } else {
                    ShapelessRecipe shapelessRecipe = new ShapelessRecipe(keyRecipe, resultItemStack);
                    boolean valid = true;
                    for (String ingredient : ingredientsShapeless.keySet()) {
                        ItemStack ingredientItemStack = plugin.getItemParser().parseItemNoIA(ingredient);
                        if (ingredientItemStack == null) {
                            plugin.getLogger().warning("Recipe " + path + " skipped: unknown ingredient "
                                    + ingredient);
                            valid = false;
                            break;
                        }
                        int countIngredient = ingredientsShapeless.get(ingredient);
                        RecipeChoice choice = new RecipeChoice.ExactChoice(ingredientItemStack);
                        for (int i = 0; i < countIngredient; i++) {
                            shapelessRecipe.addIngredient(choice);
                        }
                    }
                    if (!valid) continue;
                    Bukkit.removeRecipe(keyRecipe);
                    Bukkit.addRecipe(shapelessRecipe);
                }
            }
        }
    }


}
