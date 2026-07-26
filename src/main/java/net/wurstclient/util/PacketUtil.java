package net.wurstclient.util;

import net.minecraft.network.protocol.Packet;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IConnection;

import java.util.HashSet;

public class PacketUtil {
    public static HashSet<Packet<?>> clientSentPackets = new HashSet<>();

    public static void sendPacket(Packet<?> packet) {
        if (WurstClient.MC.getConnection() == null) return;

        clientSentPackets.add(packet);
        WurstClient.MC.getConnection().send(packet);
    }

    public static void receivePacket(Packet<?> packet) {
        if (WurstClient.MC.getConnection() == null) return;

        clientSentPackets.add(packet);
        ((IConnection) (WurstClient.MC.getConnection().getConnection())).bridge$receivePacket(packet);
    }
}
