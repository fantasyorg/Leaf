package com.redefantasy.server.util;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Serializes an entity's full non-default data-watcher into the vanilla entity-metadata wire format
 * (the {@code ClientboundSetEntityDataPacket} value list + 0xFF terminator, without the entity id),
 * for the replay {@code EntityMetadataUpdateEvent}.
 */
public final class ReplayMetadataSerializer {

    private ReplayMetadataSerializer() {
    }

    public static byte[] serialize(Entity entity, RegistryAccess registryAccess) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            List<SynchedEntityData.DataValue<?>> values = entity.getEntityData().getNonDefaultValues();
            if (values != null) {
                for (SynchedEntityData.DataValue<?> value : values) {
                    value.write(buffer);
                }
            }
            buffer.writeByte(255);

            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }
}
