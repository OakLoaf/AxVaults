package com.artillexstudios.axvaults.commands;

import com.artillexstudios.axapi.nms.NMSHandlers;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axvaults.AxVaults;
import com.artillexstudios.axvaults.converters.PlayerVaultsXConverter;
import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.schedulers.AutoSaveScheduler;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.artillexstudios.axvaults.AxVaults.*;
import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("axvaults.admin")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        switch (args[0]) {
            case "help" -> help(sender);
            case "reload" -> reload(sender);
            case "forceopen" -> {
                if (args.length < 2) {
                    MESSAGEUTILS.sendFormatted(sender, MESSAGES.getString("commands.missing-argument")
                        .replace("%value%", "/cosmeticboxadmin forceopen <player> [number]"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                Integer number = args.length >= 3 ? Integer.parseInt(args[2]) : null;

                forceOpen(sender, target, number);
            }
            case "view" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console cannot send this command");
                    return true;
                }

                if (args.length < 2) {
                    MESSAGEUTILS.sendFormatted(sender, MESSAGES.getString("commands.missing-argument")
                        .replace("%value%", "/cosmeticboxadmin view <player> [number]"));
                    return true;
                }

                OfflinePlayer target = getOfflinePlayer(player, args[1]);
                if (target == null) {
                    MESSAGEUTILS.sendFormatted(sender, MESSAGES.getString("commands.invalid-player")
                        .replace("%player%", args[1]));
                    return true;
                }

                Integer number = args.length >= 3 ? Integer.parseInt(args[2]) : null;

                view(player, target, number);
            }
            case "delete" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console cannot send this command");
                    return true;
                }

                if (args.length < 3) {
                    MESSAGEUTILS.sendFormatted(sender, MESSAGES.getString("commands.missing-argument")
                        .replace("%value%", "/cosmeticboxadmin delete <player> <number>"));
                    return true;
                }

                OfflinePlayer target = getOfflinePlayer(player, args[1]);
                if (target == null) {
                    MESSAGEUTILS.sendFormatted(sender, MESSAGES.getString("commands.invalid-player")
                        .replace("%player%", args[1]));
                    return true;
                }

                int number = Integer.parseInt(args[2]);

                delete(player, target, number);
            }
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console cannot send this command");
                    return true;
                }

                Integer number = args.length >= 2 ? Integer.parseInt(args[1]) : null;

                set(player, number);
            }
            case "stats" -> stats(sender);
            case "converter" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console cannot send this command");
                    return true;
                }

                converter(player);
            }
            case "save" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Console cannot send this command");
                    return true;
                }

                save(player);
            }
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (args.length) {
            case 1 -> {
                yield List.of();
            }
            default -> List.of();
        };
    }

    private void help(@NotNull CommandSender sender) {
        for (String m : MESSAGES.getStringList("help")) {
            sender.sendMessage(StringUtils.formatToString(m));
        }
    }

    public void reload(@NotNull CommandSender sender) {
        if (sender.hasPermission("axvaults.admin.reload")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00[AxVaults] &#AAFFAAReloading configuration..."));
        if (!CONFIG.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "config.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00╠ &#00FF00Reloaded &fconfig.yml&#00FF00!"));

        if (!MESSAGES.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Collections.singletonMap("%file%", "messages.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00╠ &#00FF00Reloaded &fmessages.yml&#00FF00!"));

        VaultManager.reload();
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00╠ &#00FF00Reloaded &fvaults&#00FF00!"));

        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#55FF00╚ &#00FF00Successful reload!"));
        MESSAGEUTILS.sendLang(sender, "reload.success");
    }

    public void forceOpen(@NotNull CommandSender sender, @NotNull Player player, Integer number) {
        if (sender.hasPermission("axvaults.admin.forceopen")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        if (number != null) {
            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%num%", "" + number);

            VaultManager.getVaultOfPlayer(player, number, vault -> {
                if (vault == null) {
                    MESSAGEUTILS.sendLang(player, "vault.not-unlocked", replacements);
                    return;
                }

                vault.open(player);
                MESSAGEUTILS.sendLang(player, "vault.opened", replacements);
            });
            replacements.put("%player%", player.getName());
            MESSAGEUTILS.sendLang(sender, "force-open-vault", replacements);
            return;
        }
        new VaultSelector().open(player);
        MESSAGEUTILS.sendLang(sender, "force-open", Collections.singletonMap("%player%", player.getName()));
    }

    public void view(@NotNull Player sender, @NotNull OfflinePlayer player, Integer number) {
        if (sender.hasPermission("axvaults.admin.view")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%player%", player.getName());

        if (number == null) {
            VaultManager.getPlayer(player.getUniqueId(), vaultPlayer -> {
                replacements.put("%vaults%", vaultPlayer.getVaultMap().values().stream().filter(vault -> vault.getSlotsFilled() != 0).map(vault -> "" + vault.getId()).collect(Collectors.joining(", ")));
                MESSAGEUTILS.sendLang(sender, "view.info", replacements);
            });
            return;
        }

        replacements.put("%num%", "" + number);

        VaultManager.getPlayer(player.getUniqueId(), vaultPlayer -> {
            final Vault vault = vaultPlayer.getVault(number);
            if (vault == null) {
                MESSAGEUTILS.sendLang(sender, "view.not-found", replacements);
                return;
            }
            vault.open(sender);
            MESSAGEUTILS.sendLang(sender, "view.open", replacements);
        });
    }

    public void delete(@NotNull Player sender, @NotNull OfflinePlayer player, int number) {
        if (sender.hasPermission("axvaults.admin.delete")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%player%", player.getName());
        replacements.put("%num%", "" + number);

        VaultManager.getPlayer(player.getUniqueId(), vaultPlayer -> {
            final Vault vault = vaultPlayer.getVault(number);
            if (vault == null) {
                MESSAGEUTILS.sendLang(sender, "view.not-found", replacements);
                return;
            }
            VaultManager.getVaults().remove(vault);
            VaultManager.removeVault(vault);
            AxVaults.getDatabase().deleteVault(player.getUniqueId(), number);
            MESSAGEUTILS.sendLang(sender, "delete", replacements);
        });
    }

    public void set(@NotNull Player sender, Integer number) {
        if (sender.hasPermission("axvaults.admin.set")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        final Block block = sender.getTargetBlockExact(5);

        if (block == null) {
            MESSAGEUTILS.sendLang(sender, "set.no-block");
            return;
        }

        AxVaults.getThreadedQueue().submit(() -> {
            if (AxVaults.getDatabase().isVault(block.getLocation())) {
                MESSAGEUTILS.sendLang(sender, "set.already");
                return;
            }

            AxVaults.getDatabase().setVault(block.getLocation(), number);
            MESSAGEUTILS.sendLang(sender, "set.success");
        });
    }

    public void stats(@NotNull CommandSender sender) {
        if (sender.hasPermission("axvaults.admin.stats")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%players%", String.valueOf(VaultManager.getPlayers().size()));
        replacements.put("%vaults%", String.valueOf(VaultManager.getVaults().size()));
        // alternative vault count (%vaults% and %vaults2% should be the same - if not, something is broken)
        int vaults2 = VaultManager.getPlayers().values().stream().mapToInt(value -> value.getVaultMap().size()).sum();
        replacements.put("%vaults2%", "" + vaults2);
        long lastSave = AutoSaveScheduler.getLastSaveLength();
        replacements.put("%auto-save%", lastSave == -1 ? "---" : "" + lastSave);
        List<String> statsMessage = MESSAGES.getStringList("stats");

        for (String s : statsMessage) {
            MESSAGEUTILS.sendFormatted(sender, s, replacements);
        }
    }

    public void converter(@NotNull Player sender) {
        if (sender.hasPermission("axvaults.admin.converter")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        new PlayerVaultsXConverter().run();
        MESSAGEUTILS.sendLang(sender, "converter.started");
    }

    private void save(@NotNull Player sender) {
        if (sender.hasPermission("axvaults.admin.save")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        long time = System.currentTimeMillis();
        AxVaults.getThreadedQueue().submit(() -> {
            CompletableFuture<Void>[] futures = new CompletableFuture[VaultManager.getVaults().size()];
            int i = 0;
            for (Vault vault : VaultManager.getVaults()) {
                futures[i] = AxVaults.getDatabase().saveVault(vault);
                i++;
            }
            CompletableFuture.allOf(futures).thenRun(() -> {
                MESSAGEUTILS.sendLang(sender, "save.manual", Map.of("%time%", "" + (System.currentTimeMillis() - time)));
            });
        });
    }

    private static OfflinePlayer getOfflinePlayer(Player sender, String value) {
        if (value.equalsIgnoreCase("self") || value.equalsIgnoreCase("me")) return sender;
        OfflinePlayer player = NMSHandlers.getNmsHandler().getCachedOfflinePlayer(value);
        if (player == null && !(player = Bukkit.getOfflinePlayer(value)).hasPlayedBefore()) return null;
        return player;
    }
}
