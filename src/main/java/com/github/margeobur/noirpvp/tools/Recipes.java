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

    private static void addRecipes() {
        Plugin thePlugin = NoirPVPPlugin.getInstance();

        ShapedRecipe wBootsRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "winterBoots"),
                createWinterBoots());
        wBootsRecipe.shape("%*%","%*%");
        wBootsRecipe.setIngredient('*', Material.AIR);
        wBootsRecipe.setIngredient('%', Material.WHITE_WOOL);
        thePlugin.getServer().addRecipe(wBootsRecipe);

        ShapedRecipe wLegsRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "winterLeggings"),
                createWinterLeggings());
        wLegsRecipe.shape("%%%", "%*%", "%*%");
        wLegsRecipe.setIngredient('*', Material.AIR);
        wLegsRecipe.setIngredient('%', Material.WHITE_WOOL);
        thePlugin.getServer().addRecipe(wLegsRecipe);

        ShapedRecipe wJacketRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "winterJacket"),
                createWinterJacket());
        wJacketRecipe.shape("%*%", "%%%", "%%%");
        wJacketRecipe.setIngredient('*', Material.AIR);
        wJacketRecipe.setIngredient('%', Material.WHITE_WOOL);
        thePlugin.getServer().addRecipe(wJacketRecipe);

        ShapedRecipe wBeanieRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "winterBeanie"),
                createWinterBeanie());
        wBeanieRecipe.shape("%%%", "%*%");
        wBeanieRecipe.setIngredient('*', Material.AIR);
        wBeanieRecipe.setIngredient('%', Material.WHITE_WOOL);
        thePlugin.getServer().addRecipe(wBeanieRecipe);


        ShapedRecipe sSneakersRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "summerSneakers"),
                createSneakers());
        sSneakersRecipe.shape("%*%", "#*#");
        sSneakersRecipe.setIngredient('*', Material.AIR);
        sSneakersRecipe.setIngredient('%', Material.STRING);
        sSneakersRecipe.setIngredient('#', Material.LEATHER);
        thePlugin.getServer().addRecipe(sSneakersRecipe);

        ShapedRecipe sShortsRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "summerShorts"),
                createShorts());
        sShortsRecipe.shape("%%%", "#*#", "#*#");
        sShortsRecipe.setIngredient('*', Material.AIR);
        sShortsRecipe.setIngredient('%', Material.STRING);
        sShortsRecipe.setIngredient('#', Material.LEATHER);
        thePlugin.getServer().addRecipe(sShortsRecipe);

        ShapedRecipe sShirtRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "summerShirt"),
                createCottonShirt());
        sShirtRecipe.shape("%*%", "###" ,"%%%");
        sShirtRecipe.setIngredient('*', Material.AIR);
        sShirtRecipe.setIngredient('%', Material.STRING);
        sShirtRecipe.setIngredient('#', Material.LEATHER);
        thePlugin.getServer().addRecipe(sShirtRecipe);

        ShapedRecipe sSunhatRecipe = new ShapedRecipe(new NamespacedKey(thePlugin, "summerSunhat"),
                createSunhat());
        sSunhatRecipe.shape("###", "%*%");
        sSunhatRecipe.setIngredient('*', Material.AIR);
        sSunhatRecipe.setIngredient('%', Material.STRING);
        sSunhatRecipe.setIngredient('#', Material.WHEAT);
        thePlugin.getServer().addRecipe(sSunhatRecipe);
    }

    public static ItemStack createWinterBoots() {
        return createWinterItem("Winter Boots", Material.LEATHER_BOOTS);
    }

    public static ItemStack createWinterLeggings() {
        return createWinterItem("Winter Leggings", Material.LEATHER_LEGGINGS);
    }

    public static ItemStack createWinterJacket() {
        return createWinterItem("Winter Jacket", Material.LEATHER_CHESTPLATE);
    }

    public static ItemStack createWinterBeanie() {
        return createWinterItem("Winter Beanie", Material.LEATHER_HELMET);
    }

    private static ItemStack createWinterItem(String name, Material armourType) {
        return createArmourItem(name, armourType, "Protects against the cold.", Color.WHITE);
    }

    public static ItemStack createSneakers() {
        return createSummerItem("Summer Sneakers", Material.LEATHER_BOOTS);
    }

    public static ItemStack createShorts() {
        return createSummerItem("Summer Shorts", Material.LEATHER_LEGGINGS);
    }

    public static ItemStack createCottonShirt() {
        return createSummerItem("Summer Shirt", Material.LEATHER_CHESTPLATE);
    }

    public static ItemStack createSunhat() {
        return createSummerItem("Summer Sunhat", Material.LEATHER_HELMET);
    }

    private static ItemStack createSummerItem(String name, Material armourType) {
        return createArmourItem(name, armourType, "Protects against the heat.", Color.OLIVE);
    }

    private static ItemStack createArmourItem(String name, Material armourType, String loreString, Color colour) {
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
