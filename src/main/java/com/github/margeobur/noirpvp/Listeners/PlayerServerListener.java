package com.github.margeobur.noirpvp.Listeners;

import com.github.margeobur.noirpvp.PVPPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;

public class PlayerServerListener implements Listener {

    private ArrayList<PVPPlayer> _players;

    public PlayerServerListener(ArrayList<PVPPlayer> players) {
        _players = players;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(PVPPlayer.getPlayerByUUID(event.getPlayer().getUniqueId(), _players) == null) {
            _players.add(new PVPPlayer(event.getPlayer().getUniqueId()));
        }
    }
}
