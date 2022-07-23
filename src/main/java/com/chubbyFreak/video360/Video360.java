package com.chubbyFreak.video360;

import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Video360 extends JavaPlugin implements Listener {

    private static final int PORT = 8080;
    private static final double SPHERE_RADIUS = 54;
    private static final int IMAGE_WIDTH = 340;
    private static final int IMAGE_HEIGHT = 170;
    private static final int X_OFFSET = 0;
    private static final int Y_OFFSET = 64;
    private static final int Z_OFFSET = 0;
    private static final int CLEAR_WIDTH = 427;
    private static final int CLEAR_HEIGHT = 240;
    private static final Block SPHERE_CANVAS_STARTING_BLOCK = Blocks.WHITE_CONCRETE;

    private IBlockData[][] prevImage;
    private World world;
    private ArrayList<Integer[]> sphereCoordinates = new ArrayList<>();
    private ArrayList<Integer[]> pixelCoordinates = new ArrayList<>();
    private Commands commands = new Commands();
    private Commands.Canvas prevCanvas = Commands.Canvas.SPHERE;

    @Override
    public void onEnable() {
        getCommand(Commands.MODE_COMMAND).setExecutor(commands);
        getCommand(Commands.CAPTURE_COMMAND).setExecutor(commands);
        getCommand(Commands.CANVAS_COMMAND).setExecutor(commands);
        // Plugin startup logic
        System.out.println("Plugin started.");
        world = Bukkit.getWorld("world");

        time(() -> clearFlatCanvas(Blocks.AIR), "clear flat canvas");
        time(this::generateCoordinates, "generate coordinates");
        time(() -> clearSphereCanvas(SPHERE_CANVAS_STARTING_BLOCK), "clear sphere canvas");
        time(this::destroyEntities, "destroy entities");

        startWebSocketServer();
    }

    private interface Operation {
        void operate();
    }

    private void time(Operation operation, String name) {
        long startTime = System.currentTimeMillis();
        operation.operate();
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Operation %s took %.2f seconds.\n", name, duration / 1000.0);
    }

    private void destroyEntities() {
        List<org.bukkit.entity.Entity> list = world.getEntities();
        Iterator<org.bukkit.entity.Entity> entities = list.iterator();
        while (entities.hasNext()) {
            Entity entity = entities.next();
            if (entity instanceof Item) {
                entity.remove();
            }
        }
    }

    private void clearFlatCanvas(Block clearBlock) {
        for (int x = 0; x < CLEAR_WIDTH; x++) {
            for (int y = 0; y < CLEAR_HEIGHT; y++) {
                setBlockInNativeWorld(world, x - CLEAR_WIDTH / 2 + X_OFFSET, Y_OFFSET, y - CLEAR_HEIGHT / 2 + Z_OFFSET, clearBlock, true);
            }
        }
    }

    private void generateCoordinates() {
        sphereCoordinates = SphereGenerator.generateCoordinates(SPHERE_RADIUS, true);
        for(Integer[] sphereCoordinate: sphereCoordinates) {
            double yaw = Math.atan2(sphereCoordinate[2], sphereCoordinate[0]);
            double pitch = Math.asin(1.0 * sphereCoordinate[1] / SPHERE_RADIUS);

            int x = (int) Math.round(clamp(SPHERE_RADIUS * yaw + IMAGE_WIDTH / 2.0, 0, IMAGE_WIDTH - 1));
            int y = (int) Math.round(clamp(IMAGE_HEIGHT - (SPHERE_RADIUS * pitch + IMAGE_HEIGHT / 2.0), 0, IMAGE_HEIGHT - 1));
            pixelCoordinates.add(new Integer[] {x, y});
        }
    }

    private void clearSphereCanvas(Block clearBlock) {
        for(Integer[] coordinate: sphereCoordinates)
            setBlockInNativeWorld(world, coordinate[0] + X_OFFSET, coordinate[1] + Y_OFFSET, coordinate[2] + Z_OFFSET, clearBlock, true);
    }

    private void startWebSocketServer() {
        try {
            Server server = new Server(new InetSocketAddress(PORT));
            server.start();
            new BukkitRunnable()
            {
                private BufferedImage prevBufferedImage;
                @Override
                public void run() {
                    BufferedImage img = server.img;
                    if(commands.getCanvas() != prevCanvas) {
                        switch(prevCanvas) {
                            case SPHERE:
                                clearSphereCanvas(Blocks.AIR);
                            case FLAT:
                                clearFlatCanvas(Blocks.AIR);
                        }
                    }
                    if (img != null && (commands.getMode() != Commands.Mode.RENDER || commands.isToCapture())) {
                        if(prevImage == null) {
                            prevImage = new IBlockData[img.getWidth()][img.getHeight()];
                            prevBufferedImage = deepCopy(img);
                        }
                        switch(commands.getCanvas()) {
                            case SPHERE:
                                for (int i = 0; i < sphereCoordinates.size(); i++) {
                                    Integer[] sphereCoordinate = sphereCoordinates.get(i);
                                    Integer[] pixelCoordinate = pixelCoordinates.get(i);

                                    int color = img.getRGB(pixelCoordinate[0], pixelCoordinate[1]);
                                    if (color == prevBufferedImage.getRGB(pixelCoordinate[0], pixelCoordinate[1]))
                                        continue;
                                    Block block = CanvasBlocks.NEW_PALETTE.get(color);
                                    if (block != null) {
                                        if (prevImage[pixelCoordinate[0]][pixelCoordinate[1]] != null && !prevImage[pixelCoordinate[0]][pixelCoordinate[1]].equals(block)) {
                                            setBlockInNativeWorld(world, sphereCoordinate[0] + X_OFFSET, sphereCoordinate[1] + Y_OFFSET, sphereCoordinate[2] + Z_OFFSET, block, false);
                                        }
                                        prevImage[pixelCoordinate[0]][pixelCoordinate[1]] = block.getBlockData();
                                    }
                                }
                                break;
                            case FLAT:
                                for(int x = 0; x < IMAGE_WIDTH; x++)  {
                                    for(int y = 0; y < IMAGE_HEIGHT; y++) {
                                        int color = img.getRGB(x, y);
                                        if (color == prevBufferedImage.getRGB(x, y))
                                            continue;
                                        Block block = CanvasBlocks.NEW_PALETTE.get(color);
                                        if (block != null) {
                                            if (prevImage[x][y] != null && !prevImage[x][y].equals(block)) {
                                                setBlockInNativeWorld(world, x + X_OFFSET, Y_OFFSET, y + Z_OFFSET, block, false);
                                            }
                                            prevImage[x][y] = block.getBlockData();
                                        }
                                    }
                                }
                                break;
                        }
                        prevBufferedImage = deepCopy(img);
                        if(commands.getMode().equals(Commands.Mode.RENDER) && commands.isToCapture())
                            commands.setToCapture(false);
                        prevCanvas = commands.getCanvas();
                    }
                }
            }.runTaskTimer(this,10L, 3L);
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public void setBlockInNativeWorld(World world, int x, int y, int z, net.minecraft.server.v1_16_R3.Block block, boolean applyPhysics) {
        net.minecraft.server.v1_16_R3.World nmsWorld = ((CraftWorld) world).getHandle();
        BlockPosition bp = new BlockPosition(x, y, z);
        nmsWorld.setTypeAndData(bp, block.getBlockData(), applyPhysics ? 3 : 2);
    }

    private double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Plugin shut down.");
    }
}
