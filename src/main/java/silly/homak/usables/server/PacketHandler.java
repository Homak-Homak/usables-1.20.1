package silly.homak.usables.server;

import com.mojang.logging.plugins.QueueLogAppender;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import silly.homak.usables.UsablesMain;
import silly.homak.usables.common.barrier.CollisionBox;
import silly.homak.usables.common.barrier.CollisionBoxManagerClient;
import silly.homak.usables.common.barrier.CollisionBoxManagerServer;
import silly.homak.usables.common.barrier.PacketBoxUtil;

import java.util.ArrayList;
import java.util.List;

public class PacketHandler {
    public static final Identifier ADD = new Identifier(UsablesMain.MOD_ID, "add_box");
    public static final Identifier REMOVE = new Identifier(UsablesMain.MOD_ID, "remove_box");
    public static final Identifier SYNC = new Identifier(UsablesMain.MOD_ID, "sync_boxes");
    public static final Identifier CUBE_EFFECT = new Identifier(UsablesMain.MOD_ID, "cube_effect");
    public static final Identifier QUAD_EFFECT = new Identifier(UsablesMain.MOD_ID, "quad_effect");

    public static void registerPackets() {
        ClientPlayNetworking.registerGlobalReceiver(ADD, CollisionBoxManagerClient::onAdd);
        ClientPlayNetworking.registerGlobalReceiver(REMOVE, CollisionBoxManagerClient::onRemove);
        ClientPlayNetworking.registerGlobalReceiver(SYNC, CollisionBoxManagerClient::onSync);
        ClientPlayNetworking.registerGlobalReceiver(CUBE_EFFECT, CollisionBoxManagerClient::cubeEffect);
        ClientPlayNetworking.registerGlobalReceiver(QUAD_EFFECT, CollisionBoxManagerClient::quadEffect);
    }

    public static void sendCubeEffect(ServerWorld world, Vec3d pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVector3f(pos.toVector3f());
        PacketBoxUtil.writeVec3d(buf, pos);
            for (ServerPlayerEntity p : world.getPlayers()) {
                ServerPlayNetworking.send(p, CUBE_EFFECT, buf);
            }
    }

    public static void sendQuadEffect(ServerWorld world, Vec3d pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVector3f(pos.toVector3f());
        PacketBoxUtil.writeVec3d(buf, pos);
        for (ServerPlayerEntity p : world.getPlayers()) {
            ServerPlayNetworking.send(p, QUAD_EFFECT, buf);
        }
    }

    public static void sendAddBox(ServerWorld world, CollisionBox box) {
        PacketByteBuf buf = PacketByteBufs.create();
        PacketBoxUtil.writeBox(buf, box);

        for (ServerPlayerEntity p : world.getPlayers()) {
            ServerPlayNetworking.send(p, ADD, buf);
        }
    }

    public static void sendRemoveBox(ServerWorld world, CollisionBox box) {
        PacketByteBuf buf = PacketByteBufs.create();
        PacketBoxUtil.writeBox(buf, box);

        for (ServerPlayerEntity p : world.getPlayers()) {
            ServerPlayNetworking.send(p, REMOVE, buf);
        }
    }

    public static void sendFullSync(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeInt(CollisionBoxManagerServer.BOXES.size());
        for (CollisionBox b : CollisionBoxManagerServer.BOXES) {
            PacketBoxUtil.writeBox(buf, b);
        }

        ServerPlayNetworking.send(player, SYNC, buf);
    }
}

