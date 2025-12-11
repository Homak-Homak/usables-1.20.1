package silly.homak.usables.common.barrier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import silly.homak.usables.server.PacketHandler;

import java.util.*;

public class CollisionBoxManagerServer {
    public static final List<CollisionBox> BOXES = new ArrayList<>();
    private static final Map<UUID, Set<CollisionBox>> PLAYER_INSIDE_BOXES = new HashMap<>();
    private static final Map<UUID, Set<CollisionBox>> LAST_SENT_STATUS = new HashMap<>();
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 2;

    private static boolean midTickSystemInitialized = false;

    public static void addBox(ServerWorld world, Vec3d pos, Vec3d rot, Vec3d scale, int durationTicks) {
        long expire = world.getTime() + durationTicks;
        CollisionBox box = new CollisionBox(pos, rot, scale, expire);
        BOXES.add(box);

        PacketHandler.sendAddBox(world, box);

        pushEntitiesInsideWallsIntoHollowSpace(world, box);

        initializeMidTickSystem();
    }

    private static void pushEntitiesInsideWallsIntoHollowSpace(ServerWorld world, CollisionBox box) {
        for (Entity entity : world.iterateEntities()) {
            if (entity == null || !entity.isAlive()) continue;

            Vec3d entityPos = entity.getPos();

            if (!isEntityInCollisionBox(entityPos, box)) {
                continue;
            }

            if (!isEntityInHollowSpace(entityPos, box)) {
                Vec3d pushForce = calculatePushForceIntoHollowSpace(entityPos, box);

                if (pushForce.lengthSquared() > 0) {
                    Vec3d currentVelocity = entity.getVelocity();
                    entity.setVelocity(currentVelocity.add(pushForce.multiply(1.5)));
                    Vec3d adjustedPos = adjustPositionToHollowSpaceEdge(entityPos, box);
                    entity.setPosition(adjustedPos);
                }
            }
        }
    }

    private static boolean isEntityInCollisionBox(Vec3d position, CollisionBox box) {
        Vec3d localPos = position.subtract(box.pos());
        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = CollisionUtil.rotateZ(localPos, -rot.z);
        localPos = CollisionUtil.rotateX(localPos, -rot.x);
        localPos = CollisionUtil.rotateY(localPos, -rot.y);

        double halfX = box.scale().x / 2.0;
        double halfY = box.scale().y / 2.0;
        double halfZ = box.scale().z / 2.0;

        return Math.abs(localPos.x) <= halfX &&
                Math.abs(localPos.y) <= halfY &&
                Math.abs(localPos.z) <= halfZ;
    }

    private static Vec3d calculatePushForceIntoHollowSpace(Vec3d position, CollisionBox box) {
        Vec3d localPos = position.subtract(box.pos());
        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = CollisionUtil.rotateZ(localPos, -rot.z);
        localPos = CollisionUtil.rotateX(localPos, -rot.x);
        localPos = CollisionUtil.rotateY(localPos, -rot.y);

        double halfX = box.scale().x / 2.0;
        double halfY = box.scale().y / 2.0;
        double halfZ = box.scale().z / 2.0;

        double wallThickness = 0.5;
        double innerHalfX = Math.max(0, halfX - wallThickness);
        double innerHalfY = Math.max(0, halfY - wallThickness);
        double innerHalfZ = Math.max(0, halfZ - wallThickness);

        if (Math.abs(localPos.x) < innerHalfX &&
                Math.abs(localPos.y) < innerHalfY &&
                Math.abs(localPos.z) < innerHalfZ) {
            return Vec3d.ZERO;
        }

        Vec3d toCenter = localPos.multiply(-1);
        double toCenterLength = toCenter.length();

        if (toCenterLength > 0) {
            Vec3d pushDirection = toCenter.multiply(1.0 / toCenterLength);

            Vec3d worldPushDirection = pushDirection;
            worldPushDirection = CollisionUtil.rotateY(worldPushDirection, rot.y);
            worldPushDirection = CollisionUtil.rotateX(worldPushDirection, rot.x);
            worldPushDirection = CollisionUtil.rotateZ(worldPushDirection, rot.z);

            return worldPushDirection.multiply(0.3);
        }

        return Vec3d.ZERO;
    }

    private static boolean isEntityInHollowSpace(Vec3d position, CollisionBox box) {
        Vec3d localPos = position.subtract(box.pos());
        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = CollisionUtil.rotateZ(localPos, -rot.z);
        localPos = CollisionUtil.rotateX(localPos, -rot.x);
        localPos = CollisionUtil.rotateY(localPos, -rot.y);

        double wallThickness = 0.5;
        double innerHalfX = Math.max(0, box.scale().x / 2.0 - wallThickness);
        double innerHalfY = Math.max(0, box.scale().y / 2.0 - wallThickness);
        double innerHalfZ = Math.max(0, box.scale().z / 2.0 - wallThickness);

        return Math.abs(localPos.x) < innerHalfX &&
                Math.abs(localPos.y) < innerHalfY &&
                Math.abs(localPos.z) < innerHalfZ;
    }

