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
        if(event.getPlayer().isOp()) {
            return;
        }
        PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId());
        if(playerPVP.isJailed() || playerPVP.equals(TrialManager.getInstance().currentDefendant())) {
            event.getPlayer().sendMessage("You may not use commands while jailed or on trial.");
            event.setCancelled(true);
        }
    }
}