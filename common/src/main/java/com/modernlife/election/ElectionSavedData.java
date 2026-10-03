package com.modernlife.election;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class ElectionSavedData extends SavedData {
    private boolean active = false;
    private final Map<UUID, Integer> votes = new HashMap<>();
    private final HashSet<UUID> votedPlayers = new HashSet<>(); // YENİ: Oy kullananlar

    public boolean isActive() { return this.active; }
    public Map<UUID, Integer> getVotes() { return this.votes; }

    public void startElection() {
        this.active = true;
        this.votes.clear();
        this.votedPlayers.clear(); // Yeni seçimde eski oyları sıfırla!
        this.setDirty();
    }

    public void stopElection() {
        this.active = false;
        this.setDirty();
    }

    // YENİ: Oyuncu daha önce oy vermiş mi kontrolü
    public boolean hasVoted(UUID playerUuid) {
        return votedPlayers.contains(playerUuid);
    }

    public void addVote(UUID voterUuid, UUID candidateUuid) {
        if (!active || votedPlayers.contains(voterUuid)) return;
        
        votes.put(candidateUuid, votes.getOrDefault(candidateUuid, 0) + 1);
        votedPlayers.add(voterUuid); // Oyuncuyu "oy kullandı" olarak işaretle
        this.setDirty();
    }

    // NBT YÜKLEME VE KAYDETME İŞLEMLERİNE YENİ LİSTEYİ EKLEYELİM
    public static ElectionSavedData load(CompoundTag tag) {
        ElectionSavedData data = new ElectionSavedData();
        data.active = tag.getBoolean("active");
        
        ListTag votesList = tag.getList("votes", Tag.TAG_COMPOUND);
        for (int i = 0; i < votesList.size(); i++) {
            CompoundTag entry = votesList.getCompound(i);
            data.votes.put(entry.getUUID("candidate"), entry.getInt("count"));
        }

        // Oy kullananları NBT'den geri yükle
        ListTag votedList = tag.getList("votedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < votedList.size(); i++) {
            CompoundTag entry = votedList.getCompound(i);
            data.votedPlayers.add(entry.getUUID("voter"));
        }
        
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("active", this.active);
        
        ListTag votesList = new ListTag();
        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("candidate", entry.getKey());
            entryTag.putInt("count", entry.getValue());
            votesList.add(entryTag);
        }
        tag.put("votes", votesList);

        // Oy kullananları NBT'ye kaydet
        ListTag votedList = new ListTag();
        for (UUID uuid : votedPlayers) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("voter", uuid);
            votedList.add(entryTag);
        }
        tag.put("votedPlayers", votedList);
        
        return tag;
    }

    public static ElectionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ElectionSavedData::load, ElectionSavedData::new, "election_data");
    }
}