package net.wurstclient.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.KeyboardInputListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends ClientInput {
    @Shadow @Final private Options options;

    @Shadow
    private static float calculateImpulse(boolean positive, boolean negative) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        KeyboardInputListener.KeyboardInputEvent params = new KeyboardInputListener.KeyboardInputEvent(new Input(
                this.options.keyUp.isDown(),
                this.options.keyDown.isDown(),
                this.options.keyLeft.isDown(),
                this.options.keyRight.isDown(),
                this.options.keyJump.isDown(),
                this.options.keyShift.isDown(),
                this.options.keySprint.isDown()
        ));
        EventManager.fire(params);
        KeyboardInputListener.KeyboardInputEvent.lastInput = params;

        this.keyPresses = new Input(
                params.input.forward(),
                params.input.backward(),
                params.input.left(),
                params.input.right(),
                params.input.jump(),
                params.input.shift(),
                params.input.sprint()
        );

        float forwardImpulse = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
        float leftImpulse = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
        this.moveVector = new Vec2(leftImpulse, forwardImpulse).normalized();

        ci.cancel();
    }
}
