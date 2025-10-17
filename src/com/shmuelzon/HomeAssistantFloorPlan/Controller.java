package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.Point;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.InterruptedException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector4d;

import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;


public class Controller {
    public class ProgressUpdate {
        private int completed;
        private String statusText;

        public ProgressUpdate(int completed, String statusText) {
            this.completed = completed;
            this.statusText = statusText;
        }

        public int getCompleted() {
            return completed;
        }

        public String getStatusText() {
            return statusText;
        }
    }

    public enum Property {PROGRESS_UPDATE, NUMBER_OF_RENDERS, PREVIEW_UPDATE}
    public enum Renderer {YAFARAY, SUNFLOW}
    public enum Quality {HIGH, LOW}
    public enum ImageFormat {PNG, JPEG}

    private static final String TRANSPARENT_IMAGE_NAME = "transparent";

    private static final String CONTROLLER_RENDER_WIDTH = "renderWidth";
    private static final String CONTROLLER_RENDER_HEIGHT = "renderHeigh";
    private static final String CONTROLLER_RENDERER = "renderer";
    private static final String CONTROLLER_QUALITY = "quality";
    private static final String CONTROLLER_IMAGE_FORMAT = "imageFormat";
    private static final String CONTROLLER_RENDER_TIME = "renderTime";
    private static final String CONTROLLER_OUTPUT_DIRECTORY_NAME = "outputDirectoryName";
    private static final String CONTROLLER_USE_EXISTING_RENDERS = "useExistingRenders";
    private static final String CONTROLLER_ENABLE_FLOOR_PLAN_POST_PROCESSING = "enableFloorPlanPostProcessing";
    private static final String CONTROLLER_TRANSPARENCY_THRESHOLD = "transparencyThreshold";
    private static final String CONTROLLER_MAINTAIN_ASPECT_RATIO = "maintainAspectRatio";
    private static final String CONTROLLER_GENERATE_FLOORPLAN_YAML = "generateFloorplanYaml";
    private static final String CONTROLLER_CEILING_LIGHTS_INTENSITY = "ceilingLightsIntensity";
    private static final String CONTROLLER_OTHER_LIGHTS_INTENSITY = "otherLightsIntensity";
    private static final String CONTROLLER_RENDER_CEILING_LIGHTS_INTENSITY = "renderCeilingLightsIntensity";
    private static final String CONTROLLER_RENDER_OTHER_LIGHTS_INTENSITY = "renderOtherLightsIntensity";

    private Home home;
    private Settings settings;
    private Camera camera;
    private List<Entity> lightEntities = new ArrayList<>();
    private List<Entity> otherEntities = new ArrayList<>();
    private List<Entity> otherLevelsEntities = new ArrayList<>();
    private Map<String, List<Entity>> lightsGroups = new HashMap<>();
    private Vector4d cameraPosition;
    private Transform3D perspectiveTransform;
    private PropertyChangeSupport propertyChangeSupport;
    private int numberOfCompletedRenders;
    private AbstractPhotoRenderer photoRenderer;
    private int renderWidth;
    private int renderHeight;
    private Renderer renderer;
    private Quality quality;
    private ImageFormat imageFormat;
    private List<Long> renderDateTimes;
    private String outputDirectoryName;
    private String outputRendersDirectoryName;
    private String outputFloorplanDirectoryName;
    private boolean useExistingRenders;
    private boolean enableFloorPlanPostProcessing;
    private int transparencyThreshold;
    private boolean maintainAspectRatio;
    private boolean generateFloorplanYaml;
    private int ceilingLightsIntensity;
    private int otherLightsIntensity;
    private int renderCeilingLightsIntensity;
    private int renderOtherLightsIntensity;
    private Rectangle cropArea = null;
    private Scenes scenes;

    public Controller(Home home) {
        this.home = home;
        settings = new Settings(home);
        camera = home.getCamera().clone();
        propertyChangeSupport = new PropertyChangeSupport(this);
        loadDefaultSettings();
        createHomeAssistantEntities();

        buildLightsGroups();
        buildScenes();
        repositionEntities();
    }

    public void loadDefaultSettings() {
        renderWidth = settings.getInteger(CONTROLLER_RENDER_WIDTH, 1024);
        renderHeight = settings.getInteger(CONTROLLER_RENDER_HEIGHT, 576);
        renderer = Renderer.valueOf(settings.get(CONTROLLER_RENDERER, Renderer.YAFARAY.name()));
        quality = Quality.valueOf(settings.get(CONTROLLER_QUALITY, Quality.HIGH.name()));
        imageFormat = ImageFormat.valueOf(settings.get(CONTROLLER_IMAGE_FORMAT, ImageFormat.PNG.name()));
        renderDateTimes = settings.getListLong(CONTROLLER_RENDER_TIME, Arrays.asList(camera.getTime()));
        outputDirectoryName = settings.get(CONTROLLER_OUTPUT_DIRECTORY_NAME, System.getProperty("user.home"));
        outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
        outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
        useExistingRenders = settings.getBoolean(CONTROLLER_USE_EXISTING_RENDERS, true);
        enableFloorPlanPostProcessing = settings.getBoolean(CONTROLLER_ENABLE_FLOOR_PLAN_POST_PROCESSING, true);
        transparencyThreshold = settings.getInteger(CONTROLLER_TRANSPARENCY_THRESHOLD, 30);
        maintainAspectRatio = settings.getBoolean(CONTROLLER_MAINTAIN_ASPECT_RATIO, true);
        generateFloorplanYaml = settings.getBoolean(CONTROLLER_GENERATE_FLOORPLAN_YAML, false);
        ceilingLightsIntensity = settings.getInteger(CONTROLLER_CEILING_LIGHTS_INTENSITY, 8);
        otherLightsIntensity = settings.getInteger(CONTROLLER_OTHER_LIGHTS_INTENSITY, 3);
        renderCeilingLightsIntensity = settings.getInteger(CONTROLLER_RENDER_CEILING_LIGHTS_INTENSITY, 20);
        renderOtherLightsIntensity = settings.getInteger(CONTROLLER_RENDER_OTHER_LIGHTS_INTENSITY, 10);
    }

