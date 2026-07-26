package com.makeforge.jjk.net;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server: "I pressed ability {slot}."
 * slot 0 = primary (G), 1 = secondary (H), 2 = ultimate/domain (J).
 *
 * The server re-derives targeting from the sending player, so the client
 * never gets to dictate what gets hit — it only says which key was pressed.
 */
public record AbilityPayload(int slot) implements CustomPacketPayload {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("jjk", "ability");
    public static final CustomPacketPayload.Type<AbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, AbilityPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AbilityPayload::slot,
                    AbilityPayload::new
            );

    @Override
    public CustomPacketPayload.Type<AbilityPayload> type() {
        return TYPE;
    }
}
