package silly.homak.usables.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.Vec3d;
import silly.homak.usables.client.render.InsideCubeHudOverlay;
import silly.homak.usables.client.render.LIBRARY_CubeRenderer;
import silly.homak.usables.client.render.LIBRARY_QuadRenderer;
import silly.homak.usables.client.render.PocketBarrierParticleEmitter;
import silly.homak.usables.client.render.post.FollyRedPostProcessor;
import silly.homak.usables.common.barrier.CollisionBox;
import silly.homak.usables.common.barrier.CollisionBoxManagerClient;
import silly.homak.usables.init.UsableItems;
import silly.homak.usables.server.PacketHandler;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.screen.ScreenParticleHolder;
import team.lodestar.lodestone.systems.postprocess.PostProcessHandler;

import java.awt.*;

public class UsablesClient implements ClientModInitializer {
    public static final Color FOLLY_RED = new Color(255, 0, 79);

    @Override
    public void onInitializeClient() {
        PacketHandler.registerPackets();
        PostProcessHandler.addInstance(FollyRedPostProcessor.INSTANCE);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                LIBRARY_CubeRenderer.clientTick();
                LIBRARY_QuadRenderer.clientTick();
            }

            if (client.player != null) {
                boolean isInsideAnyBox = false;

                for (CollisionBox box : CollisionBoxManagerClient.CLIENT_BOXES) {
                    if (CollisionBoxManagerClient.isPlayerInsideBox(box)) {
                        isInsideAnyBox = true;
                        var vel = client.player.getVelocity();
                        if (vel.lengthSquared() > 0.001) {
                            var builder = WorldParticleBuilder.create(LodestoneParticleRegistry.TWINKLE_PARTICLE)
                                    .setGravityStrength(0f)
                                    .setColorData(ColorParticleData.create(
                                                    new Color(220, 23, 85, (int) Math.min(255, vel.length() * 30)), // Fixed: use min not max
                                                    new Color(141, 14, 56, (int) Math.min(255, vel.length() * 30)))
                                            .build())
                                    .setScaleData(GenericParticleData.create(0.15f).build());
                            Vec3d pos = client.player.getPos().add(0, 1, 0);
                            builder.spawn(client.player.clientWorld, pos.x, pos.y, pos.z);
                        }
                    }
                }
                FollyRedPostProcessor.INSTANCE.setActive(isInsideAnyBox);
            } else {
                FollyRedPostProcessor.INSTANCE.setActive(false);
            }
        });

        HudRenderCallback.EVENT.register(new InsideCubeHudOverlay());
        ClientTickEvents.END_CLIENT_TICK.register(client -> CollisionBoxManagerClient.tick());

        ParticleEmitterHandler.registerItemParticleEmitter(new PocketBarrierParticleEmitter(), UsableItems.POCKET_BARRIER);
    }
}