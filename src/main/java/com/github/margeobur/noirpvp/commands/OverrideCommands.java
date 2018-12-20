package com.github.margeobur.noirpvp.commands;

import com.github.margeobur.noirpvp.FSDatabase;
import com.github.margeobur.noirpvp.NoirPVPConfig;
import com.github.margeobur.noirpvp.NoirPVPPlugin;
import com.github.margeobur.noirpvp.PVPPlayer;
import com.github.margeobur.noirpvp.tools.TimeTracker;
import com.github.margeobur.noirpvp.tools.TimerCallback;
import com.github.margeobur.noirpvp.trials.JailCell;
import com.github.margeobur.noirpvp.trials.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;

public class OverrideCommands implements CommandExecutor {

    private static final String CONTROL_COMMAND = "noirpvp";
    private TimeTracker timer;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(commandSender instanceof Player && !commandSender.isOp()) {
            commandSender.sendMessage("Must be an Op to use this command");
            return true;
        }
        if(!command.getLabel().equalsIgnoreCase("noirpvp")) {
            return true;
        }

        if(commandSender instanceof  Player &&
                args.length == 1 && args[0].equalsIgnoreCase("itemdata")) {
            commandSender.sendMessage("other commands OK");
            Player thePlayer = (Player) commandSender;
            if(!thePlayer.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                ItemMeta handItemMeta = thePlayer.getInventory().getItemInMainHand().getItemMeta();
                thePlayer.sendMessage("Item data:\n");
                thePlayer.sendMessage("type: " + thePlayer.getInventory().getItemInMainHand().getType());
                thePlayer.sendMessage("Display name: " + handItemMeta.getDisplayName());
                thePlayer.sendMessage("Localised name: " + handItemMeta.getLocalizedName());
                thePlayer.sendMessage("Lore: " + handItemMeta.getLore());
                return true;
            }
        }

        if(args.length < 1) {
            return false;
        }

        if(args[0].equalsIgnoreCase("reload")) {
            if(args.length < 2) {
                return false;
            } else if(args[1].equalsIgnoreCase("config")) {
                NoirPVPConfig.getInstance().reload();
            } else if(args[1].equalsIgnoreCase("database")) {
                FSDatabase.getInstance().reloadDatabase();
                for(Player player: Bukkit.getOnlinePlayers()) {
                    PVPPlayer.removePlayer(player.getUniqueId());
                    PVPPlayer.addIfNotPresent(player.getUniqueId());
                }
            }
        } else if(args[0].equalsIgnoreCase("saveall")) {
            JailCell.saveCells();
            PVPPlayer.saveAllPVPData();
        } else if(args[0].equalsIgnoreCase("timer") && args.length > 1) {
            if(args[1].equalsIgnoreCase("start")) {
                int seconds;
                if(args.length < 3) {
                    seconds = 10;
                } else {
                    seconds = Integer.valueOf(args[2]);
                }
                if(timer == null) {
                    timer = new TimeTracker(NoirPVPPlugin.getInstance());
                    System.out.println(LocalDateTime.now() + ": " + "starting new timer");
                    timer.registerTimer(() ->
                            System.out.println(LocalDateTime.now() + ": " + "ended timer after exactly " +
                                    seconds + " seconds"), seconds, "timer");
                } else {
                    System.out.println(LocalDateTime.now() + ": " + "starting timer");
                    timer.registerTimerFromNow(() ->
                            System.out.println(LocalDateTime.now() + ": " + "ended timer after exactly " +
                                    seconds + " seconds"), seconds, "anothertimer");
                }
            } else if(args[1].equalsIgnoreCase("pause")) {
                System.out.println(LocalDateTime.now() + ": " + "pausing timer");
                timer.pause();
            } else if(args[1].equalsIgnoreCase("resume")) {
                System.out.println(LocalDateTime.now() + ": " + "resuming timer");
                timer.resume();
            }
        } else {
            return false;
        }
        return true;
    }
}
