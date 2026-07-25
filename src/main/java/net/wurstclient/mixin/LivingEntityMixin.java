/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.ILivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ILivingEntity
{
	@Shadow
	private int noJumpDelay;

	public LivingEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	/**
	 * Stops the other darkness effect in caves when AntiBlind is enabled.
	 */
	@Inject(method = "getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F",
		at = @At("HEAD"),
		cancellable = true)
	private void onGetEffectFadeFactor(Holder<MobEffect> registryEntry,
		float delta, CallbackInfoReturnable<Float> cir)
	{
		if(registryEntry != MobEffects.DARKNESS)
			return;
		
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			cir.setReturnValue(0F);
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	public void onAiStep(CallbackInfo ci) {
		if (!this.is(WurstClient.p()) || !WurstClient.INSTANCE.getHax().noJumpDelayHack.isEnabled()) return;
		this.noJumpDelay = WurstClient.INSTANCE.getHax().noJumpDelayHack.jumpDelay.getValueI();
	}

	@Override
	public int bridge$getNoJumpDelay() {
		return this.noJumpDelay;
	}
}
