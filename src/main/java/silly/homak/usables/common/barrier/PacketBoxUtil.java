package silly.homak.usables.common.barrier;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public class PacketBoxUtil {
    public static void writeBox(PacketByteBuf buf, CollisionBox box) {
        writeVec3d(buf, box.pos());
        writeVec3d(buf, box.rot());
        writeVec3d(buf, box.scale());
        buf.writeLong(box.expireGameTime());
    }

    public static CollisionBox readBox(PacketByteBuf buf) {
        Vec3d pos = readVec3d(buf);
        Vec3d rot = readVec3d(buf);
        Vec3d scale = readVec3d(buf);
        long expire = buf.readLong();
        return new CollisionBox(pos, rot, scale, expire);
    }

    public static void writeVec3d(PacketByteBuf buf, Vec3d v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    public static Vec3d readVec3d(PacketByteBuf buf) {
        return new Vec3d(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }
}