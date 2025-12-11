package silly.homak.usables.common.barrier;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import silly.homak.usables.UsablesMain;
import silly.homak.usables.client.TickSchedulerClient;
import silly.homak.usables.client.render.LIBRARY_CubeRenderer;
import silly.homak.usables.client.render.LIBRARY_QuadRenderer;
import silly.homak.usables.common.TickSchedulerServer;
import silly.homak.usables.custom.item.PocketBarrierItem;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.world.behaviors.components.LodestoneBehaviorComponent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CollisionBoxManagerClient {
    public static final List<CollisionBox> CLIENT_BOXES = new ArrayList<>();
    public static final Map<CollisionBox, Boolean> PLAYER_INSIDE_STATUS = new HashMap<>();
    private static final Random RANDOM = new Random();

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
        Vec3d center = pos.add(0, 1, 0);
        float barrierScale = PocketBarrierItem.BARRIER_SCALE;

        LIBRARY_CubeRenderer.schedule(center,
                barrierScale,
                barrierScale,
                barrierScale,
                Vec3d.ZERO, 1f,
                Identifier.of(UsablesMain.MOD_ID, "textures/effect/pocket_barrier.png"),
                25 * 20,
                true, 25 * 60 - 10,
                true, 25 * 60 - 10, -0.25f,
                0.75f);

        createCubeEdgeParticles(client, center, barrierScale);
    }

    private static void createCubeEdgeParticles(MinecraftClient client, Vec3d center, float cubeSize) {
        float halfSize = cubeSize / 2.0f;
        Vec3d[] vertices = {
                new Vec3d(center.x - halfSize, center.y - halfSize, center.z - halfSize), // 0: -X, -Y, -Z
                new Vec3d(center.x + halfSize, center.y - halfSize, center.z - halfSize), // 1: +X, -Y, -Z
                new Vec3d(center.x + halfSize, center.y - halfSize, center.z + halfSize), // 2: +X, -Y, +Z
                new Vec3d(center.x - halfSize, center.y - halfSize, center.z + halfSize), // 3: -X, -Y, +Z

                new Vec3d(center.x - halfSize, center.y + halfSize, center.z - halfSize), // 4: -X, +Y, -Z
                new Vec3d(center.x + halfSize, center.y + halfSize, center.z - halfSize), // 5: +X, +Y, -Z
                new Vec3d(center.x + halfSize, center.y + halfSize, center.z + halfSize), // 6: +X, +Y, +Z
                new Vec3d(center.x - halfSize, center.y + halfSize, center.z + halfSize)  // 7: -X, +Y, +Z
        };

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        // Particle parameters
        int particlesPerEdge = 20;
        int lifetime = 35 * 20;
        float particleSpeed = 0.1f;

        var edgeBuilder = WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                .enableNoClip()
                .setLifetime(lifetime)
                .setColorData(ColorParticleData.create(
                        new Color(220, 23, 85),
                        new Color(141, 14, 56)).build())
                .setGravityStrength(0f)
                .setScaleData(GenericParticleData.create(0.35f, 0).build())
                .setSpinData(SpinParticleData.create(RANDOM.nextFloat(0.01f, 0.03f), RANDOM.nextFloat(0.02f, 0.04f), RANDOM.nextFloat(0.01f, 0.03f)).build())
                .setTransparencyData(GenericParticleData.create(0.9f, 0.7f, 0.4f).build());

        for (int[] edge : edges) {
            Vec3d start = vertices[edge[0]];
            Vec3d end = vertices[edge[1]];
            Vec3d direction = end.subtract(start).normalize();

            for (int i = 0; i < particlesPerEdge; i++) {
                float t = i / (float)(particlesPerEdge - 1);

                Vec3d position = start.add(end.subtract(start).multiply(t));

                double velocityMagnitude = particleSpeed * (0.8f + RANDOM.nextFloat() * 0.4f);
                Vec3d velocity = direction.multiply(velocityMagnitude);

                Vec3d perpVelocity = calculatePerpendicularVelocity(direction, (float) (velocityMagnitude * 0.2f));

                edgeBuilder.spawn(client.world, position.x, position.y, position.z);
            }
        }
        createEdgeSweepEffect(client, vertices, edges, cubeSize);
    }

    private static Vec3d calculatePerpendicularVelocity(Vec3d direction, float magnitude) {
        Vec3d perp;
        if (Math.abs(direction.x) < 0.9) {
            perp = new Vec3d(1, 0, 0).crossProduct(direction).normalize();
        } else {
            perp = new Vec3d(0, 1, 0).crossProduct(direction).normalize();
        }

        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        Vec3d rotatedPerp = rotateAroundAxis(perp, direction, angle);

        return rotatedPerp.multiply(magnitude * (0.8 + RANDOM.nextDouble() * 0.4));
    }

    private static Vec3d rotateAroundAxis(Vec3d vector, Vec3d axis, double angle) {
        axis = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        Vec3d rotated = vector.multiply(cos)
                .add(axis.crossProduct(vector).multiply(sin))
                .add(axis.multiply(axis.dotProduct(vector) * (1 - cos)));

        return rotated;
    }

    private static void createEdgeSweepEffect(MinecraftClient client, Vec3d[] vertices, int[][] edges, float cubeSize) {

        var sweepBuilder = WorldParticleBuilder.create(LodestoneParticleRegistry.WISP_PARTICLE)
                .enableNoClip()
                .setLifetime(80)
                .setColorData(ColorParticleData.create(
                        new Color(255, 200, 220),
                        new Color(255, 150, 180)).build())
                .setGravityStrength(0f)
                .setScaleData(GenericParticleData.create(0.25f, 0.15f).build())
                .setTransparencyData(GenericParticleData.create(0.8f, 0.6f, 0.8f).build());

        TickSchedulerClient.scheduleRepeating(0, tick -> {
            if (tick >= edges.length * 10) return;
            int edgeIndex = (tick / 10) % edges.length;
            int positionInEdge = tick % 10;

            int[] edge = edges[edgeIndex];
            Vec3d start = vertices[edge[0]];
            Vec3d end = vertices[edge[1]];

            float t = positionInEdge / 9.0f;
            Vec3d position = start.add(end.subtract(start).multiply(t));

            Vec3d direction = end.subtract(start).normalize();

            sweepBuilder
                    .addMotion(direction.x * 0.05, direction.y * 0.05, direction.z * 0.05)
                    .spawn(client.world, position.x, position.y, position.z);

            if (tick % 2 == 0) {
                var trailBuilder = WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                        .enableNoClip()
                        .setLifetime(20)
                        .setColorData(ColorParticleData.create(
                                new Color(255, 180, 200),
                                new Color(230, 120, 160)).build())
                        .setGravityStrength(0f)
                        .setScaleData(GenericParticleData.create(0.1f, 0).build());

                for (int i = 0; i < 2; i++) {
                    Vec3d trailPos = position.subtract(direction.multiply(0.3 * i));
                    trailBuilder
                            .addMotion(
                                    RANDOM.nextDouble(-0.01, 0.01),
                                    RANDOM.nextDouble(-0.01, 0.01),
                                    RANDOM.nextDouble(-0.01, 0.01)
                            )
                            .spawn(client.world, trailPos.x, trailPos.y, trailPos.z);
                }
            }
        });
    }

    public static void quadEffect(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        Vector3f vector3f = buf.readVector3f();
        Vec3d pos = new Vec3d(vector3f);
        LIBRARY_QuadRenderer.schedule(pos.add(0, 0.01, 0), 1, 1,
                new Vec3d(90, 0, 0), 2.5f,
                Identifier.of(UsablesMain.MOD_ID, "textures/effect/pocket_barrier_circle.png"),
                10,
                true,0,
                true, 0, 8f,
                1f);
        var builder = WorldParticleBuilder.create(LodestoneParticleRegistry.SPARKLE_PARTICLE)
                .disableNoClip()
                .setColorData(ColorParticleData.create(
                        new Color(255 - 50, 0, 79 - 50),
                        new Color(118 - 50, 0, 36 - 30)).build())
                .setGravityStrength(0f)
                .setLifetime(20);
        TickSchedulerClient.scheduleRepeating(10, i -> {
            for (int j = 0; j < 4; j++) {
                builder.addMotion(RANDOM.nextDouble(-0.1, 0.1), 0.5, RANDOM.nextDouble(-0.1, 0.1));
                builder.spawn(client.world, pos.x, pos.y + RANDOM.nextDouble(-1, 1), pos.z);
            }
        });
    }

    public static void onAdd(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        CollisionBox box = PacketBoxUtil.readBox(buf);
        client.execute(() -> {
            CLIENT_BOXES.add(box);
            PLAYER_INSIDE_STATUS.put(box, false);
        });
    }

    public static void onRemove(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        CollisionBox box = PacketBoxUtil.readBox(buf);
        client.execute(() -> {
            CLIENT_BOXES.removeIf(b -> boxesEqual(b, box));
            PLAYER_INSIDE_STATUS.remove(box);
        });
    }

    public static void onSync(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int count = buf.readInt();
        List<CollisionBox> newList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CollisionBox box = PacketBoxUtil.readBox(buf);
            newList.add(box);
        }

        client.execute(() -> {
            CLIENT_BOXES.clear();
            CLIENT_BOXES.addAll(newList);

            Map<CollisionBox, Boolean> newStatus = new HashMap<>();
            for (CollisionBox box : newList) {
                newStatus.put(box, PLAYER_INSIDE_STATUS.getOrDefault(box, false));
            }
            PLAYER_INSIDE_STATUS.clear();
            PLAYER_INSIDE_STATUS.putAll(newStatus);
        });
    }

    public static void onInsideStatus(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        int count = buf.readInt();
        Map<CollisionBox, Boolean> updates = new HashMap<>();

        for (int i = 0; i < count; i++) {
            CollisionBox box = PacketBoxUtil.readBox(buf);
            boolean inside = buf.readBoolean();
            updates.put(box, inside);
        }

        client.execute(() -> {
            for (Map.Entry<CollisionBox, Boolean> entry : updates.entrySet()) {
                CollisionBox receivedBox = entry.getKey();
                boolean inside = entry.getValue();

                for (CollisionBox clientBox : CLIENT_BOXES) {
                    if (boxesEqual(clientBox, receivedBox)) {
                        PLAYER_INSIDE_STATUS.put(clientBox, inside);

                        if (inside) {
                            onPlayerEnteredBox(clientBox);
                        } else {
                            onPlayerExitedBox(clientBox);
                        }
                        break;
                    }
                }
            }
        });
    }

    private static void onPlayerEnteredBox(CollisionBox box) {
    }

    private static void onPlayerExitedBox(CollisionBox box) {
    }

    public static boolean boxesEqual(CollisionBox a, CollisionBox b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        return a.pos().equals(b.pos()) &&
                a.rot().equals(b.rot()) &&
                a.scale().equals(b.scale()) &&
                a.expireGameTime() == b.expireGameTime();
    }

    public static boolean isPlayerInsideBox(CollisionBox box) {
        return PLAYER_INSIDE_STATUS.getOrDefault(box, false);
    }

    public static Set<CollisionBox> getBoxesPlayerIsInside() {
        Set<CollisionBox> result = new HashSet<>();
        for (Map.Entry<CollisionBox, Boolean> entry : PLAYER_INSIDE_STATUS.entrySet()) {
            if (entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}