package com.github.margeobur.noirpvp.listeners;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

public class PlayerCraftListener implements Listener {

    /**
     * This method listens for players crafing Snow armour or Desert armour. It check the durability
     * of the leather chestplate they're using and sets the resulting item's durability to that.
     */
    @EventHandler
    public void onPlayerPrepareCraft(PrepareItemCraftEvent event) {
        if(event.getRecipe() instanceof ShapedRecipe) {
            ShapedRecipe recipe = (ShapedRecipe) event.getRecipe();
            if(recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "snowBoots")) ||
            recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "snowLeggings")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "snowChestpiece")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "snowHat")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "desertBoots")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "desertLeggings")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "desertChestpiece")) ||
                    recipe.getKey().equals(new NamespacedKey(NoirPVPPlugin.getInstance(), "desertHat"))) {
                ItemStack[] craftingItems = event.getInventory().getMatrix();
                ItemStack resultingArmour = event.getInventory().getResult();

                if(craftingItems.length < 5) {
                    return;
                }

                for (ItemStack item : craftingItems) {
                    if (item.getType().equals(Material.LEATHER_BOOTS)
                            || item.getType().equals(Material.LEATHER_LEGGINGS)
                            || item.getType().equals(Material.LEATHER_CHESTPLATE)
                            || item.getType().equals(Material.LEATHER_HELMET)) {
                        Damageable chestPlateDurability = (Damageable) item.getItemMeta();
                        ItemMeta resultMeta = resultingArmour.getItemMeta();
                        ((Damageable) resultMeta).setDamage(chestPlateDurability.getDamage());
                        resultingArmour.setItemMeta(resultMeta);
                    }
                }
            }
        }
    }
}