    public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
    }

    public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
    }

    public List<Entity> getLightEntities() {
        return lightEntities;
    }

    public List<Entity> getOtherEntities() {
        return otherEntities;
    }

    public Map<String, List<Entity>> getLightsGroups() {
        return lightsGroups;
    }

    private int getNumberOfControllableLights(List<Entity> lights) {
        int numberOfControllableLights = 0;

        for (Entity light : lights)
            numberOfControllableLights += light.getAlwaysOn() ? 0 : 1;

        return numberOfControllableLights;
    }

    public int getNumberOfTotalRenders() {
        if (scenes == null)
            return 0;

        int totalRenders = 0;

        // Count stamp image
        if (enableFloorPlanPostProcessing) {
            totalRenders++;
        }

        // Count light combination renders
        int numberOfLightRenders = 1; // for base_day
        for (List<Entity> groupLights : lightsGroups.values()) {
            numberOfLightRenders += (1 << getNumberOfControllableLights(groupLights)) - 1;
        }
        totalRenders += numberOfLightRenders;

        // Count night base image
        if (renderDateTimes.size() > 1) {
            totalRenders++;
        }

        if (generateFloorplanYaml) {
            totalRenders++;
        }

        return totalRenders;
    }

    public int getRenderHeight() {
        return renderHeight;
    }

    public void setRenderHeight(int renderHeight) {
        this.renderHeight = renderHeight;
        settings.setInteger(CONTROLLER_RENDER_HEIGHT, renderHeight);
        repositionEntities();
    }

    public int getRenderWidth() {
        return renderWidth;
    }

    public void setRenderWidth(int renderWidth) {
        this.renderWidth = renderWidth;
        settings.setInteger(CONTROLLER_RENDER_WIDTH, renderWidth);
        repositionEntities();
    }

    public String getOutputDirectory() {
        return outputDirectoryName;
    }

    public void setOutputDirectory(String outputDirectoryName) {
        this.outputDirectoryName = outputDirectoryName;
        outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
        outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
        settings.set(CONTROLLER_OUTPUT_DIRECTORY_NAME, outputDirectoryName);
    }

    public boolean getUserExistingRenders() {
        return useExistingRenders;
    }

    public void setUserExistingRenders(boolean useExistingRenders) {
        this.useExistingRenders = useExistingRenders;
        settings.setBoolean(CONTROLLER_USE_EXISTING_RENDERS, useExistingRenders);
    }

    public boolean getEnableFloorPlanPostProcessing() {
        return enableFloorPlanPostProcessing;
    }

    public void setEnableFloorPlanPostProcessing(boolean enableFloorPlanPostProcessing) {
        this.enableFloorPlanPostProcessing = enableFloorPlanPostProcessing;
        settings.setBoolean(CONTROLLER_ENABLE_FLOOR_PLAN_POST_PROCESSING, enableFloorPlanPostProcessing);
    }

    public int getTransparencyThreshold() {
        return transparencyThreshold;
    }

    public void setTransparencyThreshold(int transparencyThreshold) {
        this.transparencyThreshold = transparencyThreshold;
        settings.setInteger(CONTROLLER_TRANSPARENCY_THRESHOLD, transparencyThreshold);
    }

    public boolean getMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    public void setMaintainAspectRatio(boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
        settings.setBoolean(CONTROLLER_MAINTAIN_ASPECT_RATIO, maintainAspectRatio);
    }

    public boolean getGenerateFloorplanYaml() {
        return generateFloorplanYaml;
    }

    public void setGenerateFloorplanYaml(boolean generateFloorplanYaml) {
        this.generateFloorplanYaml = generateFloorplanYaml;
        settings.setBoolean(CONTROLLER_GENERATE_FLOORPLAN_YAML, generateFloorplanYaml);
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
        settings.set(CONTROLLER_RENDERER, renderer.name());
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
        settings.set(CONTROLLER_QUALITY, quality.name());
    }

    public ImageFormat getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(ImageFormat imageFormat) {
        this.imageFormat = imageFormat;
        settings.set(CONTROLLER_IMAGE_FORMAT, imageFormat.name());
    }

    public List<Long> getRenderDateTimes() {
        return renderDateTimes;
    }

    public void setRenderDateTimes(List<Long> renderDateTimes) {
        this.renderDateTimes = renderDateTimes;
        settings.setListLong(CONTROLLER_RENDER_TIME, renderDateTimes);
        buildScenes();
    }

    public int getCeilingLightsIntensity() {
        return ceilingLightsIntensity;
    }

    public void setCeilingLightsIntensity(int ceilingLightsIntensity) {
        this.ceilingLightsIntensity = ceilingLightsIntensity;
        settings.setInteger(CONTROLLER_CEILING_LIGHTS_INTENSITY, ceilingLightsIntensity);
    }

    public int getOtherLightsIntensity() {
        return otherLightsIntensity;
    }

    public void setOtherLightsIntensity(int otherLightsIntensity) {
        this.otherLightsIntensity = otherLightsIntensity;
        settings.setInteger(CONTROLLER_OTHER_LIGHTS_INTENSITY, otherLightsIntensity);
    }

    public int getRenderCeilingLightsIntensity() {
        return renderCeilingLightsIntensity;
    }

    public void setRenderCeilingLightsIntensity(int renderCeilingLightsIntensity) {
        this.renderCeilingLightsIntensity = renderCeilingLightsIntensity;
        settings.setInteger(CONTROLLER_RENDER_CEILING_LIGHTS_INTENSITY, renderCeilingLightsIntensity);
    }

    public int getRenderOtherLightsIntensity() {
        return renderOtherLightsIntensity;
    }

    public void setRenderOtherLightsIntensity(int renderOtherLightsIntensity) {
        this.renderOtherLightsIntensity = renderOtherLightsIntensity;
        settings.setInteger(CONTROLLER_RENDER_OTHER_LIGHTS_INTENSITY, renderOtherLightsIntensity);
    }

    public void stop() {
        if (photoRenderer != null) {
            photoRenderer.stop();
            photoRenderer = null;
        }
    }

    public boolean isProjectEmpty() {
        return home == null || home.getFurniture().isEmpty();
    }

    public void render() throws IOException, InterruptedException {
        numberOfCompletedRenders = 0;
        propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(numberOfCompletedRenders, "Starting render..."));
        cropArea = null;
        int originalSkyColor = home.getEnvironment().getSkyColor();
        int originalGroundColor = home.getEnvironment().getGroundColor();
        BufferedImage stencilMask = null;

        try {
            Files.createDirectories(Paths.get(outputRendersDirectoryName));
            Files.createDirectories(Paths.get(outputFloorplanDirectoryName));

            if (enableFloorPlanPostProcessing) {
                propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(numberOfCompletedRenders, "Generating stamp..."));
                File stampFile = new File(outputFloorplanDirectoryName + File.separator + "stamp.png");
                if (useExistingRenders && stampFile.exists()) {
                    stencilMask = ImageIO.read(stampFile);
                } else {
                    home.getEnvironment().setSkyColor(AutoCrop.CROP_COLOR.getRGB());
                    home.getEnvironment().setGroundColor(AutoCrop.CROP_COLOR.getRGB());
                    camera.setTime(renderDateTimes.get(0));
                    BufferedImage tempBaseImage = renderScene();
                    stencilMask = createFloorplanStamp(tempBaseImage);
                    home.getEnvironment().setSkyColor(originalSkyColor);
                    home.getEnvironment().setGroundColor(originalGroundColor);
                }
                this.cropArea = findCropAreaFromStamp(stencilMask);
                updateEntityPositionsForCrop();
                propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(++numberOfCompletedRenders, "Stamp processed."));
            }

            generateTransparentImage(outputFloorplanDirectoryName + File.separator + TRANSPARENT_IMAGE_NAME + ".png");
            String yaml = String.format(
                "type: picture-elements\n" +
                "image: /local/floorplan/%s.png?version=%s\n" +
                "elements:\n", TRANSPARENT_IMAGE_NAME, renderHash(TRANSPARENT_IMAGE_NAME, true));

            turnOffLightsFromOtherLevels();

            camera.setTime(renderDateTimes.get(0));
            BufferedImage rawDayBaseImage = processImage("base_day", new ArrayList<>(), null, stencilMask);
            if (generateFloorplanYaml) {
                yaml += generateLightYaml(new Scene(camera, renderDateTimes, renderDateTimes.get(0), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), Collections.emptyList(), null, "base_day", false);
            }

            for (String group : lightsGroups.keySet()) {
                List<Entity> groupLights = lightsGroups.get(group);
                long renderTime = renderDateTimes.get(renderDateTimes.size() - 1);
                camera.setTime(renderTime);

                List<List<Entity>> lightCombinations = getCombinations(groupLights);
                for (List<Entity> onLights : lightCombinations) {
                    String imageName = String.join("_", onLights.stream().map(Entity::getName).collect(Collectors.toList()));
                    BufferedImage lightImage = processImage(imageName, onLights, rawDayBaseImage, stencilMask);

                    Entity firstLight = onLights.get(0);
                    if (firstLight.getIsRgb()) {
                        generateRedTintedImage(lightImage, imageName);
                        Scene nightScene = new Scene(camera, renderDateTimes, renderTime, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        yaml += generateRgbLightYaml(nightScene, firstLight, imageName);
                    } else {
                        Scene nightScene = new Scene(camera, renderDateTimes, renderTime, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        yaml += generateLightYaml(nightScene, groupLights, onLights, imageName);
                    }
                }
            }

            if (renderDateTimes.size() > 1) {
                camera.setTime(renderDateTimes.get(renderDateTimes.size() - 1));
                processImage("base_night", null, null, stencilMask);
                if (generateFloorplanYaml) {
                    yaml += generateLightYaml(new Scene(camera, renderDateTimes, renderDateTimes.get(renderDateTimes.size() - 1), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), Collections.emptyList(), null, "base_night", false);
                }
            }

            if (generateFloorplanYaml) {
                propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(++numberOfCompletedRenders, "Generating floorplan.yaml..."));
                yaml += generateEntitiesYaml();
                Files.write(Paths.get(outputDirectoryName + File.separator + "floorplan.yaml"), yaml.getBytes());
            }
        } catch (InterruptedIOException | ClosedByInterruptException e) {
            throw new InterruptedException();
        } catch (IOException e) {
            throw e;
        } finally {
            home.getEnvironment().setSkyColor(originalSkyColor);
            home.getEnvironment().setGroundColor(originalGroundColor);
            restoreEntityConfiguration();
        }
    }

private BufferedImage createFloorplanStamp(BufferedImage image) throws IOException {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage stamp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    // 1. Create initial stamp
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            if (isBackgroundColor(image.getRGB(x, y), AutoCrop.CROP_COLOR.getRGB(), transparencyThreshold)) {
                stamp.setRGB(x, y, Color.BLACK.getRGB());
            } else {
                stamp.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
    }

    // 2. Flood-fill the exterior from the borders
    Queue<Point> queue = new LinkedList<>();

    // Add all black border pixels to the queue
    for (int x = 0; x < width; x++) {
        if (stamp.getRGB(x, 0) == Color.BLACK.getRGB()) {
            queue.add(new Point(x, 0));
        }
        if (stamp.getRGB(x, height - 1) == Color.BLACK.getRGB()) {
            queue.add(new Point(x, height - 1));
        }
    }
    for (int y = 1; y < height - 1; y++) {
        if (stamp.getRGB(0, y) == Color.BLACK.getRGB()) {
            queue.add(new Point(0, y));
        }
        if (stamp.getRGB(width - 1, y) == Color.BLACK.getRGB()) {
            queue.add(new Point(width - 1, y));
        }
    }

    // Temporary color for flood fill
    int gray = Color.GRAY.getRGB();

    while (!queue.isEmpty()) {
        Point p = queue.poll();
        int x = p.x;
        int y = p.y;

        if (x < 0 || x >= width || y < 0 || y >= height || stamp.getRGB(x, y) != Color.BLACK.getRGB()) {
            continue;
        }

        stamp.setRGB(x, y, gray);

        queue.add(new Point(x + 1, y));
        queue.add(new Point(x - 1, y));
        queue.add(new Point(x, y + 1));
        queue.add(new Point(x, y - 1));
    }

    // 3. Fill holes and restore background
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int color = stamp.getRGB(x, y);
            if (color == Color.BLACK.getRGB()) {
                stamp.setRGB(x, y, Color.WHITE.getRGB()); // Fill hole
            } else if (color == gray) {
                stamp.setRGB(x, y, Color.BLACK.getRGB()); // Restore background
            }
        }
    }

    File stampFile = new File(outputFloorplanDirectoryName + File.separator + "stamp.png");
    ImageIO.write(stamp, "png", stampFile);

    return stamp;
}

