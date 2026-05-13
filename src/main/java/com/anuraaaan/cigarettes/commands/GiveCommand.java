package com.anuraaaan.cigarettes.commands;

import com.anuraaaan.cigarettes.cigarette.CigaretteRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

// простая команда на выдачу предмета обычной сигареты
public class GiveCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        // проверка на игрока
        if (!(commandSender instanceof Player)) return true;
        Player target = (Player) commandSender;

        ItemStack cigaretteType = CigaretteRegistry.getUnlitCigarettes().get(strings[0]);
        // вдя предмет
        target.getInventory().addItem(cigaretteType);

        return true;
    }
}
