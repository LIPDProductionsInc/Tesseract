package com.supermartijn642.tesseract;

import com.supermartijn642.core.ClientUtils;
import com.supermartijn642.tesseract.screen.TesseractScreen;
import com.supermartijn642.tesseract.screen.info.InfoScreen;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 3/19/2020 by SuperMartijn642
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientProxy {

    @SubscribeEvent
    public static void setup(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(Tesseract.tesseract_tile, context -> new TesseractTileRenderer());
    }

//    @SubscribeEvent
//    public static void onModelBake(ModelEvent.RegisterAdditional e){
//        e.register(new ResourceLocation("tesseract", "block/pipe"));
//        e.register(new ResourceLocation("tesseract", "block/pipe_extract"));
//        e.register(new ResourceLocation("tesseract", "block/pipe_extract_and_insert"));
//        e.register(new ResourceLocation("tesseract", "block/pipe_insert"));
//    }

    public static void openScreen(BlockPos pos){
        ClientUtils.getMinecraft().setScreen(new TesseractScreen(pos));
    }

    public static void openInfoScreen(BlockPos pos){
        ClientUtils.getMinecraft().setScreen(new InfoScreen(pos));
    }
}