    private static Vec3d adjustPositionToHollowSpaceEdge(Vec3d position, CollisionBox box) {
        Vec3d localPos = position.subtract(box.pos());
        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = CollisionUtil.rotateZ(localPos, -rot.z);
        localPos = CollisionUtil.rotateX(localPos, -rot.x);
        localPos = CollisionUtil.rotateY(localPos, -rot.y);

        double halfX = box.scale().x / 2.0;
        double halfY = box.scale().y / 2.0;
        double halfZ = box.scale().z / 2.0;

        double wallThickness = 0.5;
        double innerHalfX = Math.max(0, halfX - wallThickness);
        double innerHalfY = Math.max(0, halfY - wallThickness);
        double innerHalfZ = Math.max(0, halfZ - wallThickness);

        double adjustedX = localPos.x;
        double adjustedY = localPos.y;
        double adjustedZ = localPos.z;

        if (Math.abs(localPos.x) > innerHalfX) {
            if (localPos.x > 0) {
                adjustedX = innerHalfX - 0.01;
            } else {
                adjustedX = -innerHalfX + 0.01;
            }
        }

        if (Math.abs(localPos.y) > innerHalfY) {
            if (localPos.y > 0) {
                adjustedY = innerHalfY - 0.01;
            } else {
                adjustedY = -innerHalfY + 0.01;
            }
        }

        if (Math.abs(localPos.z) > innerHalfZ) {
            if (localPos.z > 0) {
                adjustedZ = innerHalfZ - 0.01;
            } else {
                adjustedZ = -innerHalfZ + 0.01;
            }
        }

        Vec3d adjustedLocalPos = new Vec3d(adjustedX, adjustedY, adjustedZ);

        Vec3d worldPos = adjustedLocalPos;
        worldPos = CollisionUtil.rotateY(worldPos, rot.y);
        worldPos = CollisionUtil.rotateX(worldPos, rot.x);
        worldPos = CollisionUtil.rotateZ(worldPos, rot.z);

        return worldPos.add(box.pos());
    }

    public static void tick(ServerWorld world) {
        long time = world.getTime();

        MidTickCollisionManager.onWorldTickStart(world);

        // Remove expired boxes
        BOXES.removeIf(box -> {
            if (time >= box.expireGameTime()) {
                box.clearPlayersInside();
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

            boolean collided = false;
            for (CollisionBox box : BOXES) {
                UUID entityId = e.getUuid();
                Vec3d originalMove = newMove;

                UUID trackingId = (e instanceof PlayerEntity) ? entityId : null;
                newMove = CollisionUtil.applyForceFieldContainment(pos, newMove, box, trackingId);

                if (!newMove.equals(originalMove)) {
                    collided = true;
                }
            }
        }

        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL == 0) {
            sendPlayerInsideUpdates(world);
        }

        MidTickCollisionManager.onWorldTickEnd(world);
    }

    private static void sendPlayerInsideUpdates(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            Set<CollisionBox> currentlyInside = new HashSet<>();
            for (CollisionBox box : BOXES) {
                if (box.isPlayerInside(playerId)) {
                    currentlyInside.add(box);
                }
            }

            Set<CollisionBox> lastSent = LAST_SENT_STATUS.getOrDefault(playerId, new HashSet<>());

            Set<CollisionBox> changedBoxes = new HashSet<>();

            for (CollisionBox box : currentlyInside) {
                if (!lastSent.contains(box)) {
                    changedBoxes.add(box);
                }
            }

            for (CollisionBox box : lastSent) {
                if (!currentlyInside.contains(box)) {
                    changedBoxes.add(box);
                }
            }

            if (!changedBoxes.isEmpty()) {
                PacketHandler.sendPlayerInsideStatus(player, changedBoxes, currentlyInside);
            }

            LAST_SENT_STATUS.put(playerId, new HashSet<>(currentlyInside));
        }
    }

    public static void onEntityRemoved(ServerWorld world, Entity entity) {
        MidTickCollisionManager.onEntityRemoved(world, entity);

        // Clean up player data when they leave
        if (entity instanceof PlayerEntity) {
            UUID playerId = entity.getUuid();
            PLAYER_INSIDE_BOXES.remove(playerId);
            LAST_SENT_STATUS.remove(playerId);
            CollisionUtil.clearPlayerData(playerId);

            for (CollisionBox box : BOXES) {
                box.setPlayerInside(playerId, false);
            }
        }
    }

    private static void initializeMidTickSystem() {
        if (!midTickSystemInitialized) {
            midTickSystemInitialized = true;
        }
    }
    public static void onMidTickPhase(ServerWorld world, int phase) {
        MidTickCollisionManager.performMidTickChecks(world, phase);
    }

    public static Set<CollisionBox> getBoxesPlayerIsInside(UUID playerId) {
        return PLAYER_INSIDE_BOXES.getOrDefault(playerId, new HashSet<>());
    }
}