package net.wurstclient.mixin;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin extends Avatar {
    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void onCauseExtraKnockbackSetSprinting(Player instance, boolean b) {
        if (this.is(WurstClient.p()) && WurstClient.INSTANCE.getHax().keepSprintHack.shouldKeepsprint()) return;
        instance.setSprinting(b);
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void onCauseExtraKnockbackSlowDown(Player instance, Vec3 vec3) {
        if (this.is(WurstClient.p()) && WurstClient.INSTANCE.getHax().keepSprintHack.shouldKeepsprint()) return;
        instance.setDeltaMovement(vec3);
    }
}