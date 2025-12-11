package silly.homak.usables.client.render;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import silly.homak.usables.client.UsablesClient;
import silly.homak.usables.init.UsableItems;
import team.lodestar.lodestone.handlers.screenparticle.ParticleEmitterHandler;
import team.lodestar.lodestone.registry.common.particle.LodestoneScreenParticleRegistry;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.particle.builder.ScreenParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;
import team.lodestar.lodestone.systems.particle.data.spin.SpinParticleData;
import team.lodestar.lodestone.systems.particle.screen.ScreenParticleHolder;
import team.lodestar.lodestone.systems.particle.screen.base.ScreenParticle;

import java.util.random.RandomGenerator;

public class PocketBarrierParticleEmitter implements ParticleEmitterHandler.ItemParticleSupplier {

    @Override
    public void spawnLateParticles(ScreenParticleHolder target, World world, float partialTick, ItemStack stack, float x, float y) {
        if (!stack.isOf(UsableItems.POCKET_BARRIER)) {
            return;
        }

        RandomGenerator random = RandomGenerator.getDefault();
        if (random.nextInt(5) == 0) {
            ScreenParticleBuilder.create(LodestoneScreenParticleRegistry.TWINKLE, target)
                    .setScaleData(GenericParticleData.create(.1f + random.nextFloat() * .3f).build())
                    .setColorData(ColorParticleData.create(UsablesClient.FOLLY_RED, UsablesClient.FOLLY_RED).build())
                    .setTransparencyData(GenericParticleData.create(0, 1f, 0).setEasing(Easing.QUAD_OUT, Easing.SINE_OUT).build())
                    .setLifetime(20)
                    .setSpinData(SpinParticleData.create((float) (random.nextGaussian() / 20f)).setSpinOffset(random.nextFloat() * 360f).build())
                    .setRandomOffset(7)
                    .spawn(x, y);
        }
    }
}