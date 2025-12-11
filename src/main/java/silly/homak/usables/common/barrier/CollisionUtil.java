package silly.homak.usables.common.barrier;

import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CollisionUtil {
    private static final Map<UUID, Boolean> PREVIOUS_INSIDE = new HashMap<>();
    private static final int MAX_SUBSTEPS = 120;
    private static final double MIN_VELOCITY_FOR_SUBSTEPS = 1.5;
    private static final double WALL_THICKNESS = 0.5;
    private static final double EPSILON = 0.001;
    private static final double PUSH_FORCE = 0.3;

    private static final Map<UUID, Map<CollisionBox, Boolean>> PLAYER_BOX_INSIDE_STATUS = new HashMap<>();

    public static Vec3d applyCollision(Vec3d position, Vec3d move, CollisionBox box) {
        return applyCollision(position, move, box, null);
    }

    public static Vec3d applyCollision(Vec3d position, Vec3d move, CollisionBox box, UUID playerId) {
        double moveLength = move.length();
        if (moveLength > MIN_VELOCITY_FOR_SUBSTEPS) {
            int substeps = Math.min(MAX_SUBSTEPS, (int) Math.ceil(moveLength / 0.5));
            Vec3d remainingMove = move;
            Vec3d currentPos = position;

            for (int i = 0; i < substeps && remainingMove.lengthSquared() > 1e-12; i++) {
                Vec3d substepMove = remainingMove.multiply(1.0 / (substeps - i));
                Vec3d adjustedSubstep = applyPushBackContainment(currentPos, substepMove, box, playerId);

                currentPos = currentPos.add(adjustedSubstep);
                remainingMove = remainingMove.subtract(substepMove).add(adjustedSubstep.subtract(substepMove));
            }

            return currentPos.subtract(position);
        } else {
            return applyPushBackContainment(position, move, box, playerId);
        }
    }

    private static Vec3d applyPushBackContainment(Vec3d position, Vec3d move, CollisionBox box, UUID playerId) {
        Vec3d targetPos = position.add(move);

        Vec3d localPos = position.subtract(box.pos());
        Vec3d localTarget = targetPos.subtract(box.pos());

        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = rotateZ(localPos, -rot.z);
        localPos = rotateX(localPos, -rot.x);
        localPos = rotateY(localPos, -rot.y);

        localTarget = rotateZ(localTarget, -rot.z);
        localTarget = rotateX(localTarget, -rot.x);
        localTarget = rotateY(localTarget, -rot.y);

        double halfX = box.scale().x / 2.0;
        double halfY = box.scale().y / 2.0;
        double halfZ = box.scale().z / 2.0;

        double innerHalfX = Math.max(0, halfX - WALL_THICKNESS);
        double innerHalfY = Math.max(0, halfY - WALL_THICKNESS);
        double innerHalfZ = Math.max(0, halfZ - WALL_THICKNESS);

        boolean inHollowSpace =
                Math.abs(localPos.x) < innerHalfX &&
                        Math.abs(localPos.y) < innerHalfY &&
                        Math.abs(localPos.z) < innerHalfZ;

        if (playerId != null) {
            PLAYER_BOX_INSIDE_STATUS.computeIfAbsent(playerId, k -> new HashMap<>())
                    .put(box, inHollowSpace);
            box.setPlayerInside(playerId, inHollowSpace);
        }

        if (inHollowSpace) {
            double adjustedX = localTarget.x;
            double adjustedY = localTarget.y;
            double adjustedZ = localTarget.z;

            if (Math.abs(localTarget.x) >= innerHalfX - EPSILON) {
                if (localTarget.x > 0) {
                    adjustedX = innerHalfX - EPSILON;
                } else {
                    adjustedX = -innerHalfX + EPSILON;
                }
            }

            if (Math.abs(localTarget.y) >= innerHalfY - EPSILON) {
                if (localTarget.y > 0) {
                    adjustedY = innerHalfY - EPSILON;
                } else {
                    adjustedY = -innerHalfY + EPSILON;
                }
            }

            if (Math.abs(localTarget.z) >= innerHalfZ - EPSILON) {
                if (localTarget.z > 0) {
                    adjustedZ = innerHalfZ - EPSILON;
                } else {
                    adjustedZ = -innerHalfZ + EPSILON;
                }
            }

            Vec3d adjustedLocalPos = new Vec3d(adjustedX, adjustedY, adjustedZ);
            Vec3d localMove = adjustedLocalPos.subtract(localPos);

            if (localMove.equals(localTarget.subtract(localPos))) {
                return move;
            }

            Vec3d worldMove = localMove;
            worldMove = rotateY(worldMove, rot.y);
            worldMove = rotateX(worldMove, rot.x);
            worldMove = rotateZ(worldMove, rot.z);

            return worldMove;
        }

        double xPush = 0;
        double yPush = 0;
        double zPush = 0;

        if (Math.abs(localPos.x) >= innerHalfX) {
            if (localPos.x > 0) {
                xPush = -(localPos.x - (innerHalfX - EPSILON));
            } else {
                xPush = -(localPos.x + (innerHalfX - EPSILON));
            }
        }

        if (Math.abs(localPos.y) >= innerHalfY) {
            if (localPos.y > 0) {
                yPush = -(localPos.y - (innerHalfY - EPSILON));
            } else {
                yPush = -(localPos.y + (innerHalfY - EPSILON));
            }
        }

        if (Math.abs(localPos.z) >= innerHalfZ) {
            if (localPos.z > 0) {
                zPush = -(localPos.z - (innerHalfZ - EPSILON));
            } else {
                zPush = -(localPos.z + (innerHalfZ - EPSILON));
            }
        }

        Vec3d pushVector = new Vec3d(xPush, yPush, zPush);
        double pushLength = pushVector.length();

        if (pushLength > 0) {
            Vec3d normalizedPush = pushVector.multiply(1.0 / pushLength);

            double pushDistance = Math.min(pushLength, PUSH_FORCE);
            Vec3d pushBackPos = localPos.add(normalizedPush.multiply(pushDistance));

            pushBackPos = new Vec3d(
                    clamp(pushBackPos.x, -innerHalfX + EPSILON, innerHalfX - EPSILON),
                    clamp(pushBackPos.y, -innerHalfY + EPSILON, innerHalfY - EPSILON),
                    clamp(pushBackPos.z, -innerHalfZ + EPSILON, innerHalfZ - EPSILON)
            );

            Vec3d localMove = pushBackPos.subtract(localPos);

            Vec3d worldMove = localMove;
            worldMove = rotateY(worldMove, rot.y);
            worldMove = rotateX(worldMove, rot.x);
            worldMove = rotateZ(worldMove, rot.z);

            return worldMove;
        }

        return move;
    }

    public static Vec3d applyForceFieldContainment(Vec3d position, Vec3d move, CollisionBox box, UUID playerId) {
        Vec3d targetPos = position.add(move);

        Vec3d localPos = position.subtract(box.pos());
        Vec3d localTarget = targetPos.subtract(box.pos());

        Vec3d rot = new Vec3d(
                Math.toRadians(box.rot().x),
                Math.toRadians(box.rot().y),
                Math.toRadians(box.rot().z)
        );

        localPos = rotateZ(localPos, -rot.z);
        localPos = rotateX(localPos, -rot.x);
        localPos = rotateY(localPos, -rot.y);

        localTarget = rotateZ(localTarget, -rot.z);
        localTarget = rotateX(localTarget, -rot.x);
        localTarget = rotateY(localTarget, -rot.y);

        double halfX = box.scale().x / 2.0;
        double halfY = box.scale().y / 2.0;
        double halfZ = box.scale().z / 2.0;

        double innerHalfX = Math.max(0, halfX - WALL_THICKNESS);
        double innerHalfY = Math.max(0, halfY - WALL_THICKNESS);
        double innerHalfZ = Math.max(0, halfZ - WALL_THICKNESS);

        boolean inHollowSpace =
                Math.abs(localPos.x) < innerHalfX &&
                        Math.abs(localPos.y) < innerHalfY &&
                        Math.abs(localPos.z) < innerHalfZ;

        if (playerId != null) {
            PLAYER_BOX_INSIDE_STATUS.computeIfAbsent(playerId, k -> new HashMap<>())
                    .put(box, inHollowSpace);
            box.setPlayerInside(playerId, inHollowSpace);
        }

        Vec3d desiredMove = localTarget.subtract(localPos);

        if (inHollowSpace) {
            double adjustedX = localTarget.x;
            double adjustedY = localTarget.y;
            double adjustedZ = localTarget.z;

            if (Math.abs(localTarget.x) >= innerHalfX - EPSILON) {
                if (localTarget.x > 0) {
                    adjustedX = innerHalfX - EPSILON;
                } else {
                    adjustedX = -innerHalfX + EPSILON;
                }
            }

            if (Math.abs(localTarget.y) >= innerHalfY - EPSILON) {
                if (localTarget.y > 0) {
                    adjustedY = innerHalfY - EPSILON;
                } else {
                    adjustedY = -innerHalfY + EPSILON;
                }
            }

            if (Math.abs(localTarget.z) >= innerHalfZ - EPSILON) {
                if (localTarget.z > 0) {
                    adjustedZ = innerHalfZ - EPSILON;
                } else {
                    adjustedZ = -innerHalfZ + EPSILON;
                }
            }

            Vec3d adjustedLocalPos = new Vec3d(adjustedX, adjustedY, adjustedZ);
            Vec3d localMove = adjustedLocalPos.subtract(localPos);

            if (localMove.equals(desiredMove)) {
                return move;
            }

            Vec3d worldMove = localMove;
            worldMove = rotateY(worldMove, rot.y);
            worldMove = rotateX(worldMove, rot.x);
            worldMove = rotateZ(worldMove, rot.z);

            return worldMove;
        }

        Vec3d toCenter = localPos.multiply(-1);

        double toCenterLength = toCenter.length();
        if (toCenterLength > 0) {
            Vec3d pushDirection = toCenter.multiply(1.0 / toCenterLength);

            double pushStrength = Math.min(PUSH_FORCE * 2, toCenterLength * 0.5);
            Vec3d pushMove = pushDirection.multiply(pushStrength);
            double dotProduct = desiredMove.dotProduct(pushDirection);
            Vec3d perpendicularComponent = desiredMove.subtract(pushDirection.multiply(dotProduct));

            double wallDepth = Math.max(
                    Math.max(
                            Math.max(0, Math.abs(localPos.x) - innerHalfX),
                            Math.max(0, Math.abs(localPos.y) - innerHalfY)
                    ),
                    Math.max(0, Math.abs(localPos.z) - innerHalfZ)
            );

            double reductionFactor = Math.max(0.1, 1.0 - wallDepth / WALL_THICKNESS);
            perpendicularComponent = perpendicularComponent.multiply(reductionFactor);

            Vec3d totalMove = pushMove.add(perpendicularComponent);

            Vec3d newLocalPos = localPos.add(totalMove);

            newLocalPos = new Vec3d(
                    clamp(newLocalPos.x, -innerHalfX + EPSILON, innerHalfX - EPSILON),
                    clamp(newLocalPos.y, -innerHalfY + EPSILON, innerHalfY - EPSILON),
                    clamp(newLocalPos.z, -innerHalfZ + EPSILON, innerHalfZ - EPSILON)
            );

            Vec3d localMove = newLocalPos.subtract(localPos);

            Vec3d worldMove = localMove;
            worldMove = rotateY(worldMove, rot.y);
            worldMove = rotateX(worldMove, rot.x);
            worldMove = rotateZ(worldMove, rot.z);

            return worldMove;
        }

        return move;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isPlayerInsideBox(UUID playerId, CollisionBox box) {
        Map<CollisionBox, Boolean> status = PLAYER_BOX_INSIDE_STATUS.get(playerId);
        return status != null && status.getOrDefault(box, false);
    }

    public static void clearPlayerData(UUID playerId) {
        PLAYER_BOX_INSIDE_STATUS.remove(playerId);
    }

    public static void clearAllPlayerData() {
        PLAYER_BOX_INSIDE_STATUS.clear();
    }

    public static Vec3d rotateX(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x,
                v.y * c - v.z * s,
                v.y * s + v.z * c
        );
    }

    public static Vec3d rotateY(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x * c + v.z * s,
                v.y,
                -v.x * s + v.z * c
        );
    }

    public static Vec3d rotateZ(Vec3d v, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(
                v.x * c - v.y * s,
                v.x * s + v.y * c,
                v.z
        );
    }
}