package com.modernlife.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Persistent economy data. A bank account belongs to an identity, never to a card. */
public class PlayerEconomy {
    private final Map<String, Long> identityBalances = new HashMap<>();
    private long legacyBalance;
    private boolean hasLegacyBalance;
    private long lastTaxPaidDay = -1;
    private long daysToPay = 1;
    private long taxDue = 0;
    private long taxDueAmount = 0;
    private String realName = "";
    private int birthYear = 0;

    public long getBalance(final String identity) {
        return identityBalances.getOrDefault(normalizeIdentity(identity), 0L);
    }

    public boolean hasIdentityBalance(final String identity) {
        return identityBalances.containsKey(normalizeIdentity(identity));
    }

    public void setBalance(final String identity, final long balance) {
        identityBalances.put(normalizeIdentity(identity), balance);
    }

    public void addBalance(final String identity, final long amount) {
        if (amount <= 0L) return;
        final String key = normalizeIdentity(identity);
        final long current = getBalance(key);
        identityBalances.put(key, current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
    }

    public boolean removeBalance(final String identity, final long amount) {
        if (amount <= 0L) return false;
        final String key = normalizeIdentity(identity);
        final long current = getBalance(key);
        if (current < amount) {
            return false;
        }
        identityBalances.put(key, current - amount);
        return true;
    }

    public boolean forceRemoveBalance(final String identity, final long amount) {
        if (amount <= 0L) return false;
        final String key = normalizeIdentity(identity);
        final long current = getBalance(key);
        identityBalances.put(key, current - amount);
        return true;
    }

    public void removeIdentity(final String identity) {
        identityBalances.remove(normalizeIdentity(identity));
    }

    public Map<String, Long> getIdentityBalances() {
        return Collections.unmodifiableMap(identityBalances);
    }

    public void replaceIdentityBalances(final Map<String, Long> balances) {
        identityBalances.clear();
        balances.forEach((identity, balance) -> identityBalances.put(normalizeIdentity(identity), balance));
    }

    public long takeLegacyBalance(final String identity) {
        if (!hasLegacyBalance || (!realName.isEmpty() && !realName.equals(identity))) return 0L;
        hasLegacyBalance = false;
        return legacyBalance;
    }

    public void copyFrom(final PlayerEconomy other) {
        replaceIdentityBalances(other.identityBalances);
        legacyBalance = other.legacyBalance;
        hasLegacyBalance = other.hasLegacyBalance;
        lastTaxPaidDay = other.lastTaxPaidDay;
        daysToPay = other.daysToPay;
        taxDue = other.taxDue;
        taxDueAmount = other.taxDueAmount;
        realName = other.realName;
        birthYear = other.birthYear;
    }

    public long getDaysToPay() { return daysToPay; }
    public void setDaysToPay(long days) { daysToPay = Math.max(1, days); }
    public long getLastTaxPaidDay() { return lastTaxPaidDay; }
    public void setLastTaxPaidDay(long day) { lastTaxPaidDay = day; }
    public long getTaxDue() { return taxDue; }
    public void setTaxDue(long amount) { taxDue = Math.max(0L, amount); }
    public long getTaxDueAmount() { return taxDueAmount; }
    public void setTaxDueAmount(long amount) { taxDueAmount = Math.max(0L, amount); }
    public String getRealName() { return realName; }
    public void setRealName(String name) { realName = name == null ? "" : name; }
    public int getBirthYear() { return birthYear; }
    public void setBirthYear(int year) { birthYear = year; }

    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        final CompoundTag balancesTag = new CompoundTag();
        identityBalances.forEach(balancesTag::putLong);
        tag.put("identityBalances", balancesTag);
        if (hasLegacyBalance) tag.putLong("legacyBalance", legacyBalance);
        tag.putLong("lastTaxPaidDay", lastTaxPaidDay);
        tag.putLong("daysToPay", daysToPay);
        tag.putLong("taxDue", taxDue);
        tag.putString("realName", realName);
        tag.putInt("birthYear", birthYear);
        return tag;
    }

    public void deserializeNBT(final CompoundTag tag) {
        identityBalances.clear();
        realName = tag.getString("realName");
        if (tag.contains("identityBalances", CompoundTag.TAG_COMPOUND)) {
            final CompoundTag balancesTag = tag.getCompound("identityBalances");
            for (String identity : balancesTag.getAllKeys()) {
                identityBalances.put(normalizeIdentity(identity), balancesTag.getLong(identity));
            }
        }
        hasLegacyBalance = tag.contains("legacyBalance") || tag.contains("balance");
        legacyBalance = Math.max(0L, tag.contains("legacyBalance") ? tag.getLong("legacyBalance") : tag.getLong("balance"));
        lastTaxPaidDay = tag.getLong("lastTaxPaidDay");
        daysToPay = Math.max(1L, tag.getLong("daysToPay"));
        taxDue = Math.max(0L, tag.getLong("taxDue"));
        birthYear = tag.getInt("birthYear");
    }

    private static String normalizeIdentity(final String identity) {
        return identity == null || identity.isBlank() ? "__unassigned__" : identity;
    }
}