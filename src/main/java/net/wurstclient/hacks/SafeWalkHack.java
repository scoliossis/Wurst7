/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

// im so sorry for my differnet coding style, im not pull requesting this anyway, sorry alex.
@SearchTags({"safe walk", "SneakSafety", "sneak safety", "SpeedBridgeHelper",
	"speed bridge helper"})
public final class SafeWalkHack extends Hack implements TickRotationListener {
	public final CheckboxSetting requireSneakDown =
			new CheckboxSetting("Require Sneak Down", "Visibly sneak at edges.", false);
	public final CheckboxSetting requireBlocks =
			new CheckboxSetting("Require Holding Blocks", "Visibly sneak at edges.", false);
	public final CheckboxSetting requireHoldS =
			new CheckboxSetting("Require Holding S", "Visibly sneak at edges.", false);
	public final CheckboxSetting requireInteractDown =
			new CheckboxSetting("Require Interact Key Down", "Visibly sneak at edges.", false);
	public final CheckboxSetting requireLookingDown =
			new CheckboxSetting("Require Looking Down", "Visibly sneak at edges.", false);

	public SafeWalkHack() {
		super("SafeWalk");
		setCategory(Category.MOVEMENT);
		addSetting(requireSneakDown);

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

	private static boolean onEdgeOfBlock() {
		return MC.level.getBlockState(ScaffoldWalkHack.blockBelowPlayer()).isAir();
	}

	@Override
	public void onRotationEvent(TickRotationEvent tickRotationEvent) {
		//tickRotationEvent.rotation = new Rotation(0, 0);
	}

	// See EntityMixin.onMoveCheckIfBackOffEdge (great naming, im a pro)
}
