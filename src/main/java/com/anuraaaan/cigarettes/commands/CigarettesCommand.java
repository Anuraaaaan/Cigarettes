package com.anuraaaan.cigarettes.commands;

import com.anuraaaan.cigarettes.Cigarettes;
import com.anuraaaan.cigarettes.managers.NicotineManager;
import com.anuraaaan.cigarettes.nicotine.NicotineData;
import com.anuraaaan.cigarettes.registry.CigaretteRegistry;
import com.anuraaaan.cigarettes.registry.ConfigRegistry;
import com.anuraaaan.cigarettes.registry.CustomItemsRegistry;
import com.anuraaaan.cigarettes.registry.RecipesRegistry;
import com.anuraaaan.cigarettes.utils.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CigarettesCommand implements CommandExecutor, TabCompleter {

    private final Cigarettes plugin;
    private final CigaretteRegistry cigaretteRegistry;
    private final CustomItemsRegistry customItemsRegistry;
    private final RecipesRegistry recipesRegistry;
    private final ConfigRegistry configRegistry;
    private final NicotineManager nicotineManager;

    public CigarettesCommand(Cigarettes plugin, CigaretteRegistry cigaretteRegistry, CustomItemsRegistry customItemsRegistry,
                             RecipesRegistry recipesRegistry, ConfigRegistry configRegistry, NicotineManager nicotineManager) {
        this.plugin = plugin;
        this.cigaretteRegistry = cigaretteRegistry;
        this.customItemsRegistry = customItemsRegistry;
        this.recipesRegistry = recipesRegistry;
        this.configRegistry = configRegistry;
        this.nicotineManager = nicotineManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, org.bukkit.command.@NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {

        if (strings.length == 0) {
            commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("no_arguments")));
            return true;
        }
        switch (strings[0].toLowerCase()) {
            case "give": {
                if (strings.length < 2) return true;
                if (!commandSender.hasPermission("cigarettes.give")) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("no_permission")));
                    return true;
                }
                ItemStack returnItem = plugin.getItemParser().parseItemNoIA(strings[1]);
                if (returnItem == null) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("unknown_item")));
                    return true;
                }
                if (strings.length == 2) {
                    if (!(commandSender instanceof Player sender)) {
                        commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("player_only")));
                        return true;
                    }
                    sender.getInventory().addItem(returnItem);
                    commandSender.sendMessage(TextFormatter.colorize(
                            TextFormatter.setPlaceholdersString(
                                    configRegistry.getLangMap().get("item_given"),
                                    "%amount%", "1",
                                    "%item%", returnItem.getItemMeta().getItemName())));
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(strings[2]);
                    if (amount < 1 || amount > 99) {
                        commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("amount_out_of_range")));
                        return true;
                    }
                    returnItem.setAmount(amount);
                } catch (NumberFormatException e) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("invalid_number")));
                    return true;
                }
                if (strings.length == 3) {
                    if (!(commandSender instanceof Player sender)) {
                        commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("player_only")));
                        return true;
                    }
                    sender.getInventory().addItem(returnItem);
                    commandSender.sendMessage(TextFormatter.colorize(
                            TextFormatter.setPlaceholdersString(
                                    configRegistry.getLangMap().get("item_given"),
                                    "%amount%", Integer.toString(amount),
                                    "%item%", returnItem.getItemMeta().getItemName())));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(strings[3]);
                if (target == null) {
                    commandSender.sendMessage(TextFormatter.colorize(
                            TextFormatter.setPlaceholdersString(
                                    configRegistry.getLangMap().get("player_offline"),
                                    "%player%", strings[3])));
                    return true;
                }
                target.getInventory().addItem(returnItem);

                commandSender.sendMessage(TextFormatter.colorize(
                        TextFormatter.setPlaceholdersString(
                                configRegistry.getLangMap().get("item_given_to_player"),
                                "%amount%", Integer.toString(amount),
                                "%item%", returnItem.getItemMeta().getItemName(),
                                "%player%", target.getName())));
                return true;
            }
            case "reload": {
                if (!commandSender.hasPermission("cigarettes.reload")) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("no_permission")));
                    return true;
                }
                configRegistry.reload();

                cigaretteRegistry.reload();
                customItemsRegistry.reload();
                recipesRegistry.reload();
                commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("config_reloaded")));
                return true;
            }
            case "add": {
                if (!commandSender.hasPermission("cigarettes.add")) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("no_permission")));
                    return true;
                }

                if (!(commandSender instanceof Player sender)) {

                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("player_only")));
                    return true;
                }

                if (strings.length < 2) return true;


                UUID uuid = sender.getUniqueId();

                int nicotine = 0;
                int tolerance = 0;
                int addiction = 0;
                try {
                    nicotine = Integer.parseInt(strings[1]);
                    if (strings.length >= 3) {
                        tolerance = Integer.parseInt(strings[2]);
                    }
                    if (strings.length >= 4) {
                        addiction = Integer.parseInt(strings[3]);
                    }
                } catch (NumberFormatException e) {
                    commandSender.sendMessage(TextFormatter.colorize(configRegistry.getLangMap().get("invalid_number")));
                    return true;
                }

                nicotineManager.addNicotineData(uuid, nicotine, tolerance, addiction);
                NicotineData nicotineData = nicotineManager.getNicotineData(uuid);

                commandSender.sendActionBar(TextFormatter.colorize(
                        TextFormatter.setPlaceholdersString(
                                configRegistry.getLangMap().get("nicotine_status"),
                                "%nicotine%", TextFormatter.formatDouble(nicotineData.getNicotine()),
                                "%tolerance%", TextFormatter.formatDouble(nicotineData.getTolerance()),
                                "%addiction%", TextFormatter.formatDouble(nicotineData.getAddiction()))));

                commandSender.sendMessage(TextFormatter.colorize(
                        TextFormatter.setPlaceholdersString(
                                configRegistry.getLangMap().get("nicotine_added"),
                                "%nicotine%", Integer.toString(nicotine),
                                "%tolerance%", Integer.toString(tolerance),
                                "%addiction%", Integer.toString(addiction))));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> tab = new ArrayList<>();
        if (args.length == 1) {
            tab.add("add");
            tab.add("give");
            tab.add("reload");
        }
        if (args[0].equals("give") && args.length == 2) {
            tab.addAll(cigaretteRegistry.getUnlitCigarettes().keySet());
            tab.addAll(customItemsRegistry.getCustomItems().keySet());
        }
        if (args[0].equals("give") && args.length == 3) {
            tab.add("1");
            tab.add("32");
            tab.add("64");
            tab.add("99");
        }
        if (args[0].equals("give") && args.length == 4) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tab.add(player.getName());
            }
        }
        if (args[0].equals("add") && args.length <= 4) {
            tab.add("-100");
            tab.add("-50");
            tab.add("0");
            tab.add("50");
            tab.add("100");
        }
        return tab;
    }

}
