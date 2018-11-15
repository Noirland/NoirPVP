package com.github.margeobur.noirpvp.commands;

import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.JailTrial;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private static final String JAIL_COMMAND = "njail";
    private static final String UNJAIL_COMMAND = "nunjail";
    private static final String BAN_COMMAND = "nban";
    private static final String KICK_COMMAND = "nkick";
    private static final String SET_DOCK_COMMAND = "setdock";
    private static final String SET_RELEASE_COMMAND = "setrelease";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String commandLabel = command.getLabel();

        JailTrial.JailTrialResult vote = JailTrial.JailTrialResult.JAIL;
        switch(commandLabel) {
            case BAN_COMMAND:
                 vote = JailTrial.JailTrialResult.BAN;
            case KICK_COMMAND:
                if(commandLabel.equalsIgnoreCase(KICK_COMMAND)) {
                    vote = JailTrial.JailTrialResult.KICK;
                }
            case JAIL_COMMAND:
                if(args.length == 0) {
                    if(!(sender instanceof Player)) {
                        sender.sendMessage("You must be a player to vote");
                    }
                    if(!sender.hasPermission("noirpvp.vote")) {
                        sender.sendMessage("You do not have permission to vote in trials.");
                    }

                    Player player = (Player) sender;

                    TrialManager.VoteResult result = TrialManager.getInstance().addVoteToJailTrial(player.getUniqueId(),
                            vote);
                    switch (result) {
                        case ALREADY_VOTED:
                            player.sendMessage(ChatColor.RED + "You have already voted in this trial");
                            break;
                        case NO_TRIAL:
                            player.sendMessage(ChatColor.RED + "There is not currently a trial in progress.");
                            break;
                        case NOT_ALLOWED:
                            player.sendMessage(ChatColor.RED + "You are barred from voting in the current trial.");
                            break;
                        case SUCCESS:
                            player.sendMessage(ChatColor.GOLD + "Your vote has been accepted and recorded.");
                            break;
                        case WRONG_TYPE:
                            player.sendMessage(ChatColor.RED + "The current trial is not an admin-initiated trial");
                            break;
                    }
                    return true;
                } else if(args.length == 1 && args[0].equals("addcell")) {
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
                            !sender.hasPermission("noirpvp.jail")) {
                        sender.sendMessage("You do not have permission to jail players");
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

                    PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(thePlayer.getUniqueId());
                    TrialManager.getInstance().dispatchNewJailTrial(playerPVP, reasonSB.toString());
                    return true;
                }
                break;
            case UNJAIL_COMMAND:
                if(sender instanceof Player) {
                    if(!sender.hasPermission("noirpvp.jail")) {
                        sender.sendMessage("You do not have permission to unjail players");
                        return true;
                    }

                    Player thePlayer = Bukkit.getPlayer(args[0]);
                    if(thePlayer == null) {
                        sender.sendMessage("No such player found");
                    }

                    PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(thePlayer.getUniqueId());
                    TrialManager.getInstance().unjailPlayer(playerPVP);
                    return true;
                }
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
