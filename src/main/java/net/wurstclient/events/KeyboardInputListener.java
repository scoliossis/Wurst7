package net.wurstclient.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

import java.util.ArrayList;

public interface KeyboardInputListener extends Listener
{
    /**
     * Fired at the beginning of
     * {@link Minecraft#tick()}.
     */
    // im passing the entire event so i can edit the rotation, because its a record and im not bouta make my own rotation class.
    public void onKeyboardInputEvent(KeyboardInputEvent keyboardInputEvent);

    /**
     * Fired at the beginning of
     * {@link Minecraft#tick()}.
     */
    public static class KeyboardInputEvent
            extends Event<KeyboardInputListener>
    {
        public static KeyboardInputEvent lastInput = null;
        public Input input;

        public KeyboardInputEvent(Input input)
        {
            this.input = input;
        }

        @Override
        public void fire(ArrayList<KeyboardInputListener> listeners)
        {
            for(KeyboardInputListener listener : listeners)
                listener.onKeyboardInputEvent(this);
        }

        @Override
        public Class<KeyboardInputListener> getListenerType()
        {
            return KeyboardInputListener.class;
        }
    }
}