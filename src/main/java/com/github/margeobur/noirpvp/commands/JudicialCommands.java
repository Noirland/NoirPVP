package com.github.margeobur.noirpvp.commands;

import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.trials.TrialManager;
import com.github.margeobur.noirpvp.trials.TrialManager.VoteResult;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * The command handler for crime management commands in NoirPVP. This includes
 * <ul>
 *     <li>/innocent and /guilty,</li>
 *     <li>/crime, and</li>
 * </ul>
 */
public class JudicialCommands implements CommandExecutor {

    private static final String INNOCENT_COMMAND = "innocent";
    private static final String GUILTY_COMMAND = "guilty";
    private static final String CRIME_COMMAND = "crime";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String commandLabel = command.getLabel();
        Player player;
        UUID playerID;
        boolean voteIsGuilty = true;

        switch(commandLabel) {
            case INNOCENT_COMMAND:
                voteIsGuilty = false;
            case GUILTY_COMMAND:
                if(sender instanceof Player) {
                    player = (Player) sender;
                    playerID = player.getUniqueId();
                } else {
                    sender.sendMessage(ChatColor.RED + "Only players may vote in trails!");
                    return true;
                }

                if(!sender.hasPermission("noirpvp.vote")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to vote in trials.");
                    return true;
                }

                VoteResult result = TrialManager.getInstance().addVoteToCurrentTrial(playerID, voteIsGuilty);
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
                }
                return true;
            case CRIME_COMMAND:
                if(args.length == 0) {
                    if(!(sender instanceof Player)) {
                        sender.getServer().dispatchCommand(sender, "crime top");
                    }
                    player = (Player) sender;
                    playerID = player.getUniqueId();

                    PVPPlayer playerPVP = PVPPlayer.getPlayerByUUID(playerID);
                    player.sendMessage("You have " + playerPVP.getCrimeMarks() + " crime marks.");
                    return true;

                } else if(args.length > 0) {
                    if(args[0].equalsIgnoreCase("top")) {
                        StringBuilder message = new StringBuilder();

                        message.append(ChatColor.GOLD);
                        message.append("==============================================================\n");
                        message.append("Top Criminals on Noirland\n");
                        message.append("==============================================================\n");
                        message.append(ChatColor.RESET);

                        int i = 0;
                        List<PVPPlayer> criminals = PVPPlayer.getTopCriminals();
                        for(PVPPlayer criminal: criminals) {
                            message.append(i++).append(": ").append(criminal.getPlayer().getDisplayName())
                            .append(" - ").append(criminal.getCrimeMarks());
                        }

                        sender.sendMessage(message.toString());

                        return true;
                    } else {
                        Player lookupPlayer = Bukkit.getPlayer(args[0]);
                        if(lookupPlayer == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found");
                            return true;
                        }

                        PVPPlayer lookupPlayerPVP = PVPPlayer.getPlayerByUUID(lookupPlayer.getUniqueId());

                        sender.sendMessage("Player " + lookupPlayer.getDisplayName() +
                                " has " + lookupPlayerPVP.getCrimeMarks() + " crime marks.");
                        return true;
                    }
                }
        }

        return false;
    }
}
