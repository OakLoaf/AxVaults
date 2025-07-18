package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.utils.IntRange;
import com.artillexstudios.axvaults.utils.ItemTransaction;
import com.artillexstudios.axvaults.vaults.Vault;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import static com.artillexstudios.axvaults.AxVaults.CONFIG;
import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class WhitelistListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (CONFIG.getSection("whitelisted-items") == null) return;
        boolean isVault = false;
        for (Vault vault : VaultManager.getVaults()) {
            if (vault.getStorage().equals(event.getView().getTopInventory())) {
                isVault = true;
                break;
            }
        }
        if (!isVault) return;

        final Player player = (Player) event.getWhoClicked();
        if (player.hasPermission("axvaults.admin.bypass.whitelist")) {
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
        if (!isTransactionWhitelisted(transaction)) {
            event.setCancelled(true);
            MESSAGEUTILS.sendLang(player, "banned-item");
        }
    }

    private boolean isTransactionWhitelisted(ItemTransaction transaction) {
        return isItemWhitelisted(transaction.incoming()) && isItemWhitelisted(transaction.outgoing());
    }

    private boolean isItemWhitelisted(ItemStack item) {
        if (item == null) return true;
        for (String s : CONFIG.getSection("whitelisted-items").getRoutesAsStrings(false)) {
            if (CONFIG.getString("whitelisted-items." + s + ".material") != null
                && !item.getType().toString().equalsIgnoreCase(CONFIG.getString("whitelisted-items." + s + ".material"))
            ) {
                continue;
            }

            if (CONFIG.getString("whitelisted-items." + s + ".custom-model-data") != null
                && (item.getItemMeta() == null
                || !item.getItemMeta().hasCustomModelData()
                || !IntRange.valueOf(CONFIG.get("whitelisted-items." + s + ".custom-model-data")).contains(item.getItemMeta().getCustomModelData()))
            ) {
                continue;
            }

            return true;
        }

        return false;
    }
}
