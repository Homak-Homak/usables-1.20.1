package silly.homak.usables.common.barrier;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import silly.homak.usables.server.PacketHandler;

import java.util.ArrayList;
import java.util.List;

public class CollisionBoxManagerServer {
    public static final List<CollisionBox> BOXES = new ArrayList<>();

    private static boolean midTickSystemInitialized = false;

    public static void addBox(ServerWorld world, Vec3d pos, Vec3d rot, Vec3d scale, int durationTicks) {
        long expire = world.getTime() + durationTicks;
        CollisionBox box = new CollisionBox(pos, rot, scale, expire);
        BOXES.add(box);

        PacketHandler.sendAddBox(world, box);

        initializeMidTickSystem();
    }

    public static void tick(ServerWorld world) {
        long time = world.getTime();

        MidTickCollisionManager.onWorldTickStart(world);

        BOXES.removeIf(box -> {
            if (time >= box.expireGameTime()) {
                PacketHandler.sendRemoveBox(world, box);
                return true;
            }
            return false;
        });

        for (Entity e : world.iterateEntities()) {
            Vec3d move = e.getVelocity();
            if (move.lengthSquared() < 1e-6) continue;

            Vec3d newMove = move;
            Vec3d pos = e.getPos();

            if (move.lengthSquared() > 4.0) {
                MidTickCollisionManager.registerForMidTickChecks(world, e);
            }

            // Apply standard collision detection
            boolean collided = false;
            for (CollisionBox box : BOXES) {
                Vec3d originalMove = newMove;
                newMove = CollisionUtil.applyCollision(pos, newMove, box);
                if (!newMove.equals(originalMove)) {
                    collided = true;
                }
            }

            if (!newMove.equals(move)) {
                e.setVelocity(newMove);
            }
        }
        MidTickCollisionManager.onWorldTickEnd(world);
    }

    public static void onEntityRemoved(ServerWorld world, Entity entity) {
        MidTickCollisionManager.onEntityRemoved(world, entity);
    }

    private static void initializeMidTickSystem() {
        if (!midTickSystemInitialized) {
            midTickSystemInitialized = true;
        }
    }

    // Method to be called from external tick scheduler
    public static void onMidTickPhase(ServerWorld world, int phase) {
        MidTickCollisionManager.performMidTickChecks(world, phase);
    }
}