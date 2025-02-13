package org.dynmap.forge_1_13_2;
/**
 * Forge specific implementation of DynmapWorld
 */
import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.EndDimension;
import net.minecraft.world.dimension.NetherDimension;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.EnumLightType;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

import org.dynmap.DynmapChunk;
import org.dynmap.DynmapLocation;
import org.dynmap.DynmapWorld;
import org.dynmap.utils.MapChunkCache;
import org.dynmap.utils.Polygon;

public class ForgeWorld extends DynmapWorld
{
    private IWorld world;
    private final boolean skylight;
    private final boolean isnether;
    private final boolean istheend;
    private final String env;
    private DynmapLocation spawnloc = new DynmapLocation();
    private static boolean doMCPCMapping = false;
    private static boolean doSaveFolderMapping = false;
    private static int maxWorldHeight = 256;    // Maximum allows world height
    
    public static void setMCPCMapping() {
        doMCPCMapping = true;
    }
    public static void setSaveFolderMapping() {
        doSaveFolderMapping = true;
    }
    public static int getMaxWorldHeight() {
        return maxWorldHeight;
    }
    public static void setMaxWorldHeight(int h) {
        maxWorldHeight = h;
    }

    public static String getWorldName(IWorld w) {
        String n;
        if (doMCPCMapping) {    // MCPC+ mapping
            n = w.getWorldInfo().getWorldName();
        }
        else if (doSaveFolderMapping) { // New vanilla Forge mapping (consistent with MCPC+)
            if (w.getDimension().getType() == DimensionType.OVERWORLD) {
                n = w.getWorldInfo().getWorldName();
            }
            else {
                n = "DIM" + w.getDimension().getType().getId();
            }
        }
        else {  // Legacy mapping
            n = w.getWorldInfo().getWorldName();
            Dimension wp = w.getDimension();
            switch(wp.getType().getId()) {
                case 0:
                    break;
                case -1:
                    n += "_nether";
                    break;
                case 1:
                    n += "_the_end";
                    break;
                default:
                    n += "_" + wp.getType().getId();
                    break;
            }
        }
        return n;
    }

    public ForgeWorld(IWorld w)
    {
        this(getWorldName(w), w.getWorld().getHeight(), w.getSeaLevel(), w.getDimension() instanceof NetherDimension,
        		w.getDimension() instanceof EndDimension, 
        		w.getWorldInfo().getWorldName() + "/" + w.getDimension().getType().toString());
        setWorldLoaded(w);
    }
    public ForgeWorld(String name, int height, int sealevel, boolean nether, boolean the_end, String deftitle)
    {
        super(name, (height > maxWorldHeight)?maxWorldHeight:height, sealevel);
        world = null;
        setTitle(deftitle);
        isnether = nether;
        istheend = the_end;
        skylight = !(isnether || istheend);

        if (isnether)
        {
            env = "nether";
        }
        else if (istheend)
        {
            env = "the_end";
        }
        else
        {
            env = "normal";
        }
        
    }
    /* Test if world is nether */
    @Override
    public boolean isNether()
    {
        return isnether;
    }
    public boolean isTheEnd()
    {
        return istheend;
    }
    /* Get world spawn location */
    @Override
    public DynmapLocation getSpawnLocation()
    {
    	if(world != null) {
    		BlockPos sloc = world.getSpawnPoint();
    		spawnloc.x = sloc.getX();
    		spawnloc.y = sloc.getY();
    		spawnloc.z = sloc.getZ();
    		spawnloc.world = this.getName();
    	}
        return spawnloc;
    }
    /* Get world time */
    @Override
    public long getTime()
    {
    	if(world != null)
    		return world.getWorld().getDayTime();
    	else
    		return -1;
    }
    /* World is storming */
    @Override
    public boolean hasStorm()
    {
    	if(world != null)
    		return world.getWorld().isRaining();
    	else
    		return false;
    }
    /* World is thundering */
    @Override
    public boolean isThundering()
    {
    	if(world != null)
    		return world.getWorld().isThundering();
    	else
    		return false;
    }
    /* World is loaded */
    @Override
    public boolean isLoaded()
    {
        return (world != null);
    }
    /* Set world to unloaded */
    @Override
    public void setWorldUnloaded() 
    {
    	getSpawnLocation();
    	world = null;
    }
    /* Set world to loaded */
    public void setWorldLoaded(IWorld w) {
    	world = w;
    	this.sealevel = w.getSeaLevel();   // Read actual current sealevel from world
    	// Update lighting table
    	float[] lt = w.getDimension().getLightBrightnessTable();
    	for (int i = 0; i < 16; i++) {
    	    this.setBrightnessTableEntry(i, lt[i]);
    	}
    }
    /* Get light level of block */
    @Override
    public int getLightLevel(int x, int y, int z)
    {
    	if(world != null)
    		return world.getLight(new BlockPos(x,  y,  z));
    	else
    		return -1;
    }
    /* Get highest Y coord of given location */
    @Override
    public int getHighestBlockYAt(int x, int z)
    {
    	if(world != null) {
            return world.getWorld().getChunk(x >> 4, z >> 4).getHeightmap(Type.LIGHT_BLOCKING).getHeight(x & 15, z & 15);
    	}
    	else
    		return -1;
    }
    /* Test if sky light level is requestable */
    @Override
    public boolean canGetSkyLightLevel()
    {
        return skylight;
    }
    /* Return sky light level */
    @Override
    public int getSkyLightLevel(int x, int y, int z)
    {
    	if(world != null) {
    	    return world.getLightFor(EnumLightType.SKY, new BlockPos(x, y, z));
    	}
    	else
    		return -1;
    }
    /**
     * Get world environment ID (lower case - normal, the_end, nether)
     */
    @Override
    public String getEnvironment()
    {
        return env;
    }
    /**
     * Get map chunk cache for world
     */
    @Override
    public MapChunkCache getChunkCache(List<DynmapChunk> chunks)
    {
    	if(world != null) {
    		ForgeMapChunkCache c = new ForgeMapChunkCache();
    		c.setChunks(this, chunks);
    		return c;
    	}
    	return null;
    }

    public World getWorld()
    {
        return world.getWorld();
    }
    @Override
    public Polygon getWorldBorder() {
        if (world != null) {
            WorldBorder wb = world.getWorldBorder();
            if ((wb != null) && (wb.getDiameter() < 5.9E7)) {
                Polygon p = new Polygon();
                p.addVertex(wb.minX(), wb.minZ());
                p.addVertex(wb.minX(), wb.maxZ());
                p.addVertex(wb.maxX(), wb.maxZ());
                p.addVertex(wb.maxX(), wb.minZ());
                return p;
            }
        }
        return null;
    }
}
