package com.modernlife.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PriceJsonGenerator {

    public static void generate() {
        System.out.println("[Bankacı] Eşya veritabanı oluşturuluyor...");

        JsonObject root = new JsonObject();
        JsonObject categories = new JsonObject();

        JsonObject cokKolay = new JsonObject();
        JsonObject kolay = new JsonObject();
        JsonObject normal = new JsonObject();
        JsonObject zor = new JsonObject();
        JsonObject buyulerVeIksirler = new JsonObject();
        JsonObject lootlanabilir = new JsonObject();

        // 1. OYUNDAKİ BÜTÜN EŞYALARI TARA VE KATEGORİLE
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
            String id = loc.toString();
            String path = loc.getPath();

            // Sadece hava bloğunu atla
            if (path.equals("air")) continue;

            // Filtreleme ve Fiyatlandırma Mantığı
            if (path.contains("diamond") || path.contains("emerald") || path.contains("netherite") || path.contains("star") || path.contains("skull") || path.contains("debris")) {
                zor.addProperty(id, 1000);
            } 
            else if (path.contains("gold") || path.contains("redstone") || path.contains("lapis") || path.contains("quartz") || path.contains("pearl") || path.contains("obsidian") || path.contains("amethyst")) {
                normal.addProperty(id, 100);
            } 
            else if (path.contains("iron") || path.contains("copper") || path.contains("coal") || path.contains("raw") || path.contains("ingot") || path.contains("beef") || path.contains("porkchop") || path.contains("leather")) {
                kolay.addProperty(id, 25);
            } 
            else if (path.contains("elytra") || path.contains("totem") || path.contains("saddle") || path.contains("disc") || path.contains("template") || path.contains("horse_armor") || path.contains("heart_of_the_sea")) {
                lootlanabilir.addProperty(id, 5000);
            } 
            else if (path.contains("enchanted_book") || path.contains("potion")) {
                continue;
            } 
            else {
                cokKolay.addProperty(id, 2);
            }
        }

        // 2. OYUNDAKİ BÜTÜN BÜYÜLERİ TARA
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            ResourceLocation loc = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            String encId = loc.toString();
            int maxLevel = enchantment.getMaxLevel();
            
            String bookNbt = "minecraft:enchanted_book{enchantment:'" + encId + "',level:" + maxLevel + "}";
            buyulerVeIksirler.addProperty(bookNbt, 2500);
        }

        // 3. OYUNDAKİ BÜTÜN İKSİRLERİ TARA
        for (Potion potion : BuiltInRegistries.POTION) {
            ResourceLocation loc = BuiltInRegistries.POTION.getKey(potion);
            String potId = loc.toString();
            if (potId.equals("minecraft:empty")) continue;
            
            String potionNbt = "minecraft:potion{Potion:'" + potId + "'}";
            buyulerVeIksirler.addProperty(potionNbt, 500);
        }

        // KATEGORİLERİ ANA JSON'A EKLE
        categories.add("COK_KOLAY", cokKolay);
        categories.add("KOLAY", kolay);
        categories.add("NORMAL", normal);
        categories.add("ZOR", zor);
        categories.add("BUYULER_VE_IKSIRLER", buyulerVeIksirler);
        categories.add("LOOTLANABILIR", lootlanabilir);
        
        root.add("categories", categories);

        // DOSYAYA YAZDIR
        File outputFile = new File("prices_generated.json");
        try (FileWriter writer = new FileWriter(outputFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
            System.out.println("[Bankacı] MUHTEŞEM! prices_generated.json dosyası başarıyla oluşturuldu! (" + outputFile.getAbsolutePath() + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}