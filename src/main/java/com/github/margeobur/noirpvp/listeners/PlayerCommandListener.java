package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.tools.DelayedMessager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listens for the /back command and intervenes if the player has just been killed for the second time.
 */
public class PlayerCommandListener implements Listener {

    @EventHandler
    public void onCommandUse(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage();
        if(command.equalsIgnoreCase("/back")) {
            PVPPlayer playerInfo = PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId());

            if(playerInfo != null && !playerInfo.canBack()) {
                event.getPlayer().sendMessage("You cannot use /back for "
                        + DelayedMessager.formatTimeString(NoirPVPConfig.DOUBLE_PROTECTION_DURATION) + " after dying twice in PVP.");
                event.setCancelled(true);
            }
        } else if(command.equalsIgnoreCase("/kick") || command.equalsIgnoreCase("/ban")) {
            Command c = Bukkit.getServer().getPluginCommand(command.substring(1));
            NoirPVPPlugin.getPlugin().getCommand("jail").getExecutor().onCommand(event.getPlayer(), c,
                    command.substring(1), new String[0]);
        }

    }
}