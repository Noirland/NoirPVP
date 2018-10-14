package com.github.margeobur.noirpvp.Listeners;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;

public class PlayerServerListener implements Listener {

    public PlayerServerListener() { }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PVPPlayer.addIfNotPresent(event.getPlayer().getUniqueId());
    }
}
