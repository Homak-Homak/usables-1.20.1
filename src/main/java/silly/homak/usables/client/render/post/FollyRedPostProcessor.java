package silly.homak.usables.client.render.post;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import silly.homak.usables.UsablesMain;
import team.lodestar.lodestone.systems.postprocess.PostProcessor;

public class FollyRedPostProcessor extends PostProcessor {
    public static final FollyRedPostProcessor INSTANCE = new FollyRedPostProcessor();
    static {
        INSTANCE.setActive(false);
    }
    @Override
    public Identifier getPostChainLocation() {
        return new Identifier(UsablesMain.MOD_ID, "folly_post");
    }

    @Override
    public void beforeProcess(MatrixStack viewModelStack) {

    }

    @Override
    public void afterProcess() {

    }
}
