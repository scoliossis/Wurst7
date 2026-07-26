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
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlinkUtil;

@DontSaveState
@SearchTags({"LagSwitch", "lag switch"})
public final class BlinkHack extends Hack
	implements PacketInputListener, PacketOutputListener, UpdateListener
{
	private final CheckboxSetting blinkOutbound = new CheckboxSetting(
			"Blink Outbound",
			"Pauses sending packets to the server.",
			true);

	private final CheckboxSetting blinkInbound = new CheckboxSetting(
			"Blink Inbound",
			"Pauses receiving packets from the server.",
			true);


	public BlinkHack()
	{
		super("Blink");
		setCategory(Category.MOVEMENT);

		addSetting(blinkOutbound);
		addSetting(blinkInbound);

		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
	}

	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		BlinkUtil.pushBlink(blinkOutbound.isChecked(), blinkInbound.isChecked(), this.getClass());
	}

	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		BlinkUtil.popBlink(blinkOutbound.isChecked(), blinkInbound.isChecked(), this.getClass());
	}

	@Override
	public void onSentPacket(PacketOutputEvent event) {
		if (!BlinkUtil.isBlinking(true, false)) return;

		BlinkUtil.OUTGOING_BLINK.getLast().packets().add(event.getPacket());
		event.cancel();
	}

	@Override
	public void onReceivedPacket(PacketInputEvent event) {
		if (!BlinkUtil.isBlinking(false, true)) return;

		BlinkUtil.INCOMING_BLINK.getLast().packets().add(event.getPacket());
		event.cancel();
	}

	// checks if either of the sub settings are toggled
	@Override
	public void onUpdate() {
		if (blinkOutbound.isChecked() != BlinkUtil.isBlinking(true, false, this.getClass())) {
			if (blinkOutbound.isChecked()) BlinkUtil.pushBlink(true, false, this.getClass());
			else BlinkUtil.popBlink(true, false, this.getClass());
		}

		if (blinkInbound.isChecked() != BlinkUtil.isBlinking(false, true, this.getClass())) {
			if (blinkInbound.isChecked()) BlinkUtil.pushBlink(false, true, this.getClass());
			else BlinkUtil.popBlink(false, true, this.getClass());
		}

		if (MC.level == null) BlinkUtil.disableBlink(true, true);
	}
}
