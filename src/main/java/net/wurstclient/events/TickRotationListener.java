package net.wurstclient.events;

import net.minecraft.client.Minecraft;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;
import net.wurstclient.util.Rotation;

import java.util.ArrayList;

public interface TickRotationListener extends Listener
{
    /**
     * Fired at the beginning of
     * {@link Minecraft#tick()}.
     */
    // im passing the entire event so i can edit the rotation, because its a record and im not bouta make my own rotation class.
    public void onRotationEvent(TickRotationEvent tickRotationEvent);

    /**
     * Fired at the beginning of
     * {@link Minecraft#tick()}.
     */
    public static class TickRotationEvent
            extends Event<TickRotationListener>
    {
        public static Rotation clientRotation;
        private static Rotation lastRotation;
        public static void setLastRotation(Rotation lastRotation) {
            TickRotationEvent.lastRotation = lastRotation;
        }
        public static Rotation getLastRotation() {
            return lastRotation == null ? clientRotation : lastRotation;
        }

        public static float lastPitch;
        public static float currentPitch;

        public Rotation rotation;

        public TickRotationEvent(Rotation rotation)
        {
            this.rotation = rotation;
        }

        @Override
        public void fire(ArrayList<TickRotationListener> listeners)
        {
            for(TickRotationListener listener : listeners)
                listener.onRotationEvent(this);
        }

        @Override
        public Class<TickRotationListener> getListenerType()
        {
            return TickRotationListener.class;
        }
    }
}