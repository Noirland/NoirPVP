package com.github.margeobur.noirpvp.tools;

import com.github.margeobur.noirpvp.NoirPVPPlugin;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class Recipes {

    public static void addRecipes() {
        Plugin thePlugin = NoirPVPPlugin.getInstance();

        addSnowArmour(createSnowBoots(), "snowBoots", Material.LEATHER_BOOTS);
        addSnowArmour(createSnowLeggings(), "snowLeggings", Material.LEATHER_LEGGINGS);
        addSnowArmour(createSnowChestPiece(), "snowChestpiece", Material.LEATHER_CHESTPLATE);
        addSnowArmour(createSnowHat(), "snowHat", Material.LEATHER_HELMET);

        addDesertArmour(createDesertBoots(), "desertBoots", Material.LEATHER_BOOTS);
        addDesertArmour(createDesertLeggings(), "desertLeggings", Material.LEATHER_LEGGINGS);
        addDesertArmour(createDesertChestPiece(), "desertChestpiece", Material.LEATHER_CHESTPLATE);
        addDesertArmour(createDesertHat(), "desertHat", Material.LEATHER_HELMET);
    }

    private static void addSnowArmour(ItemStack result, String key, Material centralPiece) {
        ShapedRecipe snowArmourRecipe = new ShapedRecipe(new NamespacedKey(NoirPVPPlugin.getInstance(), key),
                result);
        snowArmourRecipe.shape("%%%","%#%", "%%%");
        snowArmourRecipe.setIngredient('%', Material.SNOW_BLOCK);
        snowArmourRecipe.setIngredient('#', centralPiece);
        NoirPVPPlugin.getInstance().getServer().addRecipe(snowArmourRecipe);
    }

    private static void addDesertArmour(ItemStack result, String key, Material centralPiece) {
        ShapedRecipe desertArmourRecipe = new ShapedRecipe(new NamespacedKey(NoirPVPPlugin.getInstance(), key),
                result);
        desertArmourRecipe.shape("%%%", "%#%", "%%%");
        desertArmourRecipe.setIngredient('%', Material.NETHERRACK);
        desertArmourRecipe.setIngredient('#', centralPiece);
        NoirPVPPlugin.getInstance().getServer().addRecipe(desertArmourRecipe);
    }

    public static ItemStack createSnowBoots() {
        return createArmour("Snow Armour Boots", "Protects against the cold.",
                Material.LEATHER_BOOTS, Color.WHITE);
    }

    public static ItemStack createSnowLeggings() {
        return createArmour("Snow Armour Leggings", "Protects against the cold.",
                Material.LEATHER_LEGGINGS, Color.WHITE);
    }

    public static ItemStack createSnowChestPiece() {
        return createArmour("Snow Armour Chestpiece", "Protects against the cold.",
                Material.LEATHER_CHESTPLATE, Color.WHITE);
    }

    public static ItemStack createSnowHat() {
        return createArmour("Snow Armour Hat", "Protects against the cold.",
                Material.LEATHER_HELMET, Color.WHITE);
    }

    public static ItemStack createDesertBoots() {
        return createArmour("Desert Armour Boots", "Protects against the heat.",
                Material.LEATHER_BOOTS, Color.RED);
    }

    public static ItemStack createDesertLeggings() {
        return createArmour("Desert Armour Leggings", "Protects against the heat.",
                Material.LEATHER_LEGGINGS, Color.RED);
    }

    public static ItemStack createDesertChestPiece() {
        return createArmour("Desert Armour Chestpiece", "Protects against the heat.",
                Material.LEATHER_CHESTPLATE, Color.RED);
    }

    public static ItemStack createDesertHat() {
        return createArmour("Desert Armour Hat", "Protects against the heat.",
                Material.LEATHER_HELMET, Color.RED);
    }

    public static ItemStack createArmour(String name, String loreString, Material armourType, Color colour) {
        ItemStack armourItem = new ItemStack(armourType);
        LeatherArmorMeta armourMeta = (LeatherArmorMeta) armourItem.getItemMeta();
        armourMeta.setColor(colour);

        List<String> lore = new ArrayList<>();
        lore.add(loreString);
        armourMeta.setLore(lore);

        armourMeta.setDisplayName(name);

        armourItem.setItemMeta(armourMeta);
        return armourItem;
    }
}
