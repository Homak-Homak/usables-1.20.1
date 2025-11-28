package silly.homak.usables.common.barrier;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import silly.homak.usables.server.PacketHandler;

import java.util.ArrayList;
import java.util.List;

public class CollisionBoxManagerServer {
    public static final List<CollisionBox> BOXES = new ArrayList<>();

    public static void addBox(ServerWorld world, Vec3d pos, Vec3d rot, Vec3d scale, int durationTicks) {
        long expire = world.getTime() + durationTicks;
        CollisionBox box = new CollisionBox(pos, rot, scale, expire);
        BOXES.add(box);

        PacketHandler.sendAddBox(world, box);
    }

    public static void tick(ServerWorld world) {
        long time = world.getTime();

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

            for (CollisionBox box : BOXES) {
                newMove = CollisionUtil.applyCollision(e.getPos(), newMove, box);
            }

            if (!newMove.equals(move)) {
                e.setVelocity(newMove);
            }
        }
    }
}

