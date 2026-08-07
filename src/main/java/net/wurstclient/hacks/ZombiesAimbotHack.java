package net.wurstclient.hacks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.C;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RaytraceUtil;
import net.wurstclient.util.RenderUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// todo: finish :sob:
@SearchTags({"aimbot", "god bypass tech"})
public class ZombiesAimbotHack extends Hack implements RenderListener, LeftClickListener, RightClickListener, PacketInputListener {
    private final CheckboxSetting straightLine = new CheckboxSetting(
            "Straight Line",
            "Color of the trajectory when it doesn't hit anything.",
            true);

    private final ColorSetting straightLineColour = new ColorSetting(
            "Straight Line Colour",
            new Color(220, 103, 238));

    private final SliderSetting shotTimeout = new SliderSetting(
            "Shot Timeout",
            "",
            100,
            0,
            100000,
            100,
            SliderSetting.ValueDisplay.INTEGER.withSuffix("ms"));

    public ZombiesAimbotHack() {
        super("ZombiesAimbot");

        addSetting(shotTimeout);
        addSetting(straightLineColour);
        addSetting(straightLine);
        EVENTS.add(RightClickListener.class, this);
        EVENTS.add(LeftClickListener.class, this);
        EVENTS.add(PacketInputListener.class, this);
    }

    private final ArrayList<LastShot> PREVIOUS_SHOTS = new ArrayList<>();
    private static final HashMap<Integer, Gun> GUNS = new HashMap<>();

    @Override
    protected void onEnable()
    {
        EVENTS.add(RenderListener.class, this);
    }

    @Override
    protected void onDisable()
    {
        EVENTS.remove(RenderListener.class, this);
    }

    @Override
    public void onRender(PoseStack matrixStack, float partialTicks)
    {
        for (LastShot lastShot : PREVIOUS_SHOTS.toArray(new LastShot[0])) {
            if (lastShot == null || !straightLine.isChecked()) return;

            RenderUtils.drawLine(matrixStack, lastShot.path().nodes().getFirst().hitVec(), lastShot.path().nodes().getLast().hitVec(), straightLineColour.getColorI(), false);
            RenderUtils.drawSolidBox(
                    matrixStack,
                    new AABB(lastShot.path().nodes().getLast().hitVec().subtract(0.1), lastShot.path().nodes().getLast().hitVec().add(0.1)),
                    new Color(0, 255, 33).getRGB(),
                    false
            );

            for (RaytraceUtil.EntityCollision entityCollision : lastShot.path().hitEntities()) {
                RenderUtils.drawSolidBox(
                        matrixStack,
                        new AABB(entityCollision.hitVec().subtract(0.1), entityCollision.hitVec().add(0.1)),
                        straightLineColour.getColorI(),
                        false
                );
            }

            if (System.currentTimeMillis() - lastShot.time < shotTimeout.getValue()) continue;
            PREVIOUS_SHOTS.remove(lastShot);
        }
    }

    @Override
    public void onRightClick(RightClickEvent event) {
        int slot = C.p().getInventory().getSelectedSlot();
        Gun gun = GUNS.get(slot);
        if (!canShoot(gun)) return;

        double yaw = Math.toRadians(C.p().getYRot());
        double pitch = Math.toRadians(C.p().getXRot());
        double cosPitch = Math.cos(pitch);

        Vec3 expandSize = new Vec3(1, 1, 1).multiply(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch
        ).normalize().scale(256);

        Vec3 playerPos = C.p().getEyePosition();
        Vec3 expandedPos = playerPos.add(expandSize);

        PREVIOUS_SHOTS.add(new LastShot(
                RaytraceUtil.traverseVoxels(playerPos, expandedPos, raytraceNodes -> {
                    // hypixel probably doesnt have a limit, but who car
                    if (raytraceNodes.size() > 200) return false;

                    // if its air, we are STILL valid
                    VoxelShape lastBlockBoundingBox = raytraceNodes.getLast().blockState().getCollisionShape(C.w(), raytraceNodes.getLast().pos());
                    if (lastBlockBoundingBox.isEmpty()) return true;

                    // in zombies, the bullet will travel through walls forever if it ever hits a non solid block, meaining, in prison and bad blood you can fully shoot through walls
                    for (RaytraceUtil.RaytraceNode node : raytraceNodes) {
                        VoxelShape collisionShape = node.blockState().getCollisionShape(C.w(), node.pos());

                        if (node.blockState().isAir() || node.blockState().getBlock() == Blocks.BARRIER) continue;

                        // check if the first block we collide with is solid, or not, if it isnt, we can shoot through walls forever
                        return !(node.blockState().getBlock() instanceof StainedGlassPaneBlock)
                                && (collisionShape.isEmpty()
                                || collisionShape.toAabbs().size() > 1
                                || !collisionShape.bounds().equals(new AABB(0, 0, 0, 1, 1, 1)));
                    }

                    return false;
                }),
                System.currentTimeMillis())
        );

        gun.updateLastShot(slot);
        if (gun.ammo() == 1) gun.startReloading(slot, true);
    }

    @Override
    public void onLeftClick(LeftClickEvent event) {
        int slot = C.p().getInventory().getSelectedSlot();
        Gun gun = GUNS.get(slot);
        if (gun == null || gun.ammo == gun.clipAmmo) return;

        gun.startReloading(slot, false);
    }

