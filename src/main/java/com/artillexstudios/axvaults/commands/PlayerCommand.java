package com.artillexstudios.axvaults.commands;

import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class PlayerCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot send this command");
            return true;
        }

        Integer number = args.length < 1 ? null : Integer.parseInt(args[0]);

        if (number == null) {
            if (!sender.hasPermission("axvaults.selector")) {
                MESSAGEUTILS.sendLang(sender, "no-permission");
                return true;
            }
            new VaultSelector().open(player);
            return true;
        }

        if (number < 1) return  true;

        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%num%", "" + number);

        if (!sender.hasPermission("axvaults.openremote")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return true;
        }

        VaultManager.getVaultOfPlayer(player, number, vault -> {
            if (vault == null) {
                MESSAGEUTILS.sendLang(sender, "vault.not-unlocked", replacements);
                return;
            }

            vault.open(player);
            MESSAGEUTILS.sendLang(sender, "vault.opened", replacements);
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Collections.singletonList("[number]") : Collections.emptyList();
    }
}
