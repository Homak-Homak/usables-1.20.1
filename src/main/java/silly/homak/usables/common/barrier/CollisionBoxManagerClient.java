package silly.homak.usables.common.barrier;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import silly.homak.usables.UsablesMain;
import silly.homak.usables.client.render.LIBRARY_CubeRenderer;
import silly.homak.usables.client.render.LIBRARY_QuadRenderer;

import java.util.ArrayList;
import java.util.List;

public class CollisionBoxManagerClient {
    public static final List<CollisionBox> CLIENT_BOXES = new ArrayList<>();

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Vec3d move = mc.player.getVelocity();
        if (move.lengthSquared() == 0) return;

        Vec3d newMove = move;
        Vec3d pos = mc.player.getPos();

        for (CollisionBox b : CLIENT_BOXES) {
            newMove = CollisionUtil.applyCollision(pos, newMove, b);
        }

        mc.player.setVelocity(newMove);
    }

    public static void cubeEffect(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        Vector3f vector3f = buf.readVector3f();
        Vec3d pos = new Vec3d(vector3f);
        LIBRARY_CubeRenderer.schedule(pos.add(0, 1, 0), 15, 15, 15,
                Vec3d.ZERO, 1f,
                Identifier.of(UsablesMain.MOD_ID, "textures/effect/pocket_barrier.png"),
                15 * 60,
                true, 15 * 60 - 10,
                true, 15 * 60 - 10, -0.25f,
                0.85f);
    }
    public static void quadEffect(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        Vector3f vector3f = buf.readVector3f();
        Vec3d pos = new Vec3d(vector3f);
        LIBRARY_QuadRenderer.schedule(pos.add(0, 0.01, 0), 1, 1,
                new Vec3d(90, 0, 0), 2.5f,
                Identifier.of(UsablesMain.MOD_ID, "textures/effect/pocket_barrier_circle.png"),
                30,
                true, 10,
                true, 0, 8f,
                1f);
    }

    public static void onAdd(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        CollisionBox box = PacketBoxUtil.readBox(buf);
        client.execute(() -> CollisionBoxManagerClient.CLIENT_BOXES.add(box));
    }

    public static void onRemove(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        CollisionBox box = PacketBoxUtil.readBox(buf);
        client.execute(() -> CollisionBoxManagerClient.CLIENT_BOXES.removeIf(b -> boxesEqual(b, box)));
    }

    public static void onSync(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int count = buf.readInt();
        List<CollisionBox> newList = new ArrayList<>();
        for (int i = 0; i < count; i++) newList.add(PacketBoxUtil.readBox(buf));

        client.execute(() -> {
            CLIENT_BOXES.clear();
            CLIENT_BOXES.addAll(newList);
        });
    }

    public static boolean boxesEqual(CollisionBox a, CollisionBox b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        return a.pos().equals(b.pos()) &&
                a.rot().equals(b.rot()) &&
                a.scale().equals(b.scale()) &&
                a.expireGameTime() == b.expireGameTime();
    }

}

