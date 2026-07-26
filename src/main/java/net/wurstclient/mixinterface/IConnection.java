package net.wurstclient.mixinterface;

import net.minecraft.network.protocol.Packet;

public interface IConnection {
    public void bridge$receivePacket(Packet<?> packet);
}
