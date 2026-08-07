/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.C;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.KeyboardInputListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlinkUtil;
import net.wurstclient.util.Rotation;

@SearchTags({"anti knockback", "AntiVelocity", "anti velocity", "NoKnockback",
	"no knockback", "AntiKB", "anti kb"})
public final class AntiKnockbackHack extends Hack implements TickRotationListener, PacketInputListener, KeyboardInputListener
{
	private final SliderSetting maxDelayTicks =
		new SliderSetting("Horizontal Strength",
			"How many ticks allowed to delay knockback",
			12, 0, 20, 1, ValueDisplay.INTEGER.withSuffix(" ticks"));
	
	public AntiKnockbackHack()
	{
		super("AntiKnockback");
		setCategory(Category.COMBAT);
		addSetting(maxDelayTicks);

		EVENTS.add(TickRotationListener.class, this);
		EVENTS.add(KeyboardInputListener.class, this);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PacketInputListener.class, this);
	}

	private Vec3 lastVelocity = Vec3.ZERO;
	private int beginDelayTick = 0;
	private boolean shouldJump = false;
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PacketInputListener.class, this);
	}

	@Override
	public void onRotationEvent(TickRotationEvent tickRotationEvent) {
		shouldJump = BlinkUtil.isBlinking(false, true, this.getClass()) && shouldUnblink();
		if (!shouldJump) return;

		BlinkUtil.popBlink(false, true, this.getClass());
		tickRotationEvent.rotation = tickRotationEvent.rotation.withYaw(getYawFromVelocity(lastVelocity));
		lastVelocity = Vec3.ZERO;
	}

	@Override
	public void onReceivedPacket(PacketInputEvent event) {
		// dont wanna rehandle the same velo packet!
		if (BlinkUtil.shouldAllowPackets) return;

		if (event.getPacket() instanceof ClientboundSetEntityMotionPacket(int id, Vec3 movement)) {
			if (id != C.p().getId() || movement.y <= 0) return;
			lastVelocity = movement;

			if (BlinkUtil.isBlinking(false, true)) return;

			beginDelayTick = C.tick;
			BlinkUtil.pushBlink(false, true, this.getClass(), event.getPacket());
			event.cancel();
		}
	}

	@Override
	public void onKeyboardInputEvent(KeyboardInputEvent keyboardInputEvent) {
		keyboardInputEvent.input = new Input(
				shouldJump || keyboardInputEvent.input.forward(),
				!shouldJump && keyboardInputEvent.input.backward(),
				!shouldJump && keyboardInputEvent.input.left(),
				!shouldJump && keyboardInputEvent.input.right(),
				shouldJump || !BlinkUtil.isBlinking(false, true, this.getClass()) && keyboardInputEvent.input.jump(),
				!shouldJump && keyboardInputEvent.input.shift(),
				shouldJump || keyboardInputEvent.input.sprint()
		);

		if (!shouldJump) return;
	}

	private boolean shouldUnblink() {
		return forcedUnblink() ||
				(C.p().onGround() && validEntityInReach());
	}

	private boolean validEntityInReach() {
		AABB playerReachBox = C.p().getBoundingBox().inflate(4, 4, 4);
		return C.w()
				.getEntities(C.p(), playerReachBox)
				.stream()
				.anyMatch(entity -> KillauraHack.getTargetRotationPoint(entity) != null);
	}

	private boolean forcedUnblink() {
		return C.tick - beginDelayTick > maxDelayTicks.getValue();
	}

	// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/combat/Velocity.java#L203
	private static float getYawFromVelocity(Vec3 velocity) {
		float yaw = (float) (Math.toDegrees(Math.atan2(velocity.x + velocity.z, velocity.x - velocity.z)) + 45);
		return Rotation.applyWrap360(TickRotationEvent.getLastRotation().yaw(), yaw > 180 ? yaw - 360 : yaw);
	}
}
