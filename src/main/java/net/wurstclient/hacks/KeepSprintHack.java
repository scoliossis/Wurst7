package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"sprint", "slowdown", "attack"})
public final class KeepSprintHack extends Hack
{
    private final SliderSetting reduceTicks =
            new SliderSetting("Reduce Ticks", "Amount of ticks to not keepsprint for after being hit to allow for kb reducing.",
                    5, 0, 10, 1, SliderSetting.ValueDisplay.INTEGER.withSuffix("ticks"));

    public final CheckboxSetting bypassHypixel =
            new CheckboxSetting("Bypass Hypixel", """
                    Uses magic to bypass hypixel's horrible anticheat
                    
                    Added on July 26th 2026, Found May 1st 2026
                    """, true);

    public KeepSprintHack()
    {
        super("KeepSprint");
        setCategory(Category.MOVEMENT);

        addSetting(reduceTicks);
        addSetting(bypassHypixel);
    }

    public boolean shouldKeepsprint() {
        return this.isEnabled()
                && !p().horizontalCollision
                && p().isSprinting()
                && p().input.getMoveVector().length() > 1e-5F
                && p().zza > 0 // forwardSpeed is named this in mojmaps for wtv reason
                && p().hurtTime <= 10 - reduceTicks.getValueI();
    }

    // See PlayerMixin
}