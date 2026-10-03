package com.modernlife.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class OpenElectionMenuPacket {
    private final boolean isActive;
    private final boolean isAdmin;
    private final List<Candidate> candidates;

    public static class Candidate {
        public final UUID uuid;
        public final String name;

        public Candidate(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public OpenElectionMenuPacket(boolean isActive, boolean isAdmin, List<Candidate> candidates) {
        this.isActive = isActive;
        this.isAdmin = isAdmin;
        this.candidates = candidates;
    }

    public static void encode(OpenElectionMenuPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isActive);
        buf.writeBoolean(msg.isAdmin);
        buf.writeInt(msg.candidates.size());
        for (Candidate c : msg.candidates) {
            buf.writeUUID(c.uuid);
            buf.writeUtf(c.name);
        }
    }

    public static OpenElectionMenuPacket decode(FriendlyByteBuf buf) {
        boolean isActive = buf.readBoolean();
        boolean isAdmin = buf.readBoolean();
        int size = buf.readInt();
        List<Candidate> candidates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            candidates.add(new Candidate(buf.readUUID(), buf.readUtf()));
        }
        return new OpenElectionMenuPacket(isActive, isAdmin, candidates);
    }

    public static void handle(OpenElectionMenuPacket msg, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            com.modernlife.client.ClientAccess.openElectionScreen(msg.isActive, msg.isAdmin, msg.candidates);
        });
    }
}