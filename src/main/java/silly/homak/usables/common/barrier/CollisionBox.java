package silly.homak.usables.common.barrier;

import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record CollisionBox(Vec3d pos, Vec3d rot, Vec3d scale, long expireGameTime) {
    private static final Set<UUID> playersInside = new HashSet<>();

    public boolean isPlayerInside(UUID playerId) {
        return playersInside.contains(playerId);
    }

    public void setPlayerInside(UUID playerId, boolean inside) {
        if (inside) {
            playersInside.add(playerId);
        } else {
            playersInside.remove(playerId);
        }
    }

    public Set<UUID> getPlayersInside() {
        return new HashSet<>(playersInside);
    }

    public void clearPlayersInside() {
        playersInside.clear();
    }
}