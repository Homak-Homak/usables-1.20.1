package silly.homak.usables.common.barrier;

import net.minecraft.util.math.Vec3d;

public record CollisionBox(Vec3d pos, Vec3d rot, Vec3d scale, long expireGameTime) {}


