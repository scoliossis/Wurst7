/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.Rotation;

import java.util.Optional;

// im so sorry for my differnet coding style, im not pull requesting this anyway, sorry alex.
@SearchTags({"safe walk", "SneakSafety", "sneak safety", "SpeedBridgeHelper",
	"speed bridge helper"})
public final class SafeWalkHack extends Hack implements TickRotationListener {
	public final CheckboxSetting requireSneakDown =
			new CheckboxSetting("Require Sneak Down", "Only activates if the sneak key is down.", false);
	public final CheckboxSetting requireBlocks =
			new CheckboxSetting("Require Holding Blocks", "Only activates if a block you can bridge with is held.", false);
	public final CheckboxSetting requireHoldS =
			new CheckboxSetting("Require Walking Back", "Only activates if the back key is held.", false);
	public final CheckboxSetting requireInteractDown =
			new CheckboxSetting("Require Interact Key Down", "Only activates if the interact key is held down.", false);
	public final CheckboxSetting requireLookingDown =
			new CheckboxSetting("Require Looking Down", "Only activates if looking downwards.", false);

	public final CheckboxSetting rotationAssist =
			new CheckboxSetting("Rotation Assist", "Visibly sneak at edges.", false);

	public SafeWalkHack() {
		super("SafeWalk");
		setCategory(Category.MOVEMENT);

		// adding each setting is effort :pensive: but ig it saves using reflections.
		addSetting(requireSneakDown);
		addSetting(requireBlocks);
		addSetting(requireHoldS);
		addSetting(requireInteractDown);
		addSetting(requireLookingDown);
		addSetting(rotationAssist);

		// i can add the listener here because the function checks if its enabled anyway
		EVENTS.add(TickRotationListener.class, this);
	}

	// not static. does that make you happy alex? (its growing on me, sadly.)
	public boolean shouldSafewalk() {
		return this.isEnabled()
				&& (!requireSneakDown.isChecked() || MC.options.keyShift.isDown())
				&& (!requireBlocks.isChecked() || ScaffoldWalkHack.isValidBlock(p().getInventory().getSelectedSlot()))
				&& (!requireHoldS.isChecked() || MC.options.keyDown.isDown())
				&& (!requireInteractDown.isChecked() || MC.options.keyUse.isDown())
				&& (!requireLookingDown.isChecked() || p().getXRot() > 20);
	}

	private boolean onEdgeOfBlock() {
		return MC.level.getBlockState(ScaffoldWalkHack.blockBelowPlayer()).isAir();
	}

	// function from unreleased injectable mushroom!
	private BlockPos findClosestBlock() {
		// loops in a 5x5x5 radius and checks if:
		// checks if the block is below the player
		// checks if the block is NOT air
		// returns the closest block that meets those conditions
		Optional<BlockPos> closestBlock = BlockPos.findClosestMatch(
				ScaffoldWalkHack.blockBelowPlayer(),
				5,
				5,
				// i dont think i knew what a predicate was at the time so i typed "predicate" and predicate.not came up
				blockPos -> blockPos.getY() <= ScaffoldWalkHack.blockBelowPlayer().getY()
						&& !MC.level.getBlockState(blockPos).isAir()
		);

		return closestBlock.orElse(null);
	}

	private boolean isBlockHitResultValid(BlockHitResult blockHitResult) {
		BlockPos blockPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
		return !MC.level.getBlockState(blockHitResult.getBlockPos()).isAir()
				&& MC.level.getEntities(null, new AABB(blockPos)).isEmpty();
	}

	@Override
	public void onRotationEvent(TickRotationEvent tickRotationEvent) {
		if (!shouldSafewalk() || !rotationAssist.isChecked()) return;

		if (!onEdgeOfBlock()) {
			tickRotationEvent.rotation = tickRotationEvent.rotation.withPitch(TickRotationEvent.getLastRotation().pitch());
			return;
		}

		// we want nothing but the best
		BlockPos closestBlock = findClosestBlock();

		// loop through all relevant pitch angles to find a rotation that allows us to keep bridging
		for (int i = 90; i >= 20; i--) {
			HitResult hitResult = EntityUtils.getMouseOver(new Rotation(TickRotationEvent.clientRotation.yaw(), i));
			if (hitResult instanceof BlockHitResult blockHitResult
					&& isBlockHitResultValid(blockHitResult)
					&& blockHitResult.getBlockPos().equals(closestBlock)
					&& blockHitResult.getBlockPos().relative(blockHitResult.getDirection()).getY() <= p().getY()-1
			) {
				tickRotationEvent.rotation = new Rotation(TickRotationEvent.clientRotation.yaw(), i);
				return;
			}
		}
	}

	// See EntityMixin.onMoveCheckIfBackOffEdge (great naming, im a pro)
}
