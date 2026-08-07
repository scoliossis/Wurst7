/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.phys.BlockHitResult;
import net.wurstclient.C;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.TickRotationListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.EntityUtils;

@SearchTags({"fast place"})
public final class FastPlaceHack extends Hack implements UpdateListener
{
	public FastPlaceHack()
	{
		super("FastPlace");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check if ur holding a block and looking at a block
		if (ScaffoldWalkHack.isValidBlock(C.p().getInventory().getSelectedSlot())
		&&  EntityUtils.getMouseOver(TickRotationListener.TickRotationEvent.getLastRotation()) instanceof BlockHitResult blockHitResult
		&& SafeWalkHack.isBlockHitResultValid(blockHitResult)) {
			MC.rightClickDelay = 0;
		}
	}
}
