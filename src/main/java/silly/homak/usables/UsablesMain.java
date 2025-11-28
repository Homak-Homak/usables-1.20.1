package silly.homak.usables;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
		LOGGER.info("Hello From {}", MOD_ID);
	}
}