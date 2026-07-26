package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"sprint", "slowdown", "attack"})
public final class KeepSprintHack extends Hack
{
    private final SliderSetting reduceTicks =
            new SliderSetting("Reduce Ticks", "Amount of ticks to not keepsprint for after being hit to allow for kb reducing.",
                    5, 0, 10, 1, SliderSetting.ValueDisplay.INTEGER.withSuffix("ticks"));

    public KeepSprintHack()
    {
        super("KeepSprint");
        setCategory(Category.MOVEMENT);
    }

    public boolean shouldKeepsprint() {
        return this.isEnabled()
                && !p().horizontalCollision
                && p().isSprinting()
                && p().hurtTime <= 10 - reduceTicks.getValueI();
    }

    // See PlayerMixin
}