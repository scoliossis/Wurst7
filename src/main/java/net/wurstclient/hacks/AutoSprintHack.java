/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.world.entity.player.Input;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.KeyboardInputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"auto sprint"})
public final class AutoSprintHack extends Hack implements KeyboardInputListener
{
	public AutoSprintHack()
	{
		super("AutoSprint");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(KeyboardInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(KeyboardInputListener.class, this);
	}

	@Override
	public void onKeyboardInputEvent(KeyboardInputEvent keyboardInputEvent) {
		keyboardInputEvent.input = new Input(
				keyboardInputEvent.input.forward(),
				keyboardInputEvent.input.backward(),
				keyboardInputEvent.input.left(),
				keyboardInputEvent.input.right(),
				keyboardInputEvent.input.jump(),
				keyboardInputEvent.input.shift(),
				true
		);
	}
}