    @Override
    public void onReceivedPacket(PacketInputEvent event) {
        if (event.getPacket() instanceof ClientboundLoginPacket) {
            PREVIOUS_SHOTS.clear();
            GUNS.clear();
            return;
        }

        if (event.getPacket() instanceof ClientboundSystemChatPacket chatPacket) {
            for (PowerUps powerUp : PowerUps.values()) {
                if (powerUp.pattern.matcher(chatPacket.content().getString()).matches()) {
                    powerUp.action.run();
                    break; 
                }
            }
            return;
        }

        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket slotPacket) {
            Gun currentGunInSlot = GUNS.get(getServerSlot(slotPacket));

            // no point checking again <3 (except if u pick up quickfire wtv.)
            if (currentGunInSlot != null && currentGunInSlot.name().equals(slotPacket.getItem().getCustomName())) return;

            handleNewGun(slotPacket);
        }
    }

    private void handleNewGun(ClientboundContainerSetSlotPacket slotPacket) {
        List<Component> words = slotPacket.getItem().getTooltipLines(
                Item.TooltipContext.of(C.w()),
                C.p(),
                TooltipFlag.Default.ADVANCED
        );

        if (words.stream().noneMatch(component -> component.getString().equalsIgnoreCase("right-click to shoot."))) return;

        for (Component row : words) {
            ChatUtils.message(row.getString());
        }

        GUNS.put(getServerSlot(slotPacket), new Gun(
                slotPacket.getItem().getCustomName(),
                (int) getValue(words, TOTAL_AMMO_MATCHER),
                (int) getValue(words, CLIP_AMMO_MATCHER),
                slotPacket.getItem().count(),
                (int) getValue(words, DAMAGE_MATCHER),
                (int) (getValue(words, FIRE_RATE_MATCHER) * 1000) / 50,
                (getValue(words, RELOAD_MATCHER) * 1000) / 50,
                0,
                0
        ));
    }

    private double getValue(List<Component> tooltip, Pattern pattern) {
        return tooltip.stream()
                .map(component -> pattern.matcher(component.getString())).
                filter(Matcher::find)
                .mapToDouble(m -> Double.parseDouble(m.group(1)))
                .findFirst()
                .orElse(0);
    }

    private boolean canShoot(Gun gun) {
        return gun != null
                && C.tick - gun.lastShotTick > gun.getFireRate()
                && C.tick - gun.lastReloadingTick >= gun.reloadTime
                && gun.totalAmmo > 0;
    }

    private int getServerSlot(ClientboundContainerSetSlotPacket slotPacket) {
        return slotPacket.getSlot() - 36;
    }

    private enum PowerUps {
        MAX_AMMO(Pattern.compile("[a-zA-Z0-9_]+ activated Max Ammo!"), () -> {}),
        INSTA_KILL(Pattern.compile("[a-zA-Z0-9_]+ activated Insta Kill for 10s!"), () -> {});

        public final Pattern pattern;
        public final Runnable action;

        PowerUps(Pattern pattern, Runnable action) {
            this.pattern = pattern;
            this.action = action;
        }
    }

    private final Pattern DAMAGE_MATCHER = Pattern.compile(" ▪ Damage: (?:\\d+\\.\\d+ ➜ )?(\\d+\\.\\d+) HP");
    private final Pattern CLIP_AMMO_MATCHER = Pattern.compile(" ▪ Clip Ammo: (?:\\d+ ➜ )?(\\d+)");
    private final Pattern TOTAL_AMMO_MATCHER = Pattern.compile(" ▪ Ammo: (?:\\d+ ➜ )?(\\d+)");
    private final Pattern FIRE_RATE_MATCHER = Pattern.compile(" ▪ Fire Rate: (?:\\d+\\.\\d+ ➜ )?(\\d+\\.\\d+)s");
    private final Pattern RELOAD_MATCHER = Pattern.compile(" ▪ Reload: (?:\\d+\\.\\d+ ➜ )?(\\d+\\.\\d+)s");

    private record LastShot(RaytraceUtil.RaytracePath path, long time){}
    private record Gun(Component name, int totalAmmo, int clipAmmo, int ammo, int damage, int fireRate, double reloadTime, int lastShotTick, int lastReloadingTick){
        public static boolean hasQuickfire;
        public static boolean instantKillTick;

        public double getFireRate() {
            return fireRate * (hasQuickfire ? 0.75 : 1);
        }

        public void updateLastShot(int slot) {
            //ChatUtils.message("guess shot: " + C.tick);
            //ChatUtils.message((ammo-1) + " : " + clipAmmo + " : " + (totalAmmo-1));
            GUNS.put(slot, new Gun(name, totalAmmo-1, clipAmmo, ammo-1, damage, fireRate, reloadTime, C.tick, lastReloadingTick));
        }
        public void startReloading(int slot, boolean forceReload) {
            //ChatUtils.message("RELOAD : " + clipAmmo + " : " + totalAmmo);
            // hypixel actually decreases reload time by fireRate if the reload is forced, how cool
            GUNS.put(slot, new Gun(name, totalAmmo, clipAmmo, clipAmmo, damage, fireRate,  reloadTime, lastShotTick, C.tick - (forceReload ? fireRate : 0)));
        }
    }
}