private BufferedImage applyFloorplanStamp(BufferedImage image, BufferedImage stamp) {
    AutoCrop cropper = new AutoCrop();
    BufferedImage croppedStamp = cropper.crop(stamp, cropArea, maintainAspectRatio, renderWidth, renderHeight);

    BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
            if ((croppedStamp.getRGB(x, y) & 0x00FFFFFF) == 0) {
                finalImage.setRGB(x, y, 0x00000000);
            } else {
                finalImage.setRGB(x, y, image.getRGB(x, y));
            }
        }
    }
    return finalImage;
}

private Rectangle findCropAreaFromStamp(BufferedImage stamp) {
    int width = stamp.getWidth();
    int height = stamp.getHeight();
    int minX = width;
    int minY = height;
    int maxX = -1;
    int maxY = -1;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // Check for non-black pixels
            if ((stamp.getRGB(x, y) & 0x00FFFFFF) != 0) {
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
    }

    if (maxX == -1) { // Stamp is all black
        return new Rectangle(0, 0, width, height);
    }

    return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
}

    private void addEligibleFurnitureToMap(Map<String, List<HomePieceOfFurniture>> furnitureByName, List<HomePieceOfFurniture> lightsFromOtherLevels, List<HomePieceOfFurniture> furnitureList) {
        for (HomePieceOfFurniture piece : furnitureList) {
            if (piece instanceof HomeFurnitureGroup) {
                addEligibleFurnitureToMap(furnitureByName, lightsFromOtherLevels, ((HomeFurnitureGroup)piece).getFurniture());
                continue;
            }
            if (!isHomeAssistantEntity(piece.getName()) || !piece.isVisible())
                continue;
            boolean isLight = piece instanceof HomeLight;
            if (isLight && ((HomeLight)piece).getPower() == 0f)
                continue;
            if (!home.getEnvironment().isAllLevelsVisible() && piece.getLevel() != home.getSelectedLevel()) {
                if (isLight)
                    lightsFromOtherLevels.add(piece);
                continue;
            }
            if (!furnitureByName.containsKey(piece.getName()))
                furnitureByName.put(piece.getName(), new ArrayList<HomePieceOfFurniture>());
            furnitureByName.get(piece.getName()).add(piece);
        }
    }

    private void createHomeAssistantEntities() {
        Map<String, List<HomePieceOfFurniture>> furnitureByName = new HashMap<>();
        List<HomePieceOfFurniture> lightsFromOtherLevels = new ArrayList<>();
        addEligibleFurnitureToMap(furnitureByName, lightsFromOtherLevels, home.getFurniture());

        for (List<HomePieceOfFurniture> pieces : furnitureByName.values()) {
            Entity entity = new Entity(settings, pieces);
            entity.addPropertyChangeListener(Entity.Property.POSITION, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {
                    repositionEntities();
                }
            });
            entity.addPropertyChangeListener(Entity.Property.ALWAYS_ON, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {
                    buildLightsGroups();
                    propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
                }
            });
            entity.addPropertyChangeListener(Entity.Property.DISPLAY_FURNITURE_CONDITION, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {
                    buildScenes();
                    propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
                }
            });
            entity.addPropertyChangeListener(Entity.Property.OPEN_FURNITURE_CONDITION, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {
                    buildScenes();
                    propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
                }
            });

            if (entity.getIsLight())
                lightEntities.add(entity);
            else
                otherEntities.add(entity);
        }

        for (HomePieceOfFurniture piece : lightsFromOtherLevels)
            otherLevelsEntities.add(new Entity(settings, Arrays.asList(piece)));
    }

    private void buildLightsGroupsByRoom() {
        List<Room> homeRooms = home.getRooms();

        for (Room room : homeRooms) {
            if (!home.getEnvironment().isAllLevelsVisible() && room.getLevel() != home.getSelectedLevel())
                continue;
            String roomName = room.getName() != null ? room.getName() : room.getId();
            for (Entity entity : lightEntities) {
                HomePieceOfFurniture light = entity.getPiecesOfFurniture().get(0);
                if (room.containsPoint(light.getX(), light.getY(), 0) && room.getLevel() == light.getLevel()) {
                    if (!lightsGroups.containsKey(roomName))
                        lightsGroups.put(roomName, new ArrayList<>());
                    lightsGroups.get(roomName).add(entity);
                }
            }
        }
    }

    private void buildLightsGroupsByLight() {
        for (Entity entity : lightEntities) {
            lightsGroups.put(entity.getName(), new ArrayList<>());
            lightsGroups.get(entity.getName()).add(entity);
        }
    }

    private void buildLightsGroupsByHome() {
        lightsGroups.put("Home", new ArrayList<>());
        for (Entity entity : lightEntities)
            lightsGroups.get("Home").add(entity);
    }

    private void buildLightsGroups() {
        lightsGroups.clear();

        buildLightsGroupsByLight();
    }

    private void buildScenes() {
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        scenes = new Scenes(camera);
        scenes.setRenderingTimes(renderDateTimes);
        scenes.setEntitiesToShowOrHide(otherEntities.stream().filter(entity -> { return entity.getDisplayFurnitureCondition() != Entity.DisplayFurnitureCondition.ALWAYS; }).collect(Collectors.toList()));
        scenes.setEntitiesToOpenOrClose(otherEntities.stream().filter(entity -> { return entity.getOpenFurnitureCondition() != Entity.OpenFurnitureCondition.ALWAYS; }).collect(Collectors.toList()));
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
    }

    private boolean isHomeAssistantEntity(String name) {
        List<String> sensorPrefixes = Arrays.asList(
            "air_quality.",
            "alarm_control_panel.",
            "assist_satellite.",
            "binary_sensor.",
            "button.",
            "camera.",
            "climate.",
            "cover.",
            "device_tracker.",
            "fan.",
            "humidifier.",
            "input_boolean.",
            "input_button.",
            "lawn_mower.",
            "light.",
            "lock.",
            "media_player.",
            "remote.",
            "sensor.",
            "siren.",
            "switch.",
            "sun.",
            "todo.",
            "update.",
            "vacuum.",
            "valve.",
            "water_heater.",
            "weather."
        );

        if (name == null)
            return false;

        return sensorPrefixes.stream().anyMatch(name::startsWith);
    }

    private void build3dProjection() {
        cameraPosition = new Vector4d(camera.getX(), camera.getZ(), camera.getY(), 0);

        Transform3D yawRotation = new Transform3D();
        yawRotation.rotY(camera.getYaw());

        Transform3D pitchRotation = new Transform3D();
        pitchRotation.rotX(-camera.getPitch());

        perspectiveTransform = new Transform3D();
        perspectiveTransform.perspective(camera.getFieldOfView(), (double)renderWidth / renderHeight, 0.1, 100);
        perspectiveTransform.mul(pitchRotation);
        perspectiveTransform.mul(yawRotation);
    }

    private String getFloorplanImageExtention() {
        return this.imageFormat.name().toLowerCase();
    }

    private void generateTransparentImage(String fileName) throws IOException {
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0);
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
    }

    private BufferedImage processAndSaveFinalImage(BufferedImage image, BufferedImage stencilMask, String imageName) throws IOException {
        BufferedImage processedImage = image;

        if (enableFloorPlanPostProcessing) {
            if (cropArea != null) {
                AutoCrop cropper = new AutoCrop();
                processedImage = cropper.crop(image, cropArea, maintainAspectRatio, renderWidth, renderHeight);
            }

            // Apply stamp before green removal, as stamp provides the definitive outer boundary.
        if (cropArea != null && stencilMask != null) {
                processedImage = applyFloorplanStamp(processedImage, stencilMask);
            }

            // Now, remove any green from the interior.
            processedImage = removeGreenBackground(processedImage);
        }
        
        saveFloorPlanImage(processedImage, imageName, "png");
        propertyChangeSupport.firePropertyChange(Property.PREVIEW_UPDATE.name(), null, processedImage);
        return processedImage;
    }

    private BufferedImage removeGreenBackground(BufferedImage image) {
        BufferedImage transparentImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                
                if (isBackgroundColor(rgb, AutoCrop.CROP_COLOR.getRGB(), transparencyThreshold)) {
                    transparentImage.setRGB(x, y, 0x00000000);
                } else {
                    // Keep original pixel with full alpha
                    transparentImage.setRGB(x, y, rgb | 0xFF000000);
                }
            }
        }
        
        return transparentImage;
    }

    private boolean isBackgroundColor(int color1, int background, int tolerance) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (background >> 16) & 0xFF;
        int g2 = (background >> 8) & 0xFF;
        int b2 = background & 0xFF;

        return Math.abs(r1 - r2) <= tolerance && Math.abs(g1 - g2) <= tolerance && Math.abs(b1 - b2) <= tolerance;
    }

    private void updateEntityPositionsForCrop() {
        if (cropArea == null) return;
        
        int originalRenderWidth = this.renderWidth;
        int originalRenderHeight = this.renderHeight;
        
        try {
            if (!maintainAspectRatio) {
                this.renderWidth = cropArea.width;
                this.renderHeight = cropArea.height;
            }
            
            // Recalculate 3D projection and entity positions based on the new, cropped dimensions
            // This is necessary so the UI icons in the YAML file have the correct coordinates.
            repositionEntities();

            // The original entity positions are still needed for the actual crop calculation.
            // This part of the original logic seems to have been flawed, as it was recalculating
            // positions based on already calculated positions. The call to repositionEntities
            // handles the projection correctly. The entity.setPosition calls are now redundant
            // as repositionEntities will handle it. We will rely on the repositionEntities method
            // to correctly calculate the new icon positions based on the temporary cropped dimensions.

        } finally {
            // Restore the original render dimensions immediately so that all subsequent
            // image generation (`generateImage`, etc.) uses the full, uncropped size.
            this.renderWidth = originalRenderWidth;
            this.renderHeight = originalRenderHeight;
        }
    }

    private void saveRawRender(BufferedImage image, String name) throws IOException {
        String fileName = outputRendersDirectoryName + File.separator + name + ".png";
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
    }

    private BufferedImage renderScene() throws IOException, InterruptedException {
        Map<Renderer, String> rendererToClassName = new HashMap<Renderer, String>() {{
            put(Renderer.SUNFLOW, "com.eteks.sweethome3d.j3d.PhotoRenderer");
            put(Renderer.YAFARAY, "com.eteks.sweethome3d.j3d.YafarayRenderer");
        }};
        photoRenderer = AbstractPhotoRenderer.createInstance(
            rendererToClassName.get(renderer),
            home, null, this.quality == Quality.LOW ? AbstractPhotoRenderer.Quality.LOW : AbstractPhotoRenderer.Quality.HIGH);
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        photoRenderer.render(image, camera, null);
        if (photoRenderer != null) {
            photoRenderer.dispose();
            photoRenderer = null;
        }
        if (Thread.interrupted())
            throw new InterruptedException();

        return image;
    }

    private BufferedImage generateFloorPlanImage(BufferedImage baseImage, BufferedImage image, boolean createOverlayImage) throws IOException {
        if (!createOverlayImage) {
            return image;
        }

        BufferedImage overlay = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for(int x = 0; x < baseImage.getWidth(); x++) {
            for(int y = 0; y < baseImage.getHeight(); y++) {
                int basePixel = baseImage.getRGB(x, y);
                if (((basePixel >> 24) & 0xff) == 0) {
                    continue;
                }
                overlay.setRGB(x, y, image.getRGB(x, y) | 0xFF000000);
            }
        }
        return overlay;
    }

    private void saveFloorPlanImage(BufferedImage image, String name, String extension) throws IOException {
        File floorPlanFile = new File(outputFloorplanDirectoryName + File.separator + name + "." + extension);
        ImageIO.write(image, extension, floorPlanFile);
    }

    private BufferedImage generateRedTintedImage(BufferedImage image, String imageName) throws IOException {
        File redTintedFile = new File(outputFloorplanDirectoryName + File.separator + imageName + ".red.png");
        BufferedImage tintedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for(int x = 0; x < image.getWidth(); x++) {
            for(int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                if (rgb == 0)
                    continue;
                Color original = new Color(rgb, true);
                float hsb[] = Color.RGBtoHSB(original.getRed(), original.getGreen(), original.getBlue(), null);
                Color redTint = Color.getHSBColor(1.0f, 0.75f, hsb[2]);
                tintedImage.setRGB(x, y, redTint.getRGB());
            }
        }

        ImageIO.write(tintedImage, "png", redTintedFile);
        return tintedImage;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[b >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[b & 0x0F];
        }
        return new String(hexChars);
    }

    private String renderHash(String imageName) throws IOException {
        return renderHash(imageName, false);
    }

    private String renderHash(String imageName, boolean forcePng) throws IOException {
        String imageExtension = forcePng ? "png" : getFloorplanImageExtention();
        byte[] content = Files.readAllBytes(Paths.get(outputFloorplanDirectoryName + File.separator + imageName + "." + imageExtension));
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(content);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Long.toString(System.currentTimeMillis() / 1000L);
        }
    }

    private String generateLightYaml(Scene scene, List<Entity> lights, List<Entity> onLights, String imageName) throws IOException {
        return generateLightYaml(scene, lights, onLights, imageName, true);
    }

    private String generateTitle(Scene scene, List<Entity> onLights) {
        List<String> titleParts = onLights != null ?  onLights.stream().map(Entity::getName).collect(Collectors.toList()) : new ArrayList<>(Arrays.asList("Base"));
        titleParts.add(0, scene.getTitle());

        return titleParts.stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
    }

    private String generateLightYaml(Scene scene, List<Entity> lights, List<Entity> onLights, String imageName, boolean includeMixBlend) throws IOException {
        String conditions = "";
        for (Entity light : lights) {
            conditions += String.format(
                "      - condition: state\n" +
                "        entity: %s\n" +
                "        state: '%s'\n",
                light.getName(), onLights.contains(light) ? "on" : "off");
        }
        conditions += scene.getConditions();
        if (conditions.length() == 0)
            conditions = "      []\n";

        return String.format(
            "  - type: conditional\n" +
            "    title: %s\n" +
            "    conditions:\n%s" +
            "    elements:\n" +
            "      - type: image\n" +
            "        tap_action:\n" +
            "          action: none\n" +
            "        hold_action:\n" +
            "          action: none\n" +
            "        image: /local/floorplan/%s.%s?version=%s\n" +
            "        filter: none\n" +
            "        style:\n" +
            "          left: 50%%\n" +
            "          top: 50%%\n" +
            "          width: 100%%\n%s",
            generateTitle(scene, onLights), conditions, normalizePath(imageName),
            getFloorplanImageExtention(), renderHash(imageName),
            includeMixBlend ? "          mix-blend-mode: lighten\n" : "");
    }

    private String generateRgbLightYaml(Scene scene, Entity light, String imageName) throws IOException {
        String lightName = light.getName();

        return String.format(
            "  - type: conditional\n" +
            "    title: %s\n" +
            "    conditions:\n" +
            "      - condition: state\n" +
            "        entity: %s\n" +
            "        state: 'on'\n%s" +
            "    elements:\n" +
            "      - type: custom:config-template-card\n" +
            "        variables:\n" +
            "          LIGHT_STATE: states['%s'].state\n" +
            "          COLOR_MODE: states['%s'].attributes.color_mode\n" +
            "          LIGHT_COLOR: states['%s'].attributes.hs_color\n" +
            "          BRIGHTNESS: states['%s'].attributes.brightness\n" +
            "          isInColoredMode: colorMode => ['hs', 'rgb', 'rgbw', 'rgbww', 'white', 'xy'].includes(colorMode)\n" +
            "        entities:\n" +
            "          - %s\n" +
            "        element:\n" +
            "          type: image\n" +
            "          image: >-\n" +
            "              ${!isInColoredMode(COLOR_MODE) || (isInColoredMode(COLOR_MODE) && LIGHT_COLOR && LIGHT_COLOR[0] == 0 && LIGHT_COLOR[1] == 0) ?\n" +
            "              '/local/floorplan/%s.png?version=%s' :\n" +
            "              '/local/floorplan/%s.png?version=%s' }\n" +
            "        style:\n" +
            "          filter: '${ \"hue-rotate(\" + (isInColoredMode(COLOR_MODE) && LIGHT_COLOR ? LIGHT_COLOR[0] : 0) + \"deg) saturate(\" + (LIGHT_COLOR ? LIGHT_COLOR[1] / 100 : 1) + \")\"}'\n" +
            "          opacity: '${LIGHT_STATE === ''on'' ? (BRIGHTNESS / 255) : ''100''}'\n" +
            "          mix-blend-mode: lighten\n" +
            "          pointer-events: none\n" +
            "          left: 50%%\n" +
            "          top: 50%%\n" +
            "          width: 100%%\n",
            generateTitle(scene, Arrays.asList(light)),
            lightName, scene.getConditions(), lightName, lightName, lightName, lightName, lightName,
            normalizePath(imageName), renderHash(imageName, true), normalizePath(imageName) + ".red", renderHash(imageName + ".red", true));
    }

    private String normalizePath(String fileName) {
        if (File.separator.equals("/"))
            return fileName;
        return fileName.replace('\\', '/');
    }

    private void turnOffLightsFromOtherLevels() {
        otherLevelsEntities.forEach(entity -> entity.setLightPower(false));
    }

    private void restoreEntityConfiguration() {
        Stream.of(lightEntities, otherEntities, otherLevelsEntities).flatMap(Collection::stream)
            .forEach(Entity::restoreConfiguration);
    }

    private void removeAlwaysOnLights(List<Entity> inputList) {
        ListIterator<Entity> iter = inputList.listIterator();

        while (iter.hasNext()) {
            if (iter.next().getAlwaysOn())
                iter.remove();
        }
    }

    public List<List<Entity>> getCombinations(List<Entity> inputSet) {
        List<List<Entity>> combinations = new ArrayList<>();
        List<Entity> inputList = new ArrayList<>(inputSet);

        removeAlwaysOnLights(inputList);
        _getCombinations(inputList, 0, new ArrayList<Entity>(), combinations);

        return combinations;
    }

    private void _getCombinations(List<Entity> inputList, int currentIndex, List<Entity> currentCombination, List<List<Entity>> combinations) {
        if (currentCombination.size() > 0)
            combinations.add(new ArrayList<>(currentCombination));

        for (int i = currentIndex; i < inputList.size(); i++) {
            currentCombination.add(inputList.get(i));
            _getCombinations(inputList, i + 1, currentCombination, combinations);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    private Point2d getFurniture2dLocation(HomePieceOfFurniture piece) {
        float levelOffset = piece.getLevel() != null ? piece.getLevel().getElevation() : 0;
        Vector4d objectPosition = new Vector4d(piece.getX(), (((piece.getElevation() * 2) + piece.getHeight()) / 2) + levelOffset, piece.getY(), 0);

        objectPosition.sub(cameraPosition);
        perspectiveTransform.transform(objectPosition);
        objectPosition.scale(1 / objectPosition.w);

        return new Point2d((objectPosition.x * 0.5 + 0.5) * 100.0, (objectPosition.y * 0.5 + 0.5) * 100.0);
    }


    private String generateEntitiesYaml() {
        return Stream.concat(lightEntities.stream(), otherEntities.stream())
            .map(Entity::buildYaml).collect(Collectors.joining());
    }

    private void repositionEntities() {
        build3dProjection();
        calculateEntityPositions();
        moveEntityIconsToAvoidIntersection();
    }

    private void calculateEntityPositions() {
        Stream.concat(lightEntities.stream(), otherEntities.stream())
            .forEach(entity -> {
                Point2d entityCenter = new Point2d();
                for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture())
                    entityCenter.add(getFurniture2dLocation(piece));
                entityCenter.scale(1.0 / entity.getPiecesOfFurniture().size());

                entity.setPosition(entityCenter, false);
            });
    }

    private boolean doStateIconsIntersect(Entity first, Entity second) {
        final double STATE_ICON_RAIDUS_INCLUDING_MARGIN = 25.0;

        Point2d firstPositionInPixels = new Point2d(first.getPosition().x / 100.0 * renderWidth, first.getPosition().y / 100 * renderHeight);
        Point2d secondPositionInPixels = new Point2d(second.getPosition().x / 100.0 * renderWidth, second.getPosition().y / 100 * renderHeight);

        double x = Math.pow(firstPositionInPixels.x - secondPositionInPixels.x, 2) + Math.pow(firstPositionInPixels.y - secondPositionInPixels.y, 2);

        return x <= Math.pow(STATE_ICON_RAIDUS_INCLUDING_MARGIN * 2, 2);
    }

    private boolean doesStateIconIntersectWithSet(Entity entity, Set<Entity> entities) {
        for (Entity other : entities) {
            if (doStateIconsIntersect(entity, other))
                return true;
        }
        return false;
    }

    private Set<Entity> setWithWhichStateIconIntersects(Entity entity, List<Set<Entity>> entities) {
        for (Set<Entity> set : entities) {
            if (doesStateIconIntersectWithSet(entity, set))
                return set;
        }
        return null;
    }

    private Optional<Entity> stateIconWithWhichStateIconIntersects(Entity entity) {
        return Stream.concat(lightEntities.stream(), otherEntities.stream())
            .filter(other -> {
                if (entity == other)
                    return false;
                return doStateIconsIntersect(entity, other);
            }).findFirst();
    }

    private List<Set<Entity>> findIntersectingStateIcons() {
        List<Set<Entity>> intersectingStateIcons = new ArrayList<Set<Entity>>();

        Stream.concat(lightEntities.stream(), otherEntities.stream())
            .forEach(entity -> {
                Set<Entity> interectingSet = setWithWhichStateIconIntersects(entity, intersectingStateIcons);
                if (interectingSet != null) {
                    interectingSet.add(entity);
                    return;
                }
                Optional<Entity> intersectingStateIcon = stateIconWithWhichStateIconIntersects(entity);
                if (!intersectingStateIcon.isPresent())
                    return;
                Set<Entity> intersectingGroup = new HashSet<Entity>();
                intersectingGroup.add(entity);
                intersectingGroup.add(intersectingStateIcon.get());
                intersectingStateIcons.add(intersectingGroup);
            });

        return intersectingStateIcons;
    }

    private Point2d getCenterOfStateIcons(Set<Entity> entities) {
        Point2d centerPostition = new Point2d();
        for (Entity entity : entities )
            centerPostition.add(entity.getPosition());
        centerPostition.scale(1.0 / entities.size());
        return centerPostition;
    }

    private void separateStateIcons(Set<Entity> entities) {
        final double STEP_SIZE = 2.0;

        Point2d centerPostition = getCenterOfStateIcons(entities);

        for (Entity entity : entities) {
            Vector2d direction = new Vector2d(entity.getPosition().x - centerPostition.x, entity.getPosition().y - centerPostition.y);

            if (direction.length() == 0) {
                double[] randomRepeatableDirection = { entity.getId().hashCode(), entity.getName().hashCode() };
                direction.set(randomRepeatableDirection);
            }

            direction.normalize();
            direction.x = direction.x * (100.0 * (STEP_SIZE / renderWidth));
            direction.y = direction.y * (100.0 * (STEP_SIZE / renderHeight));
            entity.move(direction);
        }
    }

    private void moveEntityIconsToAvoidIntersection() {
        for (int i = 0; i < 100; i++) {
            List<Set<Entity>> intersectingStateIcons = findIntersectingStateIcons();
            if (intersectingStateIcons.size() == 0)
                break;
            for (Set<Entity> set : intersectingStateIcons)
                separateStateIcons(set);
        }
    }

    private BufferedImage processImage(String imageName, List<Entity> onLights, BufferedImage baseImage, BufferedImage stencilMask) throws IOException, InterruptedException {
        File finalImageFile = new File(outputFloorplanDirectoryName + File.separator + imageName + "." + getFloorplanImageExtention());
        File rawRenderFile = new File(outputRendersDirectoryName + File.separator + imageName + ".png");

        if (useExistingRenders && finalImageFile.exists()) {
            propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(++numberOfCompletedRenders, "Skipping " + imageName + "..."));
            if (rawRenderFile.exists()) {
                return ImageIO.read(rawRenderFile);
            }
            return ImageIO.read(finalImageFile);
        }

        propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(numberOfCompletedRenders, "Rendering " + imageName + "..."));

        BufferedImage rawImage;
        if (onLights == null) { // Special case for base_night
             rawImage = generateNightBaseImage();
        } else {
            Map<HomeLight, Float> originalPowers = new HashMap<>();
            try {
                // Determine which lights should be on for this render pass
                Set<Entity> lightsToTurnOn = new HashSet<>(onLights);
                lightEntities.stream()
                    .filter(Entity::getAlwaysOn)
                    .forEach(lightsToTurnOn::add);

                for (Entity light : lightEntities) {
                    boolean isLightOn = lightsToTurnOn.contains(light);

                    // Save original power for all underlying HomeLight objects
                    for (HomePieceOfFurniture piece : light.getPiecesOfFurniture()) {
                        if (piece instanceof HomeLight) {
                            originalPowers.put((HomeLight)piece, ((HomeLight)piece).getPower());
                        }
                    }

                    // Set the entity state. This likely also sets the HomeLight power to 0.0 or 1.0
                    light.setLightPower(isLightOn);

                    // If the light is on, override the power with the desired intensity
                    if (isLightOn) {
                        float intensity = light.getName().toLowerCase().contains("deckenlampe")
                            ? renderCeilingLightsIntensity
                            : renderOtherLightsIntensity;
                        for (HomePieceOfFurniture piece : light.getPiecesOfFurniture()) {
                            if (piece instanceof HomeLight) {
                                ((HomeLight)piece).setPower(intensity / 100.0f);
                            }
                        }
                    }
                }

                rawImage = renderScene();

            } finally {
                // Restore all original power values after rendering
                for (Map.Entry<HomeLight, Float> entry : originalPowers.entrySet()) {
                    entry.getKey().setPower(entry.getValue());
                }
            }
        }

        saveRawRender(rawImage, imageName);

        BufferedImage processedImage;
        if (baseImage != null) {
            boolean isRgb = onLights.stream().anyMatch(Entity::getIsRgb);
            processedImage = generateFloorPlanImage(baseImage, rawImage, isRgb);
        } else {
            processedImage = rawImage;
        }

        processAndSaveFinalImage(processedImage, stencilMask, imageName);

        propertyChangeSupport.firePropertyChange(Property.PROGRESS_UPDATE.name(), null, new ProgressUpdate(++numberOfCompletedRenders, "Finished " + imageName + "."));

        return rawImage;
    }

    private BufferedImage generateNightBaseImage() throws IOException, InterruptedException {
        Map<HomeLight, Float> originalPowers = new HashMap<>();
        try {
            Stream.concat(lightEntities.stream(), otherEntities.stream())
                .filter(entity -> entity.getName().startsWith("light."))
                .forEach(light -> {
                    if (!light.getAlwaysOn()) {
                        light.setLightPower(true);

                        for (HomePieceOfFurniture piece : light.getPiecesOfFurniture()) {
                            if (piece instanceof HomeLight) {
                                HomeLight homeLight = (HomeLight) piece;
                                originalPowers.put(homeLight, homeLight.getPower());
                                float intensity = light.getName().toLowerCase().contains("deckenlampe")
                                ? ceilingLightsIntensity
                                : otherLightsIntensity;
                                homeLight.setPower(intensity / 100.0f);
                            }
                        }
                    }
                });
            return renderScene();
        } finally {
            for (Map.Entry<HomeLight, Float> entry : originalPowers.entrySet()) {
                entry.getKey().setPower(entry.getValue());
            }
        }
    }
};
