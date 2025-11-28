package silly.homak.usables.common.barrier;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MidTickCollisionManager {
    private static final Map<ServerWorld, List<HighVelocityEntity>> TRACKED_ENTITIES = new ConcurrentHashMap<>();
    private static final int PHASES_PER_TICK = 4; // Check 4 times per tick
    private static final double HIGH_VELOCITY_THRESHOLD = 2.0; // 2 blocks/tick
    private static final int MAX_TRACKING_TICKS = 20; // Stop tracking after 1 second

    private static final Map<ServerWorld, Integer> CURRENT_PHASE = new ConcurrentHashMap<>();

    public static void registerForMidTickChecks(ServerWorld world, Entity entity) {
        Vec3d velocity = entity.getVelocity();
        double speed = velocity.length();

        if (speed > HIGH_VELOCITY_THRESHOLD) {
            TRACKED_ENTITIES.computeIfAbsent(world, k -> new ArrayList<>())
                    .add(new HighVelocityEntity(entity));
        }
    }

    public static void onWorldTickStart(ServerWorld world) {
        CURRENT_PHASE.put(world, 0);

        TRACKED_ENTITIES.computeIfPresent(world, (w, entities) -> {
            entities.removeIf(HighVelocityEntity::shouldRemove);
            return entities.isEmpty() ? null : entities;
        });
    }

    public static void performMidTickChecks(ServerWorld world, int phase) {
        if (phase < 0 || phase >= PHASES_PER_TICK) return;

        CURRENT_PHASE.put(world, phase);

        List<HighVelocityEntity> worldEntities = TRACKED_ENTITIES.get(world);
        if (worldEntities == null || worldEntities.isEmpty()) return;

        double phaseFraction = 1.0 / PHASES_PER_TICK;
        List<HighVelocityEntity> toRemove = new ArrayList<>();

        for (HighVelocityEntity tracked : worldEntities) {
            Entity entity = tracked.getEntity(world);
            if (entity == null || !entity.isAlive()) {
                toRemove.add(tracked);
                continue;
            }

            Vec3d originalVelocity = entity.getVelocity();
            if (originalVelocity.lengthSquared() < 1e-6) {
                toRemove.add(tracked);
                continue;
            }

            Vec3d phaseMove = originalVelocity.multiply(phaseFraction);
            Vec3d currentPos = entity.getPos();
            Vec3d adjustedMove = phaseMove;

            for (CollisionBox box : CollisionBoxManagerServer.BOXES) {
                adjustedMove = CollisionUtil.applyCollision(currentPos, adjustedMove, box);
            }

            if (!adjustedMove.equals(phaseMove)) {
                Vec3d newVelocity = adjustedMove.multiply(PHASES_PER_TICK);
                entity.setVelocity(newVelocity);

                Vec3d newPos = currentPos.add(adjustedMove);
                entity.setPosition(newPos);

                tracked.updateVelocity(newVelocity);
            }

            tracked.advancePhase();
        }

        if (!toRemove.isEmpty()) {
            worldEntities.removeAll(toRemove);
            if (worldEntities.isEmpty()) {
                TRACKED_ENTITIES.remove(world);
            }
        }
    }

    public static void onWorldTickEnd(ServerWorld world) {
        List<HighVelocityEntity> worldEntities = TRACKED_ENTITIES.get(world);
        if (worldEntities != null) {
            worldEntities.removeIf(HighVelocityEntity::shouldRemove);
            if (worldEntities.isEmpty()) {
                TRACKED_ENTITIES.remove(world);
            }
        }
        CURRENT_PHASE.remove(world);
    }

    public static void onEntityRemoved(ServerWorld world, Entity entity) {
        List<HighVelocityEntity> worldEntities = TRACKED_ENTITIES.get(world);
        if (worldEntities != null) {
            worldEntities.removeIf(tracked -> tracked.entityId == entity.getId());
            if (worldEntities.isEmpty()) {
                TRACKED_ENTITIES.remove(world);
            }
        }
    }

    public static int getCurrentPhase(ServerWorld world) {
        return CURRENT_PHASE.getOrDefault(world, 0);
    }

    public static int getPhasesPerTick() {
        return PHASES_PER_TICK;
    }

    public static boolean isEntityTracked(ServerWorld world, Entity entity) {
        List<HighVelocityEntity> worldEntities = TRACKED_ENTITIES.get(world);
        if (worldEntities == null) return false;

        return worldEntities.stream()
                .anyMatch(tracked -> tracked.entityId == entity.getId());
    }

    private static class HighVelocityEntity {
        private final int entityId;
        private Vec3d lastVelocity;
        private final long startTick;
        private int phasesCompleted;
        private Identifier worldUUID;

        public HighVelocityEntity(Entity entity) {
            this.entityId = entity.getId();
            this.lastVelocity = entity.getVelocity();
            this.startTick = entity.getWorld().getTime();
            this.phasesCompleted = 0;
            this.worldUUID = ((ServerWorld) entity.getWorld()).getRegistryKey().getValue();
        }

        public Entity getEntity(ServerWorld world) {
            if (!world.getRegistryKey().getValue().equals(worldUUID)) {
                return null;
            }

            return world.getEntityById(entityId);
        }

        public void updateVelocity(Vec3d newVelocity) {
            this.lastVelocity = newVelocity;
        }

        public void advancePhase() {
            this.phasesCompleted++;
        }

        public boolean shouldRemove() {
            return phasesCompleted >= PHASES_PER_TICK ||
                    (System.currentTimeMillis() / 50) - startTick > MAX_TRACKING_TICKS;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HighVelocityEntity that = (HighVelocityEntity) o;
            return entityId == that.entityId && Objects.equals(worldUUID, that.worldUUID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityId, worldUUID);
        }
    }
}