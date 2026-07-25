/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.*;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

@SearchTags({"kill aura", "ForceField", "force field", "CrystalAura",
	"crystal aura", "AutoCrystal", "auto crystal"})
public final class KillauraHack extends Hack
	implements TickRotationListener, HandleInputListener, RenderListener
{
	private final SliderSetting range = new SliderSetting("Range",
		"Determines how far Killaura will reach to attack entities.\n"
			+ "Anything that is further away than the specified value will not be attacked.",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final SliderSetting speedRandMS =
		new SliderSetting("Speed randomization",
			"Helps you bypass anti-cheat plugins by varying the delay between"
				+ " attacks.\n\n" + "\u00b1100ms is recommended for Vulcan.\n\n"
				+ "0 (off) is fine for NoCheat+, AAC, Grim, Verus, Spartan, and"
				+ " vanilla servers.",
			100, 0, 1000, 50, ValueDisplay.INTEGER.withPrefix("\u00b1")
				.withSuffix("ms").withLabel(0, "off"));
	
	private final EnumSetting<Priority> priority = new EnumSetting<>("Priority",
		"Determines which entity will be attacked first.\n"
			+ "\u00a7lDistance\u00a7r - Attacks the closest entity.\n"
			+ "\u00a7lAngle\u00a7r - Attacks the entity that requires the least head movement.\n"
			+ "\u00a7lHealth\u00a7r - Attacks the weakest entity.",
		Priority.values(), Priority.ANGLE);
	
	private final SliderSetting fov =
		new SliderSetting("FOV", 360, 30, 360, 10, ValueDisplay.DEGREES);

	private final CheckboxSetting damageIndicator = new CheckboxSetting(
		"Damage indicator",
		"Renders a colored box within the target, inversely proportional to its remaining health.",
		true);

	private final CheckboxSetting noJumpRotate = new CheckboxSetting(
			"No Jump Rotate",
			"Doesn't rotate the player when jumping.",
			true);

	private final PauseAttackOnContainersSetting pauseOnContainers =
		new PauseAttackOnContainersSetting(true);

	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"Ensures that you don't reach through blocks when attacking.\n\n"
				+ "Slower but can help with anti-cheat plugins.",
			false);

	private final EntityFilterList entityFilters =
			EntityFilterList.genericCombat();
	
	private Entity target;
	private Entity renderTarget;
	
	public KillauraHack()
	{
		super("Killaura");
		setCategory(Category.COMBAT);
		
		addSetting(range);
		addSetting(speed);
		addSetting(speedRandMS);
		addSetting(priority);
		addSetting(fov);
		addSetting(damageIndicator);
		addSetting(pauseOnContainers);
		addSetting(checkLOS);
		addSetting(noJumpRotate);

		entityFilters.forEach(this::addSetting);
	}
	
	@Override
	protected void onEnable()
	{
		// disable other killauras
		WURST.getHax().aimAssistHack.setEnabled(false);
		WURST.getHax().clickAuraHack.setEnabled(false);
		WURST.getHax().crystalAuraHack.setEnabled(false);
		WURST.getHax().fightBotHack.setEnabled(false);
		WURST.getHax().killauraLegitHack.setEnabled(false);
		WURST.getHax().multiAuraHack.setEnabled(false);
		WURST.getHax().protectHack.setEnabled(false);
		WURST.getHax().triggerBotHack.setEnabled(false);
		WURST.getHax().tpAuraHack.setEnabled(false);
		
		speed.resetTimer(speedRandMS.getValue());
		EVENTS.add(TickRotationListener.class, this);
		EVENTS.add(HandleInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(TickRotationListener.class, this);
		EVENTS.remove(HandleInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		target = null;
		renderTarget = null;
	}
	
	@Override
	public void onRotationEvent(TickRotationEvent tickRotationEvent) {
		speed.updateTimer();
		if (pauseOnContainers.shouldPause() || MC.getConnection() == null || MC.level == null) return;

		Stream<Entity> stream = EntityUtils.getAttackableEntities();
		double rangeSq = range.getValueSq();
		stream = stream.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rangeSq);
		
		if(fov.getValue() < 360.0)
			stream = stream.filter(e -> RotationUtils.getAngleToLookVec(
				e.getBoundingBox().getCenter()) <= fov.getValue() / 2.0);

		stream = entityFilters.applyTo(stream);
		
		target = stream.min(priority.getSelected().comparator).orElse(null);
		renderTarget = target;
		if(target == null)
			return;
		
		WURST.getHax().autoSwordHack.setSlot(target);
		
		Vec3 hitVec = target.getBoundingBox().getCenter();
		Rotation hitRotation = RotationUtils.getNeededRotations(hitVec);

		if (noJumpRotate.isChecked() && EntityUtils.willJump()) tickRotationEvent.rotation = tickRotationEvent.rotation.withPitch(hitRotation.pitch());
		else tickRotationEvent.rotation = hitRotation;
	}
	
	@Override
	public void onHandleInput()
	{
		speed.updateTimer();
		if(!speed.isTimeToAttack() || target == null) return;

		HitResult hitResult = EntityUtils.getMouseOver(TickRotationEvent.getLastRotation());
		if(checkLOS.isChecked() && (!(hitResult instanceof EntityHitResult entityHitResult) || entityHitResult.getEntity() != target)) return;

		MC.gameMode.attack(p(), target);
		p().swing(InteractionHand.MAIN_HAND);
		
		target = null;
		speed.resetTimer(speedRandMS.getValue());
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(renderTarget == null || !damageIndicator.isChecked())
			return;
		
		float p = 1;
		if(renderTarget instanceof LivingEntity le && le.getMaxHealth() > 1e-5)
			p = 1 - le.getHealth() / le.getMaxHealth();
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		AABB box = EntityUtils.getLerpedBox(renderTarget, partialTicks);
		if(p < 1)
			box = box.deflate((1 - p) * 0.5 * box.getXsize(),
				(1 - p) * 0.5 * box.getYsize(), (1 - p) * 0.5 * box.getZsize());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}

	private enum Priority
	{
		DISTANCE("Distance", EntityUtils::distanceToHitboxSq),
		
		ANGLE("Angle",
			e -> RotationUtils
				.getAngleToLookVec(e.getBoundingBox().getCenter())),
		
		HEALTH("Health", e -> e instanceof LivingEntity
			? ((LivingEntity)e).getHealth() : Integer.MAX_VALUE);
		
		private final String name;
		private final Comparator<Entity> comparator;
		
		private Priority(String name, ToDoubleFunction<Entity> keyExtractor)
		{
			this.name = name;
			comparator = Comparator.comparingDouble(keyExtractor);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
