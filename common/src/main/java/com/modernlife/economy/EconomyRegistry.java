package com.modernlife.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class EconomyRegistry {
    private static final Map<Item, Double> PRICES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadOrGenerateConfig() {
        PRICES.clear();
        Path configPath = Platform.getConfigFolder().resolve("modernlife_prices.json");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            generateDefaultConfig(configFile);
        }

        loadConfig(configFile);
    }

    private static void generateDefaultConfig(File file) {
        Map<String, Double> defaults = new HashMap<>();

        defaults.put("minecraft:dirt", 0.1);
        defaults.put("minecraft:sand", 0.1);
        defaults.put("minecraft:gravel", 0.1);
        defaults.put("minecraft:flint", 0.2);

        defaults.put("minecraft:stone", 0.25);
        defaults.put("minecraft:cobblestone", 0.25);
        defaults.put("minecraft:deepslate", 0.25);
        defaults.put("minecraft:cobbled_deepslate", 0.25);

        defaults.put("minecraft:oak_log", 1.0);
        defaults.put("minecraft:spruce_log", 1.0);
        defaults.put("minecraft:birch_log", 1.0);
        defaults.put("minecraft:jungle_log", 1.0);
        defaults.put("minecraft:acacia_log", 1.0);
        defaults.put("minecraft:dark_oak_log", 1.0);
        defaults.put("minecraft:mangrove_log", 1.0);
        defaults.put("minecraft:cherry_log", 1.0);

        defaults.put("minecraft:oak_planks", 0.25);
        defaults.put("minecraft:spruce_planks", 0.25);
        defaults.put("minecraft:birch_planks", 0.25);
        defaults.put("minecraft:jungle_planks", 0.25);
        defaults.put("minecraft:acacia_planks", 0.25);
        defaults.put("minecraft:dark_oak_planks", 0.25);
        defaults.put("minecraft:mangrove_planks", 0.25);
        defaults.put("minecraft:cherry_planks", 0.25);

        defaults.put("minecraft:redstone", 1.0);
        defaults.put("minecraft:lapis_lazuli", 1.0);
        defaults.put("minecraft:coal", 2.5);
        defaults.put("minecraft:raw_copper", 2.5);
        defaults.put("minecraft:copper_ingot", 2.5);
        defaults.put("minecraft:quartz", 2.5);
        defaults.put("minecraft:emerald", 5.0);

        defaults.put("minecraft:raw_iron", 10.0);
        defaults.put("minecraft:iron_ingot", 10.0);
        defaults.put("minecraft:raw_gold", 25.0);
        defaults.put("minecraft:gold_ingot", 25.0);
        defaults.put("minecraft:diamond", 50.0);
        defaults.put("minecraft:netherite_scrap", 350.0);
        defaults.put("minecraft:netherite_ingot", 1500.0);

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(defaults, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig(File file) {
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Double>>() {}.getType();
            Map<String, Double> map = GSON.fromJson(reader, type);

            if (map != null) {
                for (Map.Entry<String, Double> entry : map.entrySet()) {
                    ResourceLocation loc = ResourceLocation.tryParse(entry.getKey());
                    if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                        Item item = BuiltInRegistries.ITEM.get(loc);
                        PRICES.put(item, entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double getItemPrice(Item item) {
        if (PRICES.isEmpty()) {
            loadOrGenerateConfig();
        }
        return PRICES.getOrDefault(item, 0.0);
    }
}