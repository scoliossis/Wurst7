package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

/*
MC.getConnection().getOnlinePlayerIds().stream()
				.map(uuid -> MC.level.getEntity(uuid))
				.filter(entity -> entity != null && entity.isAlive() && entity != p())
 */
public final class FilterBotsSetting extends EntityFilterCheckbox
{
    public FilterBotsSetting(String description, boolean checked)
    {
        super("Filter Bots", description, checked);
    }

    @Override
    public boolean test(Entity e)
    {
        if(!(e instanceof Player pe) || WurstClient.MC.getConnection() == null)
            return true;

        return WurstClient.MC.getConnection().getOnlinePlayerIds().contains(pe.getUUID());
    }

    public static FilterBotsSetting genericCombat(boolean checked)
    {
        return new FilterBotsSetting(
                "Filters out server bots pretending to be players.",
                checked);
    }

    public static FilterBotsSetting genericVision(boolean checked)
    {
        return new FilterBotsSetting(
                "Filters out server bots pretending to be players.",
                checked);
    }
}
