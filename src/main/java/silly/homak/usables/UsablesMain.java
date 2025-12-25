package silly.homak.usables;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silly.homak.usables.common.barrier.CollisionBoxManagerServer;
import silly.homak.usables.init.UsableItems;

public class UsablesMain implements ModInitializer {
	public static final String MOD_ID = "usables";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UsableItems.init();

		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!world.isClient) {
				CollisionBoxManagerServer.tick(world);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
			if (!world.isClient) {
				CollisionBoxManagerServer.onEntityRemoved(world, entity);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "test"), (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				ServerWorld world = player.getServerWorld();
				BlockPos pos = player.getSteppingPos();
				NbtCompound blockEntityNbt = new NbtCompound();
				blockEntityNbt.putString("id", "minecraft:lectern");
				blockEntityNbt.putInt("x", pos.getX());
				blockEntityNbt.putInt("y", pos.getY());
				blockEntityNbt.putInt("z", pos.getZ());
				blockEntityNbt.putBoolean("hasBook", true);
				blockEntityNbt.putInt("page", 0);
				BlockState shulkerState = Blocks.SHULKER_BOX.getDefaultState();
				world.setBlockState(pos, shulkerState, 3);
				BlockEntity blockEntity = BlockEntity.createFromNbt(pos, shulkerState, blockEntityNbt);
				if (blockEntity != null) {
					world.addBlockEntity(blockEntity);
				}
			});
		});

		LOGGER.info("Hello From {}", MOD_ID);
	}
}