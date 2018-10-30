package com.github.margeobur.noirpvp.commands;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * The command handler for admin commands in NoirPVP. This includes
 * <ul>
 *     <li>/jail (and its variants)</li>
 *     <li>/setdock</li>
 * </ul>
 */
public class AdminCommands implements CommandExecutor {

    private static final String JAIL_COMMAND = "jail";
    private static final String SET_DOCK_COMMAND = "setdock";
    private static final String SET_RELEASE_COMMAND = "setrelease";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String commandLabel = command.getLabel();
        if(!sender.hasPermission("noirpvp.jail")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        switch(commandLabel) {
            case JAIL_COMMAND:
                if(args.length == 1 && args[0].equals("addcell")) {
                    if(!(sender instanceof Player)) {
                        sender.sendMessage("You must use this command as a player so that the location can be found");
                    }
                    if(!sender.hasPermission("noirpvp.setlocations")) {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }

                    Player admin = (Player) sender;
                    Location loc = admin.getLocation();
                    NoirPVPConfig.getInstance().addJailCell(loc);
                    return true;
                } else if(args.length > 1) {
                    if((sender instanceof Player) &&
                            !sender.hasPermission("noirpvp.setlocations")) {
                        sender.sendMessage("You do not have permission to use this command.");
                        return true;
                    }

                    Player thePlayer = Bukkit.getPlayer(args[0]);
                    if(thePlayer == null) {
                        sender.sendMessage("No such player found");
                    }

                    StringBuilder reasonSB = new StringBuilder();
                    for(int i = 1; i < args.length; i++) {
                        reasonSB.append(args[i]).append(" ");
                    }
                    Location cell = JailCell.getVacantCellFor(thePlayer.getUniqueId());
                    thePlayer.teleport(cell);
                    return true;
                }
                break;
            case SET_DOCK_COMMAND:
                if(!(sender instanceof Player)) {
                    sender.sendMessage("You must use this command as a player so that the location can be found");
                }
                Player admin = (Player) sender;
                if(!admin.hasPermission("noirpvp.setlocations")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                Location newCourtDock = admin.getLocation();
                NoirPVPConfig.getInstance().setCourtDock(newCourtDock);
                return true;
            case SET_RELEASE_COMMAND:
                if(!(sender instanceof Player)) {
                    sender.sendMessage("You must use this command as a player so that the location can be found");
                }
                admin = (Player) sender;
                if(!admin.hasPermission("noirpvp.setlocations")) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                Location newReleasePoint = admin.getLocation();
                NoirPVPConfig.getInstance().setReleasePoint(newReleasePoint);
                return true;
        }
        return false;
    }
}
