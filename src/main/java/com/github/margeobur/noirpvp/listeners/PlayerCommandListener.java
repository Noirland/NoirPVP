package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.tools.DelayedMessager;
import com.github.margeobur.noirpvp.trials.TrialManager;
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
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId());
        if(playerPVP.isJailed() || playerPVP.equals(TrialManager.getInstance().currentDefendant())) {
            event.getPlayer().sendMessage("You may not use commands while jailed or on trial.");
            event.setCancelled(true);
        }
        if(command.equalsIgnoreCase("/back")) {
            PVPPlayer playerInfo = PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId());

            if(!event.getPlayer().isOp()) {
                if (playerInfo != null && !playerInfo.canBack()) {
                    event.getPlayer().sendMessage("You cannot use /back unless you have just died. You may " +
                            "not use /back after dying in PVP twice in a short period of time.");
                    event.setCancelled(true);
                }
            }
        }
//        } else if(command.equalsIgnoreCase("/kick") || command.equalsIgnoreCase("/ban")) {
//            Command c = Bukkit.getServer().getPluginCommand(command.substring(1));
//            NoirPVPPlugin.getInstance().getCommand("jail").getExecutor().onCommand(event.getPlayer(), c,
//                    command.substring(1), new String[0]);
//            event.setCancelled(true);
//        }
    }
}