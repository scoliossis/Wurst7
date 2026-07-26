/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;
import net.wurstclient.events.TickRotationListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
	/**
	 * Forces the nametag to be rendered if configured in NameTags.
	 */
	@Inject(
		method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;",
			ordinal = 0),
		cancellable = true)
	private void shouldForceLabel(LivingEntity entity, double distanceSq,
		CallbackInfoReturnable<Boolean> cir)
	{
		// return true immediately after the distance check
		if(WurstClient.INSTANCE.getHax().nameTagsHack
			.shouldForcePlayerNametags()
			&& WurstClient.INSTANCE.getHax().nameTagsHack.entityFilters.testOne(entity))
			cir.setReturnValue(true);
	}


	/// overwrites the player render pitch to match the pitch sent to the server as its edited in net.wurstclient.mixin.MinecraftMixin.preTickPlayer()
	@Redirect(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F")
	)
	public float fixPitch(LivingEntity instance, float partialTicks) {
		return instance == WurstClient.p()
				? Mth.rotLerp(partialTicks, TickRotationListener.TickRotationEvent.lastPitch, TickRotationListener.TickRotationEvent.currentPitch)
				: instance.getXRot(partialTicks);
	}
}
