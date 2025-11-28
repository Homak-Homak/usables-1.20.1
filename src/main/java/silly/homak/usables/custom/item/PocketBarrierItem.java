package silly.homak.usables.custom.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import silly.homak.usables.common.TickSchedulerServer;
import silly.homak.usables.common.barrier.CollisionBoxManagerServer;
import silly.homak.usables.server.PacketHandler;

public class PocketBarrierItem extends Item {
    private static final float BARRIER_SCALE = 20f;
    private static final int DURATION = 25;
    private static final int DELAY_TICKS = 5 * 20;
    public PocketBarrierItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            var pos = user.getPos();
            world.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                    user.getSoundCategory(),
                    1.0f, 1.0f);
            TickSchedulerServer.scheduleRepeating(DELAY_TICKS, i -> {
                if (i % 10 == 0) {
                    PacketHandler.sendQuadEffect((ServerWorld) world, pos);
                    world.playSound(null, pos.x, pos.y, pos.z,
                            SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                            user.getSoundCategory(),
                            0.5f, 1.0f);
                }
            });
            TickSchedulerServer.schedule(DELAY_TICKS, () -> {
                PacketHandler.sendCubeEffect((ServerWorld) world, pos);
            });
            TickSchedulerServer.schedule(DELAY_TICKS, () -> {
                // X - axis ones
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.add(BARRIER_SCALE / 2, 0, 0), Vec3d.ZERO,
                        new Vec3d(1, BARRIER_SCALE + 2, BARRIER_SCALE + 1),
                        DURATION * 20);
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.subtract(BARRIER_SCALE / 2, 0, 0), Vec3d.ZERO,
                        new Vec3d(1, BARRIER_SCALE + 2, BARRIER_SCALE + 1),
                        DURATION * 20);

                // Z - axis ones
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.add(0, 0, BARRIER_SCALE / 2), Vec3d.ZERO,
                        new Vec3d(BARRIER_SCALE + 1, BARRIER_SCALE + 2, 1),
                        DURATION * 20);
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.subtract(0, 0, BARRIER_SCALE / 2), Vec3d.ZERO,
                        new Vec3d(BARRIER_SCALE + 1, BARRIER_SCALE + 2, 1),
                        DURATION * 20);
                // Y - axis ones
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.add(0, BARRIER_SCALE / 2, 0), Vec3d.ZERO,
                        new Vec3d(BARRIER_SCALE + 1, 1, BARRIER_SCALE + 1),
                        DURATION * 20);
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos.subtract(0, BARRIER_SCALE / 2, 0), Vec3d.ZERO,
                        new Vec3d(BARRIER_SCALE + 1, 1, BARRIER_SCALE + 1),
                        DURATION * 20);
            });
        }
        if (!user.isCreative()) user.getItemCooldownManager().set(this, (DELAY_TICKS + DURATION * 20 + 160 * 20));
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.of("Pocket Barrier").copy().formatted(Formatting.GOLD);
    }
}
