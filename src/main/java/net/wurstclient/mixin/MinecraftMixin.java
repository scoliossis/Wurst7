/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.HandleBlockBreakingListener.HandleBlockBreakingEvent;
import net.wurstclient.events.HandleInputListener.HandleInputEvent;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.events.RightClickListener.RightClickEvent;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.mixinterface.ILocalPlayer;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.mixinterface.IMultiPlayerGameMode;
import net.wurstclient.util.Rotation;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin
	extends ReentrantBlockableEventLoop<Runnable>
	implements WindowEventHandler, IMinecraftClient
{
	@Shadow
	@Final
	public File gameDirectory;
	@Shadow
	public MultiPlayerGameMode gameMode;
	@Shadow
	public LocalPlayer player;

	@Shadow
	@Nullable
	public ClientLevel level;
	@Shadow
	private volatile boolean pause;
	@Unique
	private YggdrasilAuthenticationService wurstAuthenticationService;
	
	private User wurstSession;
	private ProfileKeyPairManager wurstProfileKeys;
	
	private MinecraftMixin(WurstClient wurst, String name,
		boolean propagatesCrashes)
	{
		super(name, propagatesCrashes);
	}
	
	@Inject(method = "<init>",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/Services;create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/server/Services;",
			shift = At.Shift.AFTER))
	private void captureAuthenticationService(GameConfig args, CallbackInfo ci,
		@Local YggdrasilAuthenticationService yggdrasilAuthenticationService)
	{
		wurstAuthenticationService = yggdrasilAuthenticationService;
	}
	
	/**
	 * Runs just before {@link Minecraft#handleKeybinds()}, bypassing
	 * the <code>gui.overlay() == null && gui.screen() == null</code> check in
	 * {@link Minecraft#tick()}.
	 */
	@Inject(method = "tick()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;",
			ordinal = 0))
	private void onHandleInputEvents(CallbackInfo ci)
	{
		// Make sure this event is not fired outside of gameplay
		if(player == null)
			return;
		
		EventManager.fire(HandleInputEvent.INSTANCE);
	}
	
	@Inject(method = "startAttack()Z",
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
			ordinal = 0),
		cancellable = true)
	private void onDoAttack(CallbackInfoReturnable<Boolean> cir)
	{
		LeftClickEvent event = new LeftClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			cir.setReturnValue(false);
	}
	
	@Inject(method = "startUseItem()V",
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I",
			ordinal = 0),
		cancellable = true)
	private void onDoItemUse(CallbackInfo ci)
	{
		RightClickEvent event = new RightClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(method = "pickBlockOrEntity()V", at = @At("HEAD"))
	private void onDoItemPick(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		HitResult hitResult = WurstClient.MC.hitResult;
		if(!(hitResult instanceof EntityHitResult eHitResult))
			return;
		
		WurstClient.INSTANCE.getFriends().middleClick(eHitResult.getEntity());
	}
	
	/**
	 * Allows hacks to cancel vanilla block breaking and replace it with their
	 * own. Useful for Nuker-like hacks.
	 */
	@Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
	private void onHandleBlockBreaking(boolean breaking, CallbackInfo ci)
	{
		HandleBlockBreakingEvent event = new HandleBlockBreakingEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(method = "getUser()Lnet/minecraft/client/User;",
		at = @At("HEAD"),
		cancellable = true)
	private void onGetSession(CallbackInfoReturnable<User> cir)
	{
		if(wurstSession != null)
			cir.setReturnValue(wurstSession);
	}
	
	@Inject(method = "getGameProfile()Lcom/mojang/authlib/GameProfile;",
		at = @At("RETURN"),
		cancellable = true)
	public void onGetGameProfile(CallbackInfoReturnable<GameProfile> cir)
	{
		if(wurstSession == null)
			return;
		
		GameProfile oldProfile = cir.getReturnValue();
		GameProfile newProfile = new GameProfile(wurstSession.getProfileId(),
			wurstSession.getName(), oldProfile.properties());
		cir.setReturnValue(newProfile);
	}
	
	@Inject(
		method = "getProfileKeyPairManager()Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;",
		at = @At("HEAD"),
		cancellable = true)
	private void onGetProfileKeys(
		CallbackInfoReturnable<ProfileKeyPairManager> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
		
		if(wurstProfileKeys == null)
			return;
		
		cir.setReturnValue(wurstProfileKeys);
	}
	
	@Inject(method = "allowsTelemetry()Z", at = @At("HEAD"), cancellable = true)
	private void onIsTelemetryEnabledByApi(CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Inject(method = "extraTelemetryAvailable()Z",
		at = @At("HEAD"),
		cancellable = true)
	private void onIsOptionalTelemetryEnabledByApi(
		CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Override
	public ILocalPlayer getPlayer()
	{
		return (ILocalPlayer)player;
	}
	
	@Override
	public IMultiPlayerGameMode getInteractionManager()
	{
		return (IMultiPlayerGameMode)gameMode;
	}
	
	@Override
	public User getWurstSession()
	{
		return wurstSession;
	}
	
	@Override
	public void setWurstSession(User session)
	{
		wurstSession = session;
		if(session == null)
		{
			wurstProfileKeys = null;
			return;
		}
		
		String accessToken = session.getAccessToken();
		boolean isOffline = accessToken == null || accessToken.isBlank()
			|| accessToken.equals("0") || accessToken.equals("null");
		UserApiService userApiService = isOffline ? UserApiService.OFFLINE
			: wurstAuthenticationService.createUserApiService(accessToken);
		wurstProfileKeys = ProfileKeyPairManager.create(userApiService, session,
			gameDirectory.toPath());
	}



	// im sadly a lover of server side rotations, and i REFUSE to play without them.
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/TickRateManager;tick()V"))
	public void preTickPlayer(CallbackInfo ci) {
		// store client side rotation so we can restore it when we are done ticking the real player
		TickRotationListener.TickRotationEvent.clientRotation = new Rotation(WurstClient.p().getYRot(), WurstClient.p().getXRot());

		// fire event, boom chika wow wow
		TickRotationListener.TickRotationEvent event = new TickRotationListener.TickRotationEvent(TickRotationListener.TickRotationEvent.clientRotation);
		EventManager.fire(event);

		// make sure that the client rotation is within 180 of the server yaw, to stop flagging aimmodulo360 on disabling
		TickRotationListener.TickRotationEvent.clientRotation =
				new Rotation(
						Rotation.applyWrap360(event.rotation.yaw(), TickRotationListener.TickRotationEvent.clientRotation.yaw()),
						TickRotationListener.TickRotationEvent.clientRotation.pitch()
				);
		TickRotationListener.TickRotationEvent.setLastRotation(event.rotation);

		TickRotationListener.TickRotationEvent.lastPitch = TickRotationListener.TickRotationEvent.currentPitch;
		TickRotationListener.TickRotationEvent.currentPitch = event.rotation.pitch();

		// override player rots with the rots set in the event
		WurstClient.p().setXRot(event.rotation.pitch());
		WurstClient.p().setYRot(event.rotation.yaw());
	}

	@Inject(method = "tick", at = @At(value = "TAIL"))
	public void postTickPlayer(CallbackInfo ci) {
		if (this.level != null && !this.pause) {
			// restore client side rotation, cause the tick is done, and we just rendering now.
			WurstClient.p().setXRot(TickRotationListener.TickRotationEvent.clientRotation.pitch());
			WurstClient.p().setYRot(TickRotationListener.TickRotationEvent.clientRotation.yaw());
		}
	}
}
