package silly.homak.usables.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import silly.homak.usables.client.render.LIBRARY_CubeRenderer;
import silly.homak.usables.client.render.LIBRARY_QuadRenderer;
import silly.homak.usables.common.barrier.CollisionBoxManagerClient;
import silly.homak.usables.server.PacketHandler;

public class UsablesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LIBRARY_CubeRenderer.init();
        LIBRARY_QuadRenderer.init();
        PacketHandler.registerPackets();
        ClientTickEvents.END_CLIENT_TICK.register(client -> CollisionBoxManagerClient.tick());
    }
}
