/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.HandleInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.AttackSpeedSliderSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.PauseAttackOnContainersSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.filterlists.EntityFilterList;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.Rotation;
import net.wurstclient.util.RotationUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// todo: check if held item is weapon, not just sword
@SearchTags({"kill aura", "ForceField", "force field", "CrystalAura",
	"crystal aura", "AutoCrystal", "auto crystal"})
public final class KillauraHack extends Hack
	implements TickRotationListener, HandleInputListener, RenderListener
{
	private final SliderSetting rotationRange = new SliderSetting("Rotation Range",
		"Determines the distance at which killaura will begin to rotate towards the target.",
		5, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final AttackSpeedSliderSetting speed =
		new AttackSpeedSliderSetting();
	
	private final SliderSetting speedRandMS =
		new SliderSetting("Speed randomization",
                """
                        Helps you bypass anti-cheat plugins by varying the delay between\
                         attacks.
                        
                        ±100ms is recommended for Vulcan.
                        
                        0 (off) is fine for NoCheat+, AAC, Grim, Verus, Spartan, and\
                         vanilla servers.""",
			100, 0, 1000, 50, ValueDisplay.INTEGER.withPrefix("±")
				.withSuffix("ms").withLabel(0, "off"));
	
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

	private final CheckboxSetting onlyWhileHoldingSword = new CheckboxSetting(
			"Swords Only",
			"Only enables when holding a sword.",
			true);
	private final CheckboxSetting onlyWhileLeftClickDown = new CheckboxSetting(
			"Only While Left Click Down",
			"Only enables when holding left click.",
			true);

	private final PauseAttackOnContainersSetting pauseOnContainers =
		new PauseAttackOnContainersSetting(true);

	private final EntityFilterList entityFilters =
			EntityFilterList.genericCombat();
	
	private Entity target;

	private static int switchIndex = 0;
	
	public KillauraHack()
	{
		super("Killaura");
		setCategory(Category.COMBAT);
		
		addSetting(rotationRange);
		addSetting(speed);
		addSetting(speedRandMS);
		addSetting(fov);
		addSetting(damageIndicator);
		addSetting(pauseOnContainers);
		addSetting(noJumpRotate);
		addSetting(onlyWhileHoldingSword);
		addSetting(onlyWhileLeftClickDown);

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
	}
	
	@Override
	public void onRotationEvent(TickRotationEvent tickRotationEvent) {
		speed.updateTimer();
		if (pauseOnContainers.shouldPause() || !shouldAura()) {
			target = null;
			return;
		}

		Stream<Entity> stream = EntityUtils.getAttackableEntities();

		if(fov.getValue() < 360.0)
			stream = stream.filter(e -> RotationUtils.getAngleToLookVec(
				e.getBoundingBox().getCenter()) <= fov.getValue() / 2.0);

		stream = entityFilters.applyTo(stream);
		stream = stream.filter(e -> EntityUtils.distanceToHitboxSq(e) <= rotationRange.getValueSq());
		List<Entity> entities = stream.sorted(Comparator.comparingInt(Entity::getId)).toList();
		if (entities.isEmpty()) {
			target = null;
			return;
		}

		int checks = 0;
		Vec3 hitVec;

		switchIndex = switchIndex % entities.size();
		while ((hitVec = getTargetRotationPoint(entities.get(switchIndex))) == null && checks++ < entities.size())
			switchIndex = (switchIndex + 1) % entities.size();

		target = entities.get(switchIndex);
		if(target == null) return;

		Rotation hitRotation = RotationUtils.getNeededRotations(hitVec != null ? hitVec : target.getEyePosition());

		if (noJumpRotate.isChecked() && EntityUtils.willJump()) tickRotationEvent.rotation = tickRotationEvent.rotation.withPitch(hitRotation.pitch());
		else tickRotationEvent.rotation = hitRotation;
	}
	
	@Override
	public void onHandleInput()
	{
		speed.updateTimer();
		if(!speed.isTimeToAttack() || target == null || p().isUsingItem()) return;

		HitResult hitResult = EntityUtils.getMouseOver(TickRotationEvent.getLastRotation());
		if(!(hitResult instanceof EntityHitResult entityHitResult) || entityHitResult.getEntity() != target) return;

		WURST.getHax().autoSwordHack.setSlot(target);

		IMC.bridge$leftClickMouse();

		switchIndex++;
		target = null;
		speed.resetTimer(speedRandMS.getValue());
	}
	
	@Override
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(target == null || !damageIndicator.isChecked())
			return;
		
		float p = 1;
		if(target instanceof LivingEntity le && le.getMaxHealth() > 1e-5)
			p = 1 - le.getHealth() / le.getMaxHealth();
		float red = p * 2F;
		float green = 2 - red;
		float[] rgb = {red, green, 0};
		int quadColor = RenderUtils.toIntColor(rgb, 0.25F);
		int lineColor = RenderUtils.toIntColor(rgb, 0.5F);
		
		AABB box = EntityUtils.getLerpedBox(target, partialTicks);
		if(p < 1)
			box = box.deflate((1 - p) * 0.5 * box.getXsize(),
				(1 - p) * 0.5 * box.getYsize(), (1 - p) * 0.5 * box.getZsize());
		
		RenderUtils.drawSolidBox(matrixStack, box, quadColor, false);
		RenderUtils.drawOutlinedBox(matrixStack, box, lineColor, false);
	}

	private boolean shouldAura() {
		boolean breakingBlock = (EntityUtils.getMouseOver(TickRotationEvent.getLastRotation()) instanceof BlockHitResult blockHitResult
				&& blockHitResult.getType() != BlockHitResult.Type.MISS
				&& MC.options.keyAttack.isDown()
		)
				|| MC.gameMode.isDestroying;
		return (!onlyWhileHoldingSword.isChecked() || p().getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS))
				&& (!onlyWhileLeftClickDown.isChecked() || MC.options.keyAttack.isDown())
				&& !breakingBlock;
	}

	// from scalehackv3 https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/TargetUtil.java#L88
	// gives you slightly more reach!
	public static Vec3 getTargetRotationPoint(Entity entity) {
		if (entity == null) return null;

		AABB targetBoundingBox = entity.getBoundingBox();
		Vec3 closestPoint = getClosestPoint(targetBoundingBox);
		Rotation bestRotation = RotationUtils.getNeededRotations(closestPoint);
		// check that looking at the closest point doesnt have a wall between us and the entity
		if (EntityUtils.getMouseOver(bestRotation) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity)
			return closestPoint;

		ArrayList<Vec3> possibleRotations = new ArrayList<>();
		for (int i = 0; i < 8; i++) {

			// add all possible corners, added using the order recommended for binary truth tables by my cs teacher, shoutout
			possibleRotations.add(new Vec3(
					i % 2 == 0 ? entity.position().x : entity.position().x + targetBoundingBox.getXsize(),
					i % 4 >= 2 ? entity.position().y : entity.position().y + targetBoundingBox.getYsize(),
					i % 8 >= 4 ? entity.position().z : entity.position().z + targetBoundingBox.getZsize()
			));
		}

		// add the top middle and bottom of the player centre aswell, probably doesnt help
		possibleRotations.add(new Vec3(entity.getX(), (targetBoundingBox.maxY + targetBoundingBox.minY) / 2, entity.getZ()));
		possibleRotations.add(new Vec3(entity.getX(), targetBoundingBox.minY, entity.getZ()));
		possibleRotations.add(new Vec3(entity.getX(), targetBoundingBox.maxY, entity.getZ()));
		possibleRotations.add(closestPoint);

		// check all possible rotations until we find
		for (Vec3 possibleRotationVector : possibleRotations) {
			Rotation possibleRotation = RotationUtils.getNeededRotations(possibleRotationVector);
			if (EntityUtils.getMouseOver(possibleRotation) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) {
				return possibleRotationVector;
			}
		}

		return null;
	}

	// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/WorldUtil.java#L208
	public static Vec3 getClosestPoint(AABB collisionBox) {
		Vec3 eyePos = p().getEyePosition();

		double posX = eyePos.x;
		double posY = eyePos.y;
		double posZ = eyePos.z;

		if (eyePos.x < collisionBox.minX) posX = collisionBox.minX;
		else if (eyePos.x > collisionBox.maxX) posX = collisionBox.maxX;

		if (eyePos.y < collisionBox.minY) posY = collisionBox.minY;
		else if (eyePos.y > collisionBox.maxY) posY = collisionBox.maxY;

		if (eyePos.z < collisionBox.minZ) posZ = collisionBox.minZ;
		else if (eyePos.z > collisionBox.maxZ) posZ = collisionBox.maxZ;

		return new Vec3(posX, posY, posZ);
	}
}
