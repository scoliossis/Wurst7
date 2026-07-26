package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

public final class FilterTeamsSetting extends EntityFilterCheckbox
{
    public FilterTeamsSetting(String description, boolean checked)
    {
        super("Filter Teams", description, checked);
    }

    @Override
    public boolean test(Entity e)
    {
        if(!(e instanceof Player pe) || WurstClient.p() == null)
            return true;

        return pe.getTeam() != WurstClient.p().getTeam();
    }

    public static FilterTeamsSetting genericCombat(boolean checked)
    {
        return new FilterTeamsSetting(
                "Filters out team mates.",
                checked);
    }

    public static FilterTeamsSetting genericVision(boolean checked)
    {
        return new FilterTeamsSetting(
                "Filters out team mates.",
                checked);
    }
}
