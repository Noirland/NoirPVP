package com.github.margeobur.noirpvp.trials;

import com.github.margeobur.noirpvp.FSDatabase;
import com.github.margeobur.noirpvp.NoirPVPConfig;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

@SerializableAs("JailCell")
public class JailCell implements ConfigurationSerializable {

    private static List<JailCell> jailCells = new ArrayList<>();   // says whether or not each cell is occupied
    private static Map<UUID, Integer> jailPlayerShortlist = new HashMap<>();

    private Location warp;
    private List<UUID> occupants = new ArrayList<>();
    private UUID singleOccupant;
    private boolean canHouseMany;

    public JailCell(Location cellWarpLocation, boolean canHouseMany) {
        warp = cellWarpLocation;
        this.canHouseMany = canHouseMany;
    }

    public JailCell(Map<String, Object> serialMap) {
        if(serialMap.containsKey("warp")) {
            warp = (Location) serialMap.get("warp");
        }

        if(serialMap.containsKey("canHouseMany") && ((Boolean) serialMap.get("canHouseMany"))) {
            if(serialMap.containsKey("occupants")) {
                List<String> occupantIDStrs = (List<String>) serialMap.get("occupants");
                for (String occIDStr : occupantIDStrs) {
                    occupants.add(UUID.fromString(occIDStr));
                }
            }
        } else {
            if(serialMap.containsKey("singleOccupant")) {
                String uuidStr = (String) serialMap.get("singleOccupant");
                if(uuidStr != null) {
                    singleOccupant = UUID.fromString(uuidStr);
                }
            }
        }
    }

    public static Map<UUID, Integer> getJailShortlist() {
        Map<UUID, Integer> jailPlayerShortlist = new HashMap<>();
        for(JailCell cell: jailCells) {
            if(cell.canHouseMany && cell.singleOccupant != null) {
                jailPlayerShortlist.put(cell.singleOccupant, jailCells.indexOf(cell));
            } else {
                for(UUID occupantID: cell.occupants) {
                    jailPlayerShortlist.put(occupantID, jailCells.indexOf(cell));
                }
            }
        }
        return jailPlayerShortlist;
    }

    public static boolean playerOnShortlist(UUID playerID) {
        Set<UUID> players = jailPlayerShortlist.keySet();
        for(UUID id: players) {
            if(id.equals(playerID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialMap = new HashMap<>();
        serialMap.put("warp", warp);

        serialMap.put("canHouseMany", canHouseMany);
        if(canHouseMany) {
            List<String> occupantIDStrs = new ArrayList<>();
            for(UUID occID: occupants) {
                occupantIDStrs.add(occID.toString());
            }
            serialMap.put("occupants", occupantIDStrs);
        } else {
            if(singleOccupant != null) {
                serialMap.put("singleOccupant", singleOccupant.toString());
            } else {
                serialMap.put("singleOccupant", null);
            }
        }
        return serialMap;
    }

    public static void addNewCell(Location cellWarpLocation) {
        JailCell newCell;
        if(jailCells.isEmpty()) {
            newCell = new JailCell(cellWarpLocation, true);
        } else {
            newCell = new JailCell(cellWarpLocation, false);
        }
        jailCells.add(newCell);
        NoirPVPConfig.getInstance().saveCells(jailCells);
    }

    /**
     * This method gets a cell that is free to be occupied. The cell might not in fact be empty, but if an empty
     * one is available it will be selected
     * @return the {@link Location} to warp a player to when jailing them
     */
    public static Location getVacantCellFor(UUID playerID) {
        if(jailCells.isEmpty()) {
            return null;
        }
//        System.out.println("Adding " + playerID.toString() + " to the shortlist");
        for(JailCell cell: jailCells) {
             if(!cell.canHouseMany && cell.singleOccupant == null) {
                cell.singleOccupant = playerID;
                return cell.warp;
            }
        }
        for(JailCell cell: jailCells) {
            if(cell.canHouseMany && cell.occupants.isEmpty()) {
                cell.occupants.add(playerID);
                return cell.warp;
            }
        }
        jailCells.get(0).occupants.add(playerID);
        return jailCells.get(0).warp;
    }

    /**
     * Searches for the cell that the player is in and deletes them from the cell
     */
    public static void releasePlayer(UUID playerId) {
        for(JailCell cell: jailCells) {
            if(cell.canHouseMany && cell.occupants.contains(playerId)) {
                cell.occupants.remove(playerId);
                return;
            } else if(cell.singleOccupant != null && cell.singleOccupant.equals(playerId)) {
                cell.singleOccupant = null;
                return;
            }
        }
    }

    public static void saveCells() {
        NoirPVPConfig.getInstance().saveCells(jailCells);
    }

    public static void setCells(List<JailCell> cells) {
        jailCells = cells;
    }
}
