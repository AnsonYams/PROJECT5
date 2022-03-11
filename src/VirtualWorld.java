import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Optional;

import processing.core.*;

public final class VirtualWorld extends PApplet
{
    private final int TIMER_ACTION_PERIOD = 100;

    private final int VIEW_WIDTH = 640;
    private static final int VIEW_HEIGHT = 480;
    private final int TILE_WIDTH = 32;
    private final int TILE_HEIGHT = 32;
    private final int WORLD_WIDTH_SCALE = 2;
    private final int WORLD_HEIGHT_SCALE = 2;

    private final int VIEW_COLS = VIEW_WIDTH / TILE_WIDTH;
    private final int VIEW_ROWS = VIEW_HEIGHT / TILE_HEIGHT;
    private final int WORLD_COLS = VIEW_COLS * WORLD_WIDTH_SCALE;
    private final int WORLD_ROWS = VIEW_ROWS * WORLD_HEIGHT_SCALE;

    private final String IMAGE_LIST_FILE_NAME = "imagelist";
    private final String DEFAULT_IMAGE_NAME = "background_default";
    private final int DEFAULT_IMAGE_COLOR = 0x808080;

    private String LOAD_FILE_NAME = "world.sav";

    private static final String FAST_FLAG = "-fast";
    private static final String FASTER_FLAG = "-faster";
    private static final String FASTEST_FLAG = "-fastest";
    private static final double FAST_SCALE = 0.5;
    private static final double FASTER_SCALE = 0.25;
    private static final double FASTEST_SCALE = 0.10;

    private static double timeScale = 1.0;

    private ImageStore imageStore;
    private WorldModel world;
    private WorldView view;
    private EventScheduler scheduler;

    private long nextTime;

    public void settings() {
        size(VIEW_WIDTH, VIEW_HEIGHT);
    }

    /*
       Processing entry point for "sketch" setup.
    */
    public void setup() {
        this.imageStore = new ImageStore(
                createImageColored(TILE_WIDTH, TILE_HEIGHT,
                                   DEFAULT_IMAGE_COLOR));
        this.world = new WorldModel(WORLD_ROWS, WORLD_COLS,
                                    createDefaultBackground(imageStore));
        this.view = new WorldView(VIEW_ROWS, VIEW_COLS, this, world, TILE_WIDTH,
                                  TILE_HEIGHT);
        this.scheduler = new EventScheduler(timeScale);

        loadImages(IMAGE_LIST_FILE_NAME, imageStore, this);
        loadWorld(world, LOAD_FILE_NAME, imageStore);

        scheduleActions(world, scheduler, imageStore);

        nextTime = System.currentTimeMillis() + TIMER_ACTION_PERIOD;
    }

    public void draw() {
        long time = System.currentTimeMillis();
        if (time >= nextTime) {
            scheduler.updateOnTime(time);
            nextTime = time + TIMER_ACTION_PERIOD;
        }

        view.drawViewport();
    }

    // Just for debugging and for P5
    public void mousePressed() {
        Point pressed = mouseToPoint(mouseX, mouseY);
        Point p1 = new Point(pressed.x+1, pressed.y);
        Point p2 = new Point(pressed.x-1, pressed.y);
        Point p3 = new Point(pressed.x, pressed.y-1);
        Point p4 = new Point(pressed.x, pressed.y+1);
        Point p5 = new Point(pressed.x+1, pressed.y+1);
        Point p6 = new Point(pressed.x-1, pressed.y-1);
        Point p7 = new Point(pressed.x+1, pressed.y-1);
        Point p8 = new Point(pressed.x-1, pressed.y+1);

        spreadFre(p1, p2);
        spreadFre(p3, p4);
        spreadFre(p5,p6);
        spreadFre(p7,p8);

        Goomab g = Factory.createGoomab(Functions.GOOMAB_KEY, pressed, imageStore.getImageList(Functions.GOOMAB_KEY), 100, 100);
        world.tryAddEntity(g);
        g.scheduleActions(scheduler, world, imageStore);

        Optional<Entity> entityOptional = world.getOccupant(pressed);
        if (entityOptional.isPresent())
        {
            Entity entity = entityOptional.get();
            System.out.println(entity.getId() + ": " + entity.getClass());
        }

    }

    public void spreadFre(Point p1, Point p2) {
        Fire f0= Factory.createFire(Functions.FIRE_KEY,p1,imageStore.getImageList(Functions.FIRE_KEY),100,100);
        world.tryAddEntity(f0);
        f0.scheduleActions(scheduler,world,imageStore);

        Fire f1= Factory.createFire(Functions.FIRE_KEY,p2,imageStore.getImageList(Functions.FIRE_KEY),100,100);
        world.tryAddEntity(f1);
        f1.scheduleActions(scheduler,world,imageStore);
    }

    private Point mouseToPoint(int x, int y)
    {
        return view.getViewport().viewportToWorld(mouseX/TILE_WIDTH, mouseY/TILE_HEIGHT);
    }
    public void keyPressed() {
        if (key == CODED) {
            int dx = 0;
            int dy = 0;

            switch (keyCode) {
                case UP:
                    dy = -1;
                    break;
                case DOWN:
                    dy = 1;
                    break;
                case LEFT:
                    dx = -1;
                    break;
                case RIGHT:
                    dx = 1;
                    break;
            }
            view.shiftView(dx, dy);
        }
    }

    private Background createDefaultBackground(ImageStore imageStore) {
        return new Background(DEFAULT_IMAGE_NAME,
                              imageStore.getImageList(DEFAULT_IMAGE_NAME));
    }

    private PImage createImageColored(int width, int height, int color) {
        PImage img = new PImage(width, height, RGB);
        img.loadPixels();
        for (int i = 0; i < img.pixels.length; i++) {
            img.pixels[i] = color;
        }
        img.updatePixels();
        return img;
    }

    private void loadImages(
            String filename, ImageStore imageStore, PApplet screen)
    {
        try {
            Scanner in = new Scanner(new File(filename));
            Functions.loadImages(in, imageStore, screen);
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadWorld(
            WorldModel world, String filename, ImageStore imageStore)
    {
        try {
            Scanner in = new Scanner(new File(filename));
            Functions.load(in, world, imageStore);
        }
        catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void scheduleActions(
            WorldModel world, EventScheduler scheduler, ImageStore imageStore)
    {
        for (Entity entity : world.getEntities()) {
            if( entity instanceof Animates) {
                ((Animates)entity).scheduleActions(scheduler, world, imageStore);
            }
        }
    }

    private static void parseCommandLine(String[] args) {
        if (args.length > 1)
        {
            if (args[0].equals("file"))
            {

            }
        }
        for (String arg : args) {
            switch (arg) {
                case FAST_FLAG:
                    timeScale = Math.min(FAST_SCALE, timeScale);
                    break;
                case FASTER_FLAG:
                    timeScale = Math.min(FASTER_SCALE, timeScale);
                    break;
                case FASTEST_FLAG:
                    timeScale = Math.min(FASTEST_SCALE, timeScale);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        parseCommandLine(args);
        PApplet.main(VirtualWorld.class);
    }
}