package net.wurstclient.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.C;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class RaytraceUtil {
    public record RaytracePath(ArrayList<RaytraceNode> nodes, ArrayList<EntityCollision> hitEntities) {}
    public record RaytraceNode(BlockPos pos, Vec3 hitVec, BlockState blockState) {}
    public record EntityCollision(Entity entity, Vec3 hitVec){}

    // thank you 112batman, goat 4eva
    // https://gist.github.com/Nik--/0beb2f5f2b93182e467db86223f6e1d2
    // Also see: https://github.com/cgyurgyik/fast-voxel-traversal-algorithm/blob/master/overview/FastVoxelTraversalOverview.md
    public static RaytracePath traverseVoxels(Vec3 start, Vec3 end, Predicate<ArrayList<RaytraceNode>> validBlock) {
        ArrayList<RaytraceNode> passedBlocks = new ArrayList<>();
        passedBlocks.add(new RaytraceNode(BlockPos.containing(start), start, C.w().getBlockState(BlockPos.containing(start))));

        ArrayList<EntityCollision> hitEntities = new ArrayList<>();

        int currVoxelX = (int) Math.floor(start.x);
        int currVoxelY = (int) Math.floor(start.y);
        int currVoxelZ = (int) Math.floor(start.z);
        final int lastVoxelX = (int) Math.floor(end.x);
        final int lastVoxelY = (int) Math.floor(end.y);
        final int lastVoxelZ = (int) Math.floor(end.z);
        final double diffX = end.x - start.x;
        final double diffY = end.y - start.y;
        final double diffZ = end.z - start.z;
        final int stepX = (int) Math.signum(diffX);
        final int stepY = (int) Math.signum(diffY);
        final int stepZ = (int) Math.signum(diffZ);

        final double tDeltaX = stepX == 0 ? Double.MAX_VALUE : (stepX / diffX);
        final double tDeltaY = stepY == 0 ? Double.MAX_VALUE : (stepY / diffY);
        final double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : (stepZ / diffZ);

        double tMaxX = stepX > 0 ? (tDeltaX * frac1(start.x)) : (tDeltaX * frac0(start.x));
        double tMaxY = stepY > 0 ? (tDeltaY * frac1(start.y)) : (tDeltaY * frac0(start.y));
        double tMaxZ = stepZ > 0 ? (tDeltaZ * frac1(start.z)) : (tDeltaZ * frac0(start.z));

        // distance inside the block when entering a new one
        double t;

        Vec3i v;
        int iterations = Math.abs(lastVoxelX - currVoxelX) + Math.abs(lastVoxelY - currVoxelY) + Math.abs(lastVoxelZ - currVoxelZ);
        while(iterations-- >= 0 && (tMaxX <= 1 || tMaxY <= 1 || tMaxZ <= 1)) {
            if(tMaxX < tMaxY) {
                if(tMaxX < tMaxZ) {
                    t = tMaxX;
                    currVoxelX += stepX;
                    v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    tMaxX += tDeltaX;
                }else {
                    t = tMaxZ;
                    currVoxelZ += stepZ;
                    v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    tMaxZ += tDeltaZ;
                }
            }else {
                if(tMaxY < tMaxZ) {
                    t = tMaxY;
                    currVoxelY += stepY;
                    v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    tMaxY += tDeltaY;
                }else {
                    t = tMaxZ;
                    currVoxelZ += stepZ;
                    v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    tMaxZ += tDeltaZ;
                }
            }

            BlockPos pos = new BlockPos(v);
            Vec3 hitVec = start.lerp(end, t);
            passedBlocks.add(new RaytraceNode(pos, hitVec, C.w().getBlockState(pos)));
            if (!validBlock.test(passedBlocks)) break;
        }
        return new RaytracePath(passedBlocks, getEntityCollisions(start, passedBlocks.getLast().hitVec()));
    }

    private static ArrayList<EntityCollision> getEntityCollisions(Vec3 start, Vec3 end) {
        ArrayList<EntityCollision> collisions = new ArrayList<>();
        AABB searchBox = new AABB(start, end).inflate(1.0); // small margin for entity hitbox size
        List<Entity> candidates = C.w().getEntities(C.p(), searchBox);

        for (Entity entity : candidates) {
            AABB hitbox = entity.getBoundingBox();
            Optional<Vec3> clip = hitbox.clip(start, end);
            clip.ifPresent(hit -> collisions.add(new EntityCollision(entity, hit)));
        }

        collisions.sort(Comparator.comparingDouble(e -> start.distanceTo(e.hitVec())));
        return collisions;
    }

    private static double frac0(double val) {
        return val - Math.floor(val);
    }
    private static double frac1(double val) {
        return 1 - val + Math.floor(val);
    }
}
