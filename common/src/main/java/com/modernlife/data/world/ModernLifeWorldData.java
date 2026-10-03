package com.modernlife.data.world;

import com.modernlife.ModernLifeMod;
import com.modernlife.data.TaxRates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModernLifeWorldData extends SavedData {
    private static final String STORAGE_ID = ModernLifeMod.MODID + "_world";
    private static final double DEFAULT_PENALTY_RATE = 0.10;
    private static final long DEFAULT_UNLICENSED_PENALTY = 100L;
    private static final long DEFAULT_UNLICENSED_MOUNT_PENALTY = 100L;

    public enum DifficultyLevel {
        VERY_EASY(2.0, "difficulty.modernlife.very_easy"),
        EASY(1.5, "difficulty.modernlife.easy"),
        NORMAL(1.0, "difficulty.modernlife.normal"),
        HARD(0.5, "difficulty.modernlife.hard"),
        VERY_HARD(0.25, "difficulty.modernlife.very_hard"),
        IMPOSSIBLE(0.10, "difficulty.modernlife.impossible");

        private final double multiplier;
        private final String translationKey;

        DifficultyLevel(double multiplier, String translationKey) {
            this.multiplier = multiplier;
            this.translationKey = translationKey;
        }

        public double getMultiplier() { return multiplier; }
        public String getTranslationKey() { return translationKey; }
    }

    public enum EconomyMode {
        MODERNLIFE_NATIVE("economy.modernlife.mode.native"),
        LIGHTMANS_CURRENCY("economy.modernlife.mode.lightmans");

        private final String translationKey;

        EconomyMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    private boolean isDifficultySet = false;
    private DifficultyLevel difficulty = DifficultyLevel.NORMAL;
    private transient boolean difficultyMenuOpened = false;

    // Ekonomi Modu Yapılandırması
    private boolean isEconomyModeSet = false;
    private EconomyMode economyMode = EconomyMode.MODERNLIFE_NATIVE;

    @Nullable
    private UUID presidentUuid;
    private TaxRates taxRates = TaxRates.empty();
    private long worldDayCounter;
    private double penaltyRate = DEFAULT_PENALTY_RATE;
    private long unlicensedPenalty = DEFAULT_UNLICENSED_PENALTY; 
    private long unlicensedMountPenalty = DEFAULT_UNLICENSED_MOUNT_PENALTY;

    private long stateTreasuryBalance = 0;
    
    private boolean voteActive = false;
    private int pendingAmount = 0;
    private String pendingTarget = "";
    private String pendingReason = "";
    private int yesVotes = 0;
    private int noVotes = 0;
    private final List<UUID> votedPlayers = new ArrayList<>();

    public ModernLifeWorldData() {}

    public static ModernLifeWorldData get(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ModernLifeWorldData::load,
                ModernLifeWorldData::new,
                STORAGE_ID
        );
    }

    public static ModernLifeWorldData load(final CompoundTag tag) {
        final ModernLifeWorldData data = new ModernLifeWorldData();
        
        data.isDifficultySet = tag.getBoolean("IsDifficultySet");
        if (tag.contains("DifficultyOrdinal")) {
            data.difficulty = DifficultyLevel.values()[tag.getInt("DifficultyOrdinal")];
        }

        data.isEconomyModeSet = tag.getBoolean("IsEconomyModeSet");
        if (tag.contains("EconomyModeOrdinal")) {
            int ord = tag.getInt("EconomyModeOrdinal");
            if (ord >= 0 && ord < EconomyMode.values().length) {
                data.economyMode = EconomyMode.values()[ord];
            }
        }

        if (tag.hasUUID("President")) {
            data.presidentUuid = tag.getUUID("President");
        }
        data.taxRates = TaxRates.load(tag.getCompound("TaxRates"));
        data.worldDayCounter = Math.max(0L, tag.getLong("WorldDay"));
        data.penaltyRate = tag.contains("PenaltyRate") ? tag.getDouble("PenaltyRate") : DEFAULT_PENALTY_RATE;
        data.unlicensedPenalty = tag.contains("UnlicensedPenalty") ? tag.getLong("UnlicensedPenalty") : DEFAULT_UNLICENSED_PENALTY;
        data.unlicensedMountPenalty = tag.contains("UnlicensedMountPenalty") ? tag.getLong("UnlicensedMountPenalty") : DEFAULT_UNLICENSED_MOUNT_PENALTY;
        
        data.stateTreasuryBalance = tag.getLong("StateTreasuryBalance");
        data.voteActive = tag.getBoolean("VoteActive");
        data.pendingAmount = tag.getInt("PendingAmount");
        data.pendingTarget = tag.getString("PendingTarget");
        data.pendingReason = tag.getString("PendingReason");
        data.yesVotes = tag.getInt("YesVotes");
        data.noVotes = tag.getInt("NoVotes");
        
        if (tag.contains("VotedPlayers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("VotedPlayers", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                data.votedPlayers.add(UUID.fromString(list.getString(i)));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        tag.putBoolean("IsDifficultySet", isDifficultySet);
        tag.putInt("DifficultyOrdinal", difficulty.ordinal());

        tag.putBoolean("IsEconomyModeSet", isEconomyModeSet);
        tag.putInt("EconomyModeOrdinal", economyMode.ordinal());

        if (presidentUuid != null) {
            tag.putUUID("President", presidentUuid);
        }
        tag.put("TaxRates", taxRates.save());
        tag.putLong("WorldDay", worldDayCounter);
        tag.putDouble("PenaltyRate", penaltyRate);
        tag.putLong("UnlicensedPenalty", unlicensedPenalty);
        tag.putLong("UnlicensedMountPenalty", unlicensedMountPenalty);
        
        tag.putLong("StateTreasuryBalance", stateTreasuryBalance);
        tag.putBoolean("VoteActive", voteActive);
        tag.putInt("PendingAmount", pendingAmount);
        tag.putString("PendingTarget", pendingTarget);
        tag.putString("PendingReason", pendingReason);
        tag.putInt("YesVotes", yesVotes);
        tag.putInt("NoVotes", noVotes);
        
        ListTag list = new ListTag();
        for (UUID uuid : votedPlayers) {
            list.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("VotedPlayers", list);
        
        return tag;
    }

    public boolean isDifficultySet() { return isDifficultySet; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { 
        this.difficulty = difficulty; 
        this.isDifficultySet = true; 
        setDirty(); 
    }
    
    public boolean isEconomyModeSet() { return isEconomyModeSet; }
    public EconomyMode getEconomyMode() { return economyMode; }
    public void setEconomyMode(EconomyMode mode) {
        this.economyMode = mode;
        this.isEconomyModeSet = true;
        setDirty();
    }

    public boolean isDifficultyMenuOpened() { return difficultyMenuOpened; }
    public void setDifficultyMenuOpened(boolean opened) { this.difficultyMenuOpened = opened; }

    public long getUnlicensedPenalty() { return unlicensedPenalty; }
    public void setUnlicensedPenalty(long penalty) { this.unlicensedPenalty = Math.max(0L, penalty); setDirty(); }

    public long getUnlicensedMountPenalty() { return unlicensedMountPenalty; }
    public void setUnlicensedMountPenalty(long penalty) { this.unlicensedMountPenalty = Math.max(0L, penalty); setDirty(); }

    public long getStateTreasuryBalance() { return stateTreasuryBalance; }
    public void addStateTreasuryBalance(long amount) { this.stateTreasuryBalance += amount; setDirty(); }
    public boolean removeStateTreasuryBalance(long amount) {
        if (this.stateTreasuryBalance >= amount) {
            this.stateTreasuryBalance -= amount;
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isVoteActive() { return voteActive; }
    public String getPendingTarget() { return pendingTarget; }
    public int getPendingAmount() { return pendingAmount; }
    public String getPendingReason() { return pendingReason; }
    
    public void startVote(int amount, String target, String reason) {
        this.voteActive = true;
        this.pendingAmount = amount;
        this.pendingTarget = target;
        this.pendingReason = reason;
        this.yesVotes = 0;
        this.noVotes = 0;
        this.votedPlayers.clear();
        setDirty();
    }

    public boolean hasVoted(UUID playerUuid) { return votedPlayers.contains(playerUuid); }
    
    public void addVote(UUID playerUuid, boolean isYes) {
        if (!voteActive || hasVoted(playerUuid)) return;
        votedPlayers.add(playerUuid);
        if (isYes) yesVotes++; else noVotes++;
        setDirty();
    }
    
    public void endVote() {
        this.voteActive = false;
        this.votedPlayers.clear();
        setDirty();
    }
    
    public int getYesVotes() { return yesVotes; }
    public int getNoVotes() { return noVotes; }

    @Nullable
    public UUID getPresidentUuid() { return presidentUuid; }
    public void setPresidentUuid(@Nullable final UUID presidentUuid) { this.presidentUuid = presidentUuid; setDirty(); }
    public TaxRates getTaxRates() { return taxRates; }
    public void setTaxRates(final TaxRates taxRates) { this.taxRates = taxRates != null ? taxRates : TaxRates.empty(); setDirty(); }
    public long getWorldDayCounter() { return worldDayCounter; }
    public void setWorldDayCounter(final long worldDayCounter) { this.worldDayCounter = Math.max(0L, worldDayCounter); setDirty(); }
    public double getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(double penaltyRate) { this.penaltyRate = Math.max(0.0, Math.min(2.0, penaltyRate)); setDirty(); }
}