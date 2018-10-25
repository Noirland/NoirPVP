package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.PVPPlayer;
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
        if(!command.equalsIgnoreCase("/back")) {
            return;
        }

        PVPPlayer playerInfo = PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId());

        if(playerInfo != null && !playerInfo.canBack())
        {
            event.getPlayer().sendMessage("You cannot use /back for 5 minutes after dying twice in PVP.");
            event.setCancelled(true);
        }
    }
}