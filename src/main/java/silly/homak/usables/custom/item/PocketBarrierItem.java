package silly.homak.usables.custom.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import silly.homak.usables.common.TickSchedulerServer;
import silly.homak.usables.common.barrier.CollisionBoxManagerServer;
import silly.homak.usables.server.PacketHandler;

import java.util.List;

public class PocketBarrierItem extends Item {
    public static float BARRIER_SCALE = 15f;
    private static final float BASE_BARRIER_SCALE = 15f;
    private static final int DURATION = 25;
    private static final int DELAY_TICKS = 5 * 20;

    public PocketBarrierItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 1));
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        NbtCompound nbt = user.getStackInHand(hand).getOrCreateNbt();
        if (!world.isClient) {
            if (nbt.contains("box_scale")) {
                BARRIER_SCALE = nbt.getFloat("box_scale");
            }
            else {
                BARRIER_SCALE = BASE_BARRIER_SCALE;
            }
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
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DURATION * 20, 2, false, false));
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION * 20, 1, false, false));
                PacketHandler.sendCubeEffect((ServerWorld) world, pos);
            });
            TickSchedulerServer.schedule(DELAY_TICKS, () -> {
                CollisionBoxManagerServer.addBox((ServerWorld) world,
                        pos,
                        Vec3d.ZERO,
                        new Vec3d(BARRIER_SCALE, BARRIER_SCALE, BARRIER_SCALE), // Outer dimensions
                        DURATION * 20);
            });
        }
        if (!user.isCreative()) user.getItemCooldownManager().set(this, ((int)(BARRIER_SCALE / 4) + DURATION + 80 * 20));
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType == ClickType.RIGHT) {
            NbtCompound nbt = stack.getOrCreateNbt();

            if (nbt.contains("box_scale")) {
                float current = nbt.getFloat("box_scale");
                if (current >= 50.0f) {
                    nbt.putFloat("box_scale", 4.0f);
                } else {
                    nbt.putFloat("box_scale", current + 1.0f);
                }
            } else {
                nbt.putFloat("box_scale", BASE_BARRIER_SCALE + 1.0f);
            }

            if (player.getWorld().isClient()) {
                player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.25f, 1.0f);
            }

            if (Screen.hasControlDown()) {
                slot.setStackNoCallbacks(stack);
            }

            return true;
        }

        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("box_scale")) {
            float boxScale = nbt.getFloat("box_scale");
            tooltip.add(Text.of("Creates a barrier the scale of " + (int) boxScale + " blocks").copy().formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.of("Creates a barrier the scale of " + (int) BASE_BARRIER_SCALE + " blocks").copy().formatted(Formatting.byColorIndex(1)));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.of("Pocket Barrier").copy().formatted(Formatting.GOLD);
    }
}