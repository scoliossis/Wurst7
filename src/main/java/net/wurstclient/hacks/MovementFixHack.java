// scale made this module, so no official wurst copyright!
package net.wurstclient.hacks;

import net.minecraft.world.entity.player.Input;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.KeyboardInputListener;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/client/MovementFix.java
@SearchTags({"straight"})
public final class MovementFixHack extends Hack implements KeyboardInputListener
{
    public MovementFixHack()
    {
        super("MovementFix");
        setCategory(Category.OTHER);
    }

    @Override
    protected void onEnable()
    {
        EVENTS.add(KeyboardInputListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(KeyboardInputListener.class, this);
    }

    private static float yawDeficit = 0;

    @Override
    public void onKeyboardInputEvent(KeyboardInputEvent keyboardInputEvent) {
        MovementDirection currentDirection = MovementDirection.getCurrentDirection(keyboardInputEvent);

        // make sure ur moving
        if (currentDirection == null) return;

        MovementDirection fixedMovementDirection = getMovementDirection(currentDirection);

        keyboardInputEvent.input = new Input(
                fixedMovementDirection.forward,
                fixedMovementDirection.back,
                fixedMovementDirection.left,
                fixedMovementDirection.right,
                keyboardInputEvent.input.jump(),
                keyboardInputEvent.input.shift(),
                keyboardInputEvent.input.sprint()
        );
    }

    private MovementDirection getMovementDirection(MovementDirection currentDirection) {
        float yawDifference = TickRotationListener.TickRotationEvent.clientRotation.yaw() - TickRotationListener.TickRotationEvent.getLastRotation().yaw();

        float yawDeficitAdded = (Math.round((yawDifference + yawDeficit) / 45) * 45) - yawDifference;

        yawDifference += yawDeficitAdded;
        yawDeficit -= yawDeficitAdded;

        // calculate how much yaw precision we have lost.
        yawDeficit += (Math.round(yawDifference / 45) * 45) - yawDifference;

        int yawOrdinal = Math.floorMod((int) (yawDifference / 45), MovementDirection.values().length);

        return MovementDirection.values()[Math.floorMod(currentDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];
    }

    private enum MovementDirection {
        NORTH       (   true,    false,  false,  false  ),
        NORTH_EAST  (   true,    true,   false,  false  ),
        EAST        (   false,   true,   false,  false  ),
        SOUTH_EAST  (   false,   true,   true,   false  ),
        SOUTH       (   false,   false,  true,   false  ),
        SOUTH_WEST  (   false,   false,  true,   true   ),
        WEST        (   false,   false,  false,  true   ),
        NORTH_WEST  (   true,    false,  false,  true   );

        // alt + insert lets u create an allargsconstructor!!!! no lombok needed
        MovementDirection(boolean forward, boolean right, boolean back, boolean left) {
            this.forward = forward;
            this.right = right;
            this.back = back;
            this.left = left;
        }

        public static MovementDirection getCurrentDirection(KeyboardInputEvent keyboardInputEvent) {
            boolean forward = keyboardInputEvent.input.forward() && !keyboardInputEvent.input.backward();
            boolean backward = !keyboardInputEvent.input.forward() && keyboardInputEvent.input.backward();
            boolean left = keyboardInputEvent.input.left() && !keyboardInputEvent.input.right();
            boolean right = !keyboardInputEvent.input.left() && keyboardInputEvent.input.right();

            for (MovementDirection direction : MovementDirection.values()) {
                if (direction.forward == forward && direction.left == left && direction.right == right && direction.back == backward) return direction;
            }

            return null;
        }

        private final boolean forward, right, back, left;
    }
}