/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.CommandSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VelocityFromEntityCollisionListener.VelocityFromEntityCollisionEvent;
import net.wurstclient.events.VelocityFromFluidListener.VelocityFromFluidEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin
	implements Nameable, EntityAccess, CommandSource
{
	@Shadow
	protected abstract Vec3 maybeBackOffFromEdge(Vec3 delta, MoverType moverType);

	@Shadow
	public abstract boolean is(Entity other);

	/**
	 * This mixin makes the VelocityFromFluidEvent work, which is used by
	 * AntiWaterPush.
	 */
	@WrapOperation(method = "updateFluidInteraction()Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z",
			ordinal = 0))
	private boolean wrapUpdateFluidInteractionIsPushedByFluid(Entity instance,
		Operation<Boolean> original)
	{
		VelocityFromFluidEvent event = new VelocityFromFluidEvent(instance);
		EventManager.fire(event);
		
		if(event.isCancelled())
			return false;
		
		return original.call(instance);
	}
	
	@Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onPushAwayFrom(Entity entity, CallbackInfo ci)
	{
		VelocityFromEntityCollisionEvent event =
			new VelocityFromEntityCollisionEvent((Entity)(Object)this);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	/**
	 * Makes invisible entities render as ghosts if TrueSight is enabled.
	 */
	@Inject(
		method = "isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z",
		at = @At("RETURN"),
		cancellable = true)
	private void onIsInvisibleTo(Player player,
		CallbackInfoReturnable<Boolean> cir)
	{
		// Return early if the entity is not invisible
		if(!cir.getReturnValueZ())
			return;
		
		if(WurstClient.INSTANCE.getHax().trueSightHack
			.shouldBeVisible((Entity)(Object)this))
			cir.setReturnValue(false);
	}

	/**
	 * scale made this!!!! i dont like using these comments, because with intellij, /// does the same
	 * but mr alexander does NOT use intellij ig
	 * this function checks if crouching would save you from falling off a block, if it would, it crouches.
	 */
	@Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;"))
	private void onMoveCheckIfBackOffEdge(MoverType moverType, Vec3 delta, CallbackInfo ci) {
		Entity entity = (Entity)(Object)this;
		if (WurstClient.p() == null || !this.is(WurstClient.p()) || !WurstClient.INSTANCE.getHax().safeWalkHack.shouldSafewalk()) return;

		Input prevPlayerInput = WurstClient.p().input.keyPresses;
		WurstClient.p().input.keyPresses = new Input(
				prevPlayerInput.forward(),
				prevPlayerInput.backward(),
				prevPlayerInput.left(),
				prevPlayerInput.right(),
				prevPlayerInput.jump(),
				true,
				prevPlayerInput.sprint()
		);

		Vec3 sneakAdjustedMovement = this.maybeBackOffFromEdge(delta, moverType);

		// if sneaking doesnt stop u, then we arent gonna go over the edge, we are safe!
		if (sneakAdjustedMovement.x == delta.x && sneakAdjustedMovement.z == delta.z) {
			// if hold crouch mode is on, we dont want to perma sneak
			boolean shouldSneak = !WurstClient.INSTANCE.getHax().safeWalkHack.requireSneakDown.isChecked() && prevPlayerInput.shift();
			WurstClient.p().input.keyPresses = new Input(
					prevPlayerInput.forward(),
					prevPlayerInput.backward(),
					prevPlayerInput.left(),
					prevPlayerInput.right(),
					prevPlayerInput.jump(),
					shouldSneak,
					prevPlayerInput.sprint()
			);
		}
	}
}