/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.RotationFaker;
import net.wurstclient.WurstClient;
import net.wurstclient.events.TickRotationListener;

public enum RotationUtils
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	public static Vec3 getEyesPos()
	{
		LocalPlayer player = MC.player;
		float eyeHeight = player.getEyeHeight(player.getPose());
		return player.position().add(0, eyeHeight, 0);
	}
	
	public static Vec3 getClientLookVec(float partialTicks)
	{
		float yaw = MC.player.getViewYRot(partialTicks);
		float pitch = MC.player.getViewXRot(partialTicks);
		return new Rotation(yaw, pitch).toLookVec();
	}
	
	public static Vec3 getServerLookVec()
	{
		RotationFaker rf = WurstClient.INSTANCE.getRotationFaker();
		return new Rotation(rf.getServerYaw(), rf.getServerPitch()).toLookVec();
	}
	
	public static Rotation getNeededRotations(Vec3 vec) {
		return applyGcd(
				TickRotationListener.TickRotationEvent.getLastRotation(),
				getRotationNoGcd(TickRotationListener.TickRotationEvent.getLastRotation(), WurstClient.p().getEyePosition(), vec)
		);
	}

	// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/RotationUtil.java#L64
	private static Rotation getRotationNoGcd(Rotation currentRotation, Vec3 from, Vec3 to) {
		Vec3 diff = to.subtract(from);
		double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

		float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, dist));
		float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

		float neededPitchChange = currentRotation.pitch() - pitch;
		float neededYawChange = (currentRotation.yaw() % 360) - yaw;

		neededYawChange = Math.abs(neededYawChange) > 180
				? -1 *
				Math.signum(neededYawChange) *
				  (360 - Math.abs(neededYawChange))
				: neededYawChange;

		return new Rotation(
				currentRotation.yaw() - neededYawChange,
				currentRotation.pitch() - neededPitchChange
		);
	}
	
	public static double getAngleToLookVec(Vec3 vec)
	{
		LocalPlayer player = MC.player;
		Rotation current = new Rotation(player.getYRot(), player.getXRot());
		Rotation needed = getNeededRotations(vec);
		return current.getAngleTo(needed);
	}
	
	public static float getHorizontalAngleToLookVec(Vec3 vec)
	{
		float currentYaw = Mth.wrapDegrees(MC.player.getYRot());
		float neededYaw = getNeededRotations(vec).yaw();
		return Mth.wrapDegrees(currentYaw - neededYaw);
	}
	
	/**
	 * Returns true if the player is already facing within 1 degree of the
	 * specified rotation.
	 */
	public static boolean isAlreadyFacing(Rotation rotation)
	{
		return getAngleToLastReportedLookVec(rotation) <= 1.0;
	}
	
	public static double getAngleToLastReportedLookVec(Vec3 vec)
	{
		Rotation needed = getNeededRotations(vec);
		return getAngleToLastReportedLookVec(needed);
	}
	
	public static double getAngleToLastReportedLookVec(Rotation rotation)
	{
		LocalPlayer player = MC.player;
		
		// lastYaw/Pitch do not get updated when the player is in a vehicle
		Rotation lastReported = player.isPassenger()
			? new Rotation(player.getYRot(), player.getXRot())
			: new Rotation(player.yRotLast, player.xRotLast);
		
		return lastReported.getAngleTo(rotation);
	}
	
	/**
	 * Returns true if the player is facing anywhere within the given box
	 * and is no further away than the given range.
	 */
	public static boolean isFacingBox(AABB box, double range)
	{
		Vec3 start = getEyesPos();
		Vec3 end = start.add(getServerLookVec().scale(range));
		return box.clip(start, end).isPresent();
	}
	
	/**
	 * Returns the next rotation that the player should be facing in order to
	 * slowly turn towards the specified end rotation, at a rate of roughly
	 * <code>maxChange</code> degrees per tick.
	 */
	public static Rotation slowlyTurnTowards(Rotation end, float maxChange)
	{
		LocalPlayer player = MC.player;
		
		float startYaw =
			player.isPassenger() ? player.getYRot() : player.yRotLast;
		float startPitch =
			player.isPassenger() ? player.getXRot() : player.xRotLast;
		float endYaw = end.yaw();
		float endPitch = end.pitch();
		
		float yawChange = Math.abs(Mth.wrapDegrees(endYaw - startYaw));
		float pitchChange = Math.abs(Mth.wrapDegrees(endPitch - startPitch));
		
		float maxChangeYaw = pitchChange == 0 ? maxChange
			: Math.min(maxChange, maxChange * yawChange / pitchChange);
		float maxChangePitch = yawChange == 0 ? maxChange
			: Math.min(maxChange, maxChange * pitchChange / yawChange);
		
		float nextYaw = limitAngleChange(startYaw, endYaw, maxChangeYaw);
		float nextPitch =
			limitAngleChange(startPitch, endPitch, maxChangePitch);
		
		return new Rotation(nextYaw, nextPitch);
	}
	
	/**
	 * Limits the change in angle between the current and intended rotation to
	 * the specified maximum change. Useful for smoothing out rotations and
	 * making combat hacks harder to detect.
	 *
	 * <p>
	 * For best results, do not wrap the current angle before calling this
	 * method!
	 */
	public static float limitAngleChange(float current, float intended,
		float maxChange)
	{
		float currentWrapped = Mth.wrapDegrees(current);
		float intendedWrapped = Mth.wrapDegrees(intended);
		
		float change = Mth.wrapDegrees(intendedWrapped - currentWrapped);
		change = Mth.clamp(change, -maxChange, maxChange);
		
		return current + change;
	}
	
	/**
	 * Removes unnecessary changes in angle caused by wrapping. Useful for
	 * making combat hacks harder to detect.
	 *
	 * <p>
	 * For example, if the current angle is 179 degrees and the intended angle
	 * is -179 degrees, you only need to turn 2 degrees to face the intended
	 * angle, not 358 degrees.
	 *
	 * <p>
	 * DO NOT wrap the current angle before calling this method! You will get
	 * incorrect results if you do.
	 */
	public static float limitAngleChange(float current, float intended)
	{
		float currentWrapped = Mth.wrapDegrees(current);
		float intendedWrapped = Mth.wrapDegrees(intended);
		
		float change = Mth.wrapDegrees(intendedWrapped - currentWrapped);
		
		return current + change;
	}

	// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/RotationUtil.java#L25
	public static Rotation applyGcd(Rotation currentRotation, Rotation targetRotation) {
		double f = MC.options.sensitivity().get() * (double) 0.6F + (double) 0.2F;
		double sens = (f * f * f) * (double) 8.0F;

		Rotation difference = targetRotation.difference(currentRotation);

		float yawDelta = (float) (Math.round(difference.yaw() / 0.15d) * sens) * 0.15F;
		float pitchDelta = (float) (Math.round(difference.pitch() / 0.15d) * sens) * 0.15F;

		return new Rotation(
				currentRotation.yaw() - yawDelta,
				Mth.clamp(currentRotation.pitch() - pitchDelta, -90, 90)
		);
	}
}
