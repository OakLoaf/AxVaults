package com.artillexstudios.axvaults.listeners;

import com.artillexstudios.axvaults.guis.VaultSelector;
import com.artillexstudios.axvaults.placed.PlacedVaults;
import com.artillexstudios.axvaults.vaults.VaultManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Range;

import java.util.HashMap;

import static com.artillexstudios.axvaults.AxVaults.MESSAGEUTILS;

public class PlayerInteractListener implements Listener {

    @EventHandler (ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getClickedBlock() == null) return;

        final Player player = event.getPlayer();
        final Location location = event.getClickedBlock().getLocation();

        if (!PlacedVaults.getVaults().containsKey(location)) return;

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        final Integer vault = PlacedVaults.getVaults().get(location);
        open(player, vault, true);
    }

    private void open(@NotNull Player sender, @Optional @Range(min = 1) Integer number, boolean force) {
        if (number == null) {
            if (!force && !sender.hasPermission("axvaults.selector")) {
                MESSAGEUTILS.sendLang(sender, "no-permission");
                return;
            }
            new VaultSelector().open(sender);
            return;
        }

        if (number < 1) return;

        final HashMap<String, String> replacements = new HashMap<>();
        replacements.put("%num%", "" + number);

        if (!force && !sender.hasPermission("axvaults.openremote")) {
            MESSAGEUTILS.sendLang(sender, "no-permission");
            return;
        }

        VaultManager.getVaultOfPlayer(sender, number, vault -> {
            if (vault == null) {
                MESSAGEUTILS.sendLang(sender, "vault.not-unlocked", replacements);
                return;
            }

            vault.open(sender);
            MESSAGEUTILS.sendLang(sender, "vault.opened", replacements);
        });
    }
}
