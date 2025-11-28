package silly.homak.usables.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class WaterBucketMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void waterBucketUse(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        BucketItem thisItem = (BucketItem)(Object)this;
        ItemStack stack = user.getStackInHand(hand);

        if (stack.getItem() == Items.WATER_BUCKET && user.isSneaking()) {
            if (user.isOnFire()) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.PLAYERS,
                        1.0f, 1.0f);
                user.extinguish();
                user.getItemCooldownManager().set(Items.WATER_BUCKET, 40);

                cir.setReturnValue(TypedActionResult.success(stack, world.isClient()));
            } else {
                cir.setReturnValue(TypedActionResult.pass(stack));
            }
        }
    }
}