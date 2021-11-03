package com.supermartijn642.tesseract.manager;

import com.supermartijn642.tesseract.TesseractTile;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created 12/16/2020 by SuperMartijn642
 */
public class TesseractTracker {

    public static MinecraftServer minecraftServer;

    public static final TesseractTracker SERVER = new TesseractTracker();
//    public static final TesseractTracker CLIENT = new TesseractTracker();

    public static TesseractTracker getInstance(World world){
        return world.isClientSide ? null /*CLIENT*/ : SERVER;
    }

    private final HashMap<String,HashMap<BlockPos,TesseractReference>> tesseracts = new HashMap<>();
    private final Set<TesseractReference> toBeRemoved = new HashSet<>();

    public TesseractReference add(TesseractTile self){
        String dimension = self.getLevel().dimension().location().toString();
        this.tesseracts.putIfAbsent(dimension, new HashMap<>());
        return this.tesseracts.get(dimension).computeIfAbsent(self.getBlockPos(), key -> new TesseractReference(self));
    }

    @Deprecated
    public TesseractReference tryAdd(String dimension, BlockPos pos){
        if(minecraftServer == null)
            return null;

        RegistryKey<World> key = RegistryKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimension));
        World world = TesseractChannelManager.minecraftServer.getLevel(key);
        TileEntity tile = world.getBlockEntity(pos);
        return tile instanceof TesseractTile ? this.add((TesseractTile)tile) : null;
    }

    public void remove(World world, BlockPos pos){
        String dimension = world.dimension().location().toString();
        this.remove(dimension, pos);
    }

    public void remove(String dimension, BlockPos pos){
        this.tesseracts.putIfAbsent(dimension, new HashMap<>());
        TesseractReference reference = this.tesseracts.get(dimension).get(pos);
        if(reference != null)
            this.toBeRemoved.add(reference);
    }

    private void removeAndUpdate(TesseractReference reference){
        reference.delete();
        this.tesseracts.putIfAbsent(reference.getDimension(), new HashMap<>());
        this.tesseracts.get(reference.getDimension()).remove(reference.getPos());
    }

    public TesseractReference get(World world, BlockPos pos){
        String dimension = world.dimension().location().toString();
        return this.tesseracts.putIfAbsent(dimension, new HashMap<>()).get(pos);
    }

    public CompoundNBT writeKey(TesseractReference reference){
        CompoundNBT tag = new CompoundNBT();
        tag.putString("dimension", reference.getDimension());
        tag.putInt("posx", reference.getPos().getX());
        tag.putInt("posy", reference.getPos().getY());
        tag.putInt("posz", reference.getPos().getZ());
        return tag;
    }

    public TesseractReference fromKey(CompoundNBT key){
        String dimension = key.getString("dimension");
        BlockPos pos = new BlockPos(key.getInt("posx"), key.getInt("posy"), key.getInt("posz"));
        return this.tesseracts.containsKey(dimension) ?
            this.tesseracts.get(dimension).get(pos) : null;
    }

    @SubscribeEvent
    public static void onSave(WorldEvent.Save e){
        if(e.getWorld().isClientSide() || !(e.getWorld() instanceof World) || ((World)e.getWorld()).dimension() != World.OVERWORLD)
            return;

        File directory = new File(((ServerWorld)e.getWorld()).getServer().getWorldPath(FolderName.ROOT).toFile(), "tesseract/tracking");
        int index = 0;
        for(Map.Entry<String,HashMap<BlockPos,TesseractReference>> dimensionEntry : SERVER.tesseracts.entrySet()){
            for(Map.Entry<BlockPos,TesseractReference> entry : dimensionEntry.getValue().entrySet()){
                File file = new File(directory, "tesseract" + index++ + ".nbt");
                try{
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    CompressedStreamTools.write(entry.getValue().write(), file);
                }catch(IOException ioException){
                    ioException.printStackTrace();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoad(WorldEvent.Load e){
        if(e.getWorld().isClientSide() || !(e.getWorld() instanceof World) || ((World)e.getWorld()).dimension() != World.OVERWORLD)
            return;

        minecraftServer = ((ServerWorld)e.getWorld()).getServer();

        SERVER.tesseracts.clear();

        File directory = new File(((ServerWorld)e.getWorld()).getServer().getWorldPath(FolderName.ROOT).toFile(), "tesseract/tracking");
        File[] files = directory.listFiles();
        if(files != null){
            for(File file : directory.listFiles()){
                if(file.isFile() && file.getName().endsWith(".nbt")){
                    try{
                        CompoundNBT tag = CompressedStreamTools.read(file);
                        TesseractReference location = new TesseractReference(tag);
                        SERVER.tesseracts.putIfAbsent(location.getDimension(), new HashMap<>());
                        SERVER.tesseracts.get(location.getDimension()).put(location.getPos(), location);
                    }catch(IOException ioException){
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.WorldTickEvent e){
        if(e.world.isClientSide || e.phase != TickEvent.Phase.END || e.world.dimension() != World.OVERWORLD)
            return;

        SERVER.toBeRemoved.forEach(SERVER::removeAndUpdate);
        SERVER.toBeRemoved.clear();
    }
}
