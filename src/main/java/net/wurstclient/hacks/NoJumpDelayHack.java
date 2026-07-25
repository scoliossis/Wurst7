// scale made this module, so no official wurst copyright!
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"speed", "bhop", "bunny hop", "hop", "stairs", "jump"})
public final class NoJumpDelayHack extends Hack
{
    public final SliderSetting jumpDelay =
            new SliderSetting("Jump Delay", 1, 1, 10, 1, SliderSetting.ValueDisplay.INTEGER);

    public NoJumpDelayHack()
    {
        super("NoJumpDelay");
        setCategory(Category.MOVEMENT);

        addSetting(jumpDelay);
    }

    // See net.wurstclient.mixin.LivingEntityMixin.onAiStep
}

