package silly.homak.usables.common.barrier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import java.util.*;

public class CollisionUtil {
    private static final Map<UUID, Boolean> PREVIOUS_INSIDE = new HashMap<>();

    public static Vec3d applyCollision(Vec3d position, Vec3d move, CollisionBox box) {

        Vec3d targetPos = position.add(move);
        Vec3d local = targetPos.subtract(box.pos());

        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        local = rotateZ(local, -rot.z);
        local = rotateX(local, -rot.x);
        local = rotateY(local, -rot.y);

        double hx = box.scale().x / 2.0;
        double hy = box.scale().y / 2.0;
        double hz = box.scale().z / 2.0;

        boolean inside =
                Math.abs(local.x) < hx &&
                        Math.abs(local.y) < hy &&
                        Math.abs(local.z) < hz;

        if (!inside) return move;

        double dx = hx - Math.abs(local.x);
        double dy = hy - Math.abs(local.y);
        double dz = hz - Math.abs(local.z);

        Vec3d push;

        if (dx < dy && dx < dz) {
            push = new Vec3d(Math.signum(local.x) * dx, 0, 0);
        } else if (dy < dz) {
            push = new Vec3d(0, Math.signum(local.y) * dy, 0);
        } else {
            push = new Vec3d(0, 0, Math.signum(local.z) * dz);
        }

        Vec3d worldPush = push;
        worldPush = rotateY(worldPush, rot.y);
        worldPush = rotateX(worldPush, rot.x);
        worldPush = rotateZ(worldPush, rot.z);

        return move.add(worldPush);
    }

    private static Vec3d rotateX(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x,
                v.y * c - v.z * s,
                v.y * s + v.z * c
        );
    }

    private static Vec3d rotateY(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x * c + v.z * s,
                v.y,
                -v.x * s + v.z * c
        );
    }

    private static Vec3d rotateZ(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x * c - v.y * s,
                v.x * s + v.y * c,
                v.z
        );
    }
}
