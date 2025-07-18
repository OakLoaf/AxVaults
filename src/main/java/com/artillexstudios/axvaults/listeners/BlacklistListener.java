package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.utils.IntRange;
import com.artillexstudios.axvaults.utils.ItemTransaction;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;
import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class BlacklistListener implements Listener {

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (CONFIG.getSection("blacklisted-items") == null) return;
        boolean isVault = false;
        for (Vault vault : VaultManager.getVaults()) {
            if (vault.getStorage().equals(event.getView().getTopInventory())) {
                isVault = true;
                break;
            }
        }
        if (!isVault) return;

        final Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("axvaults.admin.bypass.blacklist")) {
            return;
        }

        final ItemTransaction transaction = switch (event.getClick()) {
            case ClickType.NUMBER_KEY -> new ItemTransaction(
                player.getInventory().getItem(event.getHotbarButton()),
                event.getCurrentItem()
            );
            case ClickType.SWAP_OFFHAND -> new ItemTransaction(
                player.getInventory().getItemInOffHand(),
                event.getCurrentItem()
            );
            default -> new ItemTransaction(event.getCurrentItem(), null);
        };

        if (transaction.isEmpty()) return;
        if (isTransactionBlacklisted(transaction)) {
            event.setCancelled(true);
            MESSAGEUTILS.sendLang(player, "banned-item");
        }
    }

    private boolean isTransactionBlacklisted(ItemTransaction transaction) {
        return isItemBlacklisted(transaction.incoming()) || isItemBlacklisted(transaction.outgoing());
    }

    private boolean isItemBlacklisted(ItemStack item) {
        if (item == null) return false;
        for (String s : CONFIG.getSection("blacklisted-items").getRoutesAsStrings(false)) {
            if (CONFIG.getString("blacklisted-items." + s + ".material") != null
                && !item.getType().toString().equalsIgnoreCase(CONFIG.getString("blacklisted-items." + s + ".material"))
            ) {
                continue;
            }

            if (CONFIG.getString("blacklisted-items." + s + ".custom-model-data") != null
                && (item.getItemMeta() == null
                || !item.getItemMeta().hasCustomModelData()
                || !IntRange.valueOf(CONFIG.get("blacklisted-items." + s + ".custom-model-data")).contains(item.getItemMeta().getCustomModelData()))
            ) {
                continue;
            }

            if (CONFIG.getString("blacklisted-items." + s + ".name-contains") != null
                && (item.getItemMeta() == null
                || !item.getItemMeta().getDisplayName().contains(CONFIG.getString("blacklisted-items." + s + ".name-contains")))
            ) {
                continue;
            }

            return true;
        }

        return false;
    }

//    private boolean isItemBlacklisted(ItemStack item) {
//        if (item == null) return false;
//        for (String s : CONFIG.getSection("blacklisted-items").getRoutesAsStrings(false)) {
//            if (CONFIG.getString("blacklisted-items." + s + ".material") != null
//                && !item.getType().toString().equalsIgnoreCase(CONFIG.getString("blacklisted-items." + s + ".material"))) {
//                continue;
//            }
//
//            if (CONFIG.getString("blacklisted-items." + s + ".custom-model-data") != null) {
//                if (item.getItemMeta() == null
//                    || !item.getItemMeta().hasCustomModelData()
//                    || !IntRange.valueOf(CONFIG.get("blacklisted-items." + s + ".custom-model-data")).contains(item.getItemMeta().getCustomModelData())
//                ) {
//                    continue;
//                }
//            }
//
//            if (CONFIG.getString("blacklisted-items." + s + ".name-contains") != null) {
//                if (item.getItemMeta() == null) continue;
//                if (!item.getItemMeta().getDisplayName().contains(CONFIG.getString("blacklisted-items." + s + ".name-contains"))) continue;
//            }
//
//            return true;
//        }
//
//        return false;
//    }
}
