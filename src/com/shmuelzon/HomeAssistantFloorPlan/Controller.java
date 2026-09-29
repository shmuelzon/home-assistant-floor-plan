package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector4d;
import javax.xml.bind.DatatypeConverter;

import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.ObserverCamera;


public class Controller {
    public enum Property {COMPLETED_RENDERS, NUMBER_OF_RENDERS, FLOORS}
    public enum LightMixingMode {CSS, OVERLAY, FULL}
    public enum Renderer {YAFARAY, SUNFLOW}
    public enum Quality {HIGH, LOW}
    public enum ImageFormat {PNG, JPEG}
    public enum RenderedFloors {SELECTED, ALL}

    private static final String TRANSPARENT_IMAGE_NAME = "transparent";
    private static final String FLOOR_BUTTON_IMAGE_NAME = "floor_button";
    private static final String SELECTED_FLOOR_BUTTON_IMAGE_NAME = "floor_button_selected";

    private static final String CONTROLLER_RENDER_WIDTH = "renderWidth";
    private static final String CONTROLLER_RENDER_HEIGHT = "renderHeigh";
    private static final String CONTROLLER_LIGHT_MIXING_MODE = "lightMixingMode";
    private static final String CONTROLLER_SENSITIVTY = "sensitivity";
    private static final String CONTROLLER_RENDERER = "renderer";
    private static final String CONTROLLER_QUALITY = "quality";
    private static final String CONTROLLER_IMAGE_FORMAT = "imageFormat";
    private static final String CONTROLLER_RENDER_TIME = "renderTime";
    private static final String CONTROLLER_HOME_ASSISTANT_PATH = "homeAssistantPath";
    private static final String CONTROLLER_ADD_IMAGE_VERSION_TAGS = "addImageVersionTags";
    private static final String CONTROLLER_OUTPUT_DIRECTORY_NAME = "outputDirectoryName";
    private static final String CONTROLLER_USE_EXISTING_RENDERS = "useExistingRenders";
    private static final String CONTROLLER_RENDERED_FLOORS = "renderedFloors";

    private Home home;
    private Settings settings;
    private Camera camera;
    private List<Floor> floors = new ArrayList<>();
    private Vector4d cameraPosition;
    private Transform3D perspectiveTransform;
    private PropertyChangeSupport propertyChangeSupport;
    private int numberOfCompletedRenders;
    private AbstractPhotoRenderer photoRenderer;
    private ImageObserver renderObserver;
    private int renderWidth;
    private int renderHeight;
    private LightMixingMode lightMixingMode;
    private int sensitivity;
    private Renderer renderer;
    private Quality quality;
    private ImageFormat imageFormat;
    private List<Long> renderDateTimes;
    private String homeAssistantPath;
    private boolean addImageVersionTags;
    private String outputDirectoryName;
    private String outputRendersDirectoryName;
    private String outputFloorplanDirectoryName;
    private boolean useExistingRenders;
    private RenderedFloors renderedFloors;
    private int floorButtonWidth;
    private int floorButtonHeight;

    public Controller(Home home) {
        this.home = home;
        settings = new Settings(home);
        camera = home.getCamera().clone();
        propertyChangeSupport = new PropertyChangeSupport(this);
        loadDefaultSettings();
        buildFloors();
    }

    public void loadDefaultSettings() {
        renderWidth = settings.getInteger(CONTROLLER_RENDER_WIDTH, 1024);
        renderHeight = settings.getInteger(CONTROLLER_RENDER_HEIGHT, 576);
        lightMixingMode = LightMixingMode.valueOf(settings.get(CONTROLLER_LIGHT_MIXING_MODE, LightMixingMode.CSS.name()));
        sensitivity = settings.getInteger(CONTROLLER_SENSITIVTY, 10);
        renderer = Renderer.valueOf(settings.get(CONTROLLER_RENDERER, Renderer.YAFARAY.name()));
        quality = Quality.valueOf(settings.get(CONTROLLER_QUALITY, Quality.HIGH.name()));
        imageFormat = ImageFormat.valueOf(settings.get(CONTROLLER_IMAGE_FORMAT, ImageFormat.PNG.name()));
        renderDateTimes = settings.getListLong(CONTROLLER_RENDER_TIME, Arrays.asList(camera.getTime()));
        homeAssistantPath = settings.get(CONTROLLER_HOME_ASSISTANT_PATH, "/local/floorplan");
        addImageVersionTags = settings.getBoolean(CONTROLLER_ADD_IMAGE_VERSION_TAGS, true);
        outputDirectoryName = settings.get(CONTROLLER_OUTPUT_DIRECTORY_NAME);
        updateOutputSubDirectoryNames();
        useExistingRenders = settings.getBoolean(CONTROLLER_USE_EXISTING_RENDERS, true);
        renderedFloors = settings.getEnum(RenderedFloors.class, CONTROLLER_RENDERED_FLOORS, RenderedFloors.SELECTED);
    }

    public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
    }

    public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
    }

    public void setRenderObserver(ImageObserver observer) {
        renderObserver = observer;
    }

    public List<Floor> getFloors() {
        return floors;
    }

    public int getNumberOfTotalRenders() {
        return floors.stream().mapToInt(Floor::getNumberOfTotalRenders).sum();
    }

    private List<Level> getFloorLevels() {
        Set<Level> levelsWithEntities = Floor.getLevelsWithEntities(home);
        return home.getLevels().stream().filter(level -> level.isViewable() && levelsWithEntities.contains(level)).collect(Collectors.toList());
    }

    public boolean hasMultipleFloors() {
        return getFloorLevels().size() > 1;
    }

    public String getSelectedFloorName() {
        Level selectedLevel = home.getSelectedLevel();
        return selectedLevel != null && selectedLevel.getName() != null ? selectedLevel.getName() : "";
    }

    public RenderedFloors getRenderedFloors() {
        return renderedFloors;
    }

    public void setRenderedFloors(RenderedFloors renderedFloors) {
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        this.renderedFloors = renderedFloors;
        settings.set(CONTROLLER_RENDERED_FLOORS, renderedFloors.name());
        buildFloors();
        propertyChangeSupport.firePropertyChange(Property.FLOORS.name(), null, floors);
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
    }

    /* Determined by the floors that were built, as rendering modifies the furniture used to detect them */
    private boolean isRenderingAllFloors() {
        return floors.size() > 1;
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

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        settings.setInteger(CONTROLLER_SENSITIVTY, sensitivity);
    }

    public String getHomeAssistantPath() {
        return homeAssistantPath;
    }

    public void setHomeAssistantPath(String homeAssistantPath) {
        this.homeAssistantPath = homeAssistantPath;
        settings.set(CONTROLLER_HOME_ASSISTANT_PATH, homeAssistantPath);
    }

    public boolean getAddImageVersionTags() {
        return addImageVersionTags;
    }

    public void setAddImageVersionTags(boolean addImageVersionTags) {
        this.addImageVersionTags = addImageVersionTags;
        settings.setBoolean(CONTROLLER_ADD_IMAGE_VERSION_TAGS, addImageVersionTags);
    }

    private String imagePath(String imageName) {
        return homeAssistantPath.replaceAll("/+$", "") + "/" + imageName;
    }

    public void resetAdvancedOptionsToDefaults() {
        settings.set(CONTROLLER_SENSITIVTY, null);
        settings.set(CONTROLLER_HOME_ASSISTANT_PATH, null);
        settings.set(CONTROLLER_ADD_IMAGE_VERSION_TAGS, null);
        loadDefaultSettings();
    }

    public LightMixingMode getLightMixingMode() {
        return lightMixingMode;
    }

    public void setLightMixingMode(LightMixingMode lightMixingMode) {
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        this.lightMixingMode = lightMixingMode;
        floors.forEach(floor -> floor.buildLightsGroups(lightMixingMode));
        settings.set(CONTROLLER_LIGHT_MIXING_MODE, lightMixingMode.name());
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
    }

    public String getOutputDirectory() {
        return outputDirectoryName;
    }

    public void setOutputDirectory(String outputDirectoryName) {
        this.outputDirectoryName = outputDirectoryName;
        updateOutputSubDirectoryNames();
        if (outputDirectoryName != null && !outputDirectoryName.isEmpty())
            settings.set(CONTROLLER_OUTPUT_DIRECTORY_NAME, outputDirectoryName);
    }

    private void updateOutputSubDirectoryNames() {
        if (outputDirectoryName == null || outputDirectoryName.isEmpty()) {
            outputRendersDirectoryName = null;
            outputFloorplanDirectoryName = null;
            return;
        }
        outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
        outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
    }

    public boolean isOutputDirectorySet() {
        return outputDirectoryName != null && !outputDirectoryName.isEmpty();
    }

    public boolean getUserExistingRenders() {
        return useExistingRenders;
    }

    public void setUserExistingRenders(boolean useExistingRenders) {
        this.useExistingRenders = useExistingRenders;
        settings.setBoolean(CONTROLLER_USE_EXISTING_RENDERS, useExistingRenders);
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

    public void stop() {
        if (photoRenderer != null) {
            photoRenderer.stop();
            photoRenderer = null;
        }
    }

    public boolean isProjectEmpty() {
        return home == null || home.getFurniture().isEmpty();
    }

    /* Probing as File.canWrite() might be false on MacOS sandbox when it'll actually allow writing to */
    public boolean isOutputDirectoryWritable() {
        if (!isOutputDirectorySet())
            return false;

        File outputDirectory = new File(outputDirectoryName);
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs())
            return false;

        File probeFile = null;
        try {
            probeFile = File.createTempFile(".homeAssistantFloorPlan", null, outputDirectory);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (probeFile != null)
                probeFile.delete();
        }
    }

    public void render() throws IOException, InterruptedException {
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, 0);
        numberOfCompletedRenders = 0;
        Map<Level, Boolean> levelsVisibility = home.getLevels().stream().collect(Collectors.toMap(level -> level, Level::isVisible));

        try {
            Files.createDirectories(Paths.get(outputRendersDirectoryName));
            Files.createDirectories(Paths.get(outputFloorplanDirectoryName));

            generateTransparentImage(outputFloorplanDirectoryName + File.separator + TRANSPARENT_IMAGE_NAME + ".png");
            if (isRenderingAllFloors())
                generateFloorButtonImages();

            Map<Floor, String> floorsYaml = new HashMap<>();
            for (Floor floor : floors) {
                if (isRenderingAllFloors())
                    showLevel(floor.getLevel());
                floorsYaml.put(floor, generateFloorYaml(floor));
                restoreEntityConfiguration();
            }

            String yaml = isRenderingAllFloors() ? generateFloorsSwitchYaml(floorsYaml) : floorsYaml.get(floors.get(0));
            Files.write(Paths.get(outputDirectoryName + File.separator + "floorplan.yaml"), yaml.getBytes());
        } catch (InterruptedIOException e) {
            throw new InterruptedException();
        } catch (ClosedByInterruptException e) {
            throw new InterruptedException();
        } catch (IOException e) {
            throw e;
        } finally {
            restoreEntityConfiguration();
            levelsVisibility.forEach(Level::setVisible);
        }
    }

    private String generateFloorYaml(Floor floor) throws IOException, InterruptedException {
        String yaml = String.format(
            "type: picture-elements\n" +
            "image: %s.png%s\n" +
            "elements:\n", imagePath(TRANSPARENT_IMAGE_NAME), imageVersionSuffix(TRANSPARENT_IMAGE_NAME, true));

        floor.turnOffLightsFromOtherLevels();
        for (Scene scene : floor.getScenes()) {
            Files.createDirectories(Paths.get(outputRendersDirectoryName, floor.getName(), scene.getName()));
            Files.createDirectories(Paths.get(outputFloorplanDirectoryName, floor.getName(), scene.getName()));

            scene.prepare();

            String baseImageName = imageName(floor, scene, "base");
            BufferedImage baseImage = generateBaseRender(floor, baseImageName);
            yaml += generateLightYaml(scene, Collections.emptyList(), null, baseImageName, false);

            for (String group : floor.getLightsGroups().keySet())
                yaml += generateGroupRenders(floor, scene, group, baseImage);
        }

        yaml += generateEntitiesYaml(floor);
        if (isRenderingAllFloors())
            yaml += generateFloorButtonsYaml(floor);

        return yaml;
    }

    private String imageName(Floor floor, Scene scene, String name) {
        return Stream.of(floor.getName(), scene.getName(), name).filter(s -> !s.isEmpty()).collect(Collectors.joining(File.separator));
    }

    /* Show the level along with the levels below it, similar to selecting it in the 3D view */
    private void showLevel(Level levelToShow) {
        boolean visible = true;

        for (Level level : home.getLevels()) {
            level.setVisible(visible);
            if (level == levelToShow)
                visible = false;
        }
    }

    /* Get the camera as SH3D would place it when the level is selected: the aerial view camera is updated
     * according to the visible levels while the virtual visitor's elevation is adjusted to the level's elevation */
    private Camera getFloorCamera(Level level) {
        Map<Level, Boolean> levelsVisibility = home.getLevels().stream().collect(Collectors.toMap(l -> l, Level::isVisible));
        showLevel(level);
        Camera floorCamera = home.getCamera().clone();
        levelsVisibility.forEach(Level::setVisible);

        floorCamera.setTime(camera.getTime());
        Level selectedLevel = home.getSelectedLevel();
        if (floorCamera instanceof ObserverCamera && home.getEnvironment().isObserverCameraElevationAdjusted() && selectedLevel != null)
            floorCamera.setZ(floorCamera.getZ() - selectedLevel.getElevation() + level.getElevation());

        return floorCamera;
    }

    private void buildFloors() {
        List<Level> levels = getFloorLevels();
        floors = new ArrayList<>();

        if (renderedFloors == RenderedFloors.ALL && levels.size() > 1) {
            Set<String> floorNames = new HashSet<>();
            for (int i = 0; i < levels.size(); i++) {
                String baseName = Utils.normalizeName(levels.get(i).getName());
                if (baseName.isEmpty())
                    baseName = "level_" + i;
                String name = baseName;
                for (int suffix = 2; floorNames.contains(name); suffix++)
                    name = baseName + "_" + suffix;
                floorNames.add(name);
                floors.add(new Floor(home, levels.get(i), getFloorCamera(levels.get(i)), false, name));
            }
        }
        else
            floors.add(new Floor(home, home.getSelectedLevel(), camera, home.getEnvironment().isAllLevelsVisible(), ""));

        for (Floor floor : floors) {
            for (Entity entity : floor.getEntities())
                addEntityListeners(floor, entity);
            floor.buildLightsGroups(lightMixingMode);
            floor.buildScenes(renderDateTimes);
            repositionEntities(floor);
        }
    }

    private void addEntityListeners(Floor floor, Entity entity) {
        entity.addPropertyChangeListener(Entity.Property.POSITION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                repositionEntities(floor);
            }
        });
        entity.addPropertyChangeListener(Entity.Property.SCALE, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                repositionEntities(floor);
            }
        });
        entity.addPropertyChangeListener(Entity.Property.ALWAYS_ON, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                floor.buildLightsGroups(lightMixingMode);
                propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
            }
        });
        entity.addPropertyChangeListener(Entity.Property.DISPLAY_FURNITURE_CONDITION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                floor.buildScenes(renderDateTimes);
                propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
            }
        });
        entity.addPropertyChangeListener(Entity.Property.OPEN_FURNITURE_CONDITION, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                floor.buildScenes(renderDateTimes);
                propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), null, getNumberOfTotalRenders());
            }
        });
    }

    private void buildScenes() {
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        floors.forEach(floor -> floor.buildScenes(renderDateTimes));
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
    }

    private void build3dProjection(Camera camera) {
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

    private BufferedImage generateBaseRender(Floor floor, String imageName) throws IOException, InterruptedException {
        BufferedImage image = generateImage(floor, new ArrayList<>(), imageName);
        return generateFloorPlanImage(image, image, imageName, false);
    }

    private String getFloorplanImageExtention() {
        if (this.lightMixingMode == LightMixingMode.OVERLAY)
            return "png";
        return this.imageFormat.name().toLowerCase();
    }

    private String generateGroupRenders(Floor floor, Scene scene, String group, BufferedImage baseImage) throws IOException, InterruptedException {
        List<Entity> groupLights = floor.getLightsGroups().get(group);

        List<List<Entity>> lightCombinations = getCombinations(groupLights);
        String yaml = "";
        for (List<Entity> onLights : lightCombinations) {
            String imageName = imageName(floor, scene, String.join("_", onLights.stream().map(Entity::getName).collect(Collectors.toList())));
            BufferedImage image = generateImage(floor, onLights, imageName);
            Entity firstLight = onLights.get(0);
            boolean createOverlayImage = lightMixingMode == LightMixingMode.OVERLAY || (lightMixingMode == LightMixingMode.CSS && firstLight.getIsRgb());
            BufferedImage floorPlanImage = generateFloorPlanImage(baseImage, image, imageName, createOverlayImage);
            if (firstLight.getIsRgb()) {
                generateRedTintedImage(floorPlanImage, imageName);
                yaml += generateRgbLightYaml(scene, firstLight, imageName);
            }
            else
                yaml += generateLightYaml(scene, groupLights, onLights, imageName);
        }
        return yaml;
    }

    private void generateFloorButtonImages() throws IOException {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, Math.round(renderHeight * 0.035f)));
        FontRenderContext fontRenderContext = new FontRenderContext(null, true, true);
        int padding = font.getSize();
        int maxTextWidth = 0;
        int textHeight = 0;

        for (Floor floor : floors) {
            TextLayout text = new TextLayout(floor.getTitle().isEmpty() ? " " : floor.getTitle(), font, fontRenderContext);
            maxTextWidth = Math.max(maxTextWidth, (int)Math.ceil(text.getAdvance()));
            textHeight = Math.max(textHeight, (int)Math.ceil(text.getAscent() + text.getDescent()));
        }
        floorButtonWidth = maxTextWidth + 2 * padding;
        floorButtonHeight = textHeight + padding;

        for (Floor floor : floors) {
            Files.createDirectories(Paths.get(outputFloorplanDirectoryName, floor.getName()));
            generateFloorButtonImage(floor, font, fontRenderContext, false);
            generateFloorButtonImage(floor, font, fontRenderContext, true);
        }
    }

    private void generateFloorButtonImage(Floor floor, Font font, FontRenderContext fontRenderContext, boolean isSelected) throws IOException {
        BufferedImage image = new BufferedImage(floorButtonWidth, floorButtonHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setColor(isSelected ? new Color(255, 255, 255, 220) : new Color(255, 255, 255, 77));
        graphics.fillRoundRect(0, 0, floorButtonWidth, floorButtonHeight, floorButtonHeight, floorButtonHeight);
        if (!floor.getTitle().isEmpty()) {
            TextLayout text = new TextLayout(floor.getTitle(), font, fontRenderContext);
            graphics.setColor(Color.BLACK);
            text.draw(graphics,
                (floorButtonWidth - text.getAdvance()) / 2,
                (floorButtonHeight - text.getAscent() - text.getDescent()) / 2 + text.getAscent());
        }
        graphics.dispose();

        String imageName = floorButtonImageName(floor, isSelected);
        ImageIO.write(image, "png", new File(outputFloorplanDirectoryName + File.separator + imageName + ".png"));
    }

    private String floorButtonImageName(Floor floor, boolean isSelected) {
        return floor.getName() + File.separator + (isSelected ? SELECTED_FLOOR_BUTTON_IMAGE_NAME : FLOOR_BUTTON_IMAGE_NAME);
    }

    private String floorHash(Floor floor) {
        return Utils.percentEncode(floor.getName());
    }

    /* Buttons are stacked at the top left corner, with the highest floor at the top */
    private String generateFloorButtonsYaml(Floor currentFloor) throws IOException {
        List<Floor> floorsTopToBottom = new ArrayList<>(floors);
        Collections.reverse(floorsTopToBottom);
        double margin = floorButtonHeight / 2.0;
        double gap = floorButtonHeight / 4.0;
        String yaml = "";

        for (int i = 0; i < floorsTopToBottom.size(); i++) {
            Floor floor = floorsTopToBottom.get(i);
            boolean isSelected = floor == currentFloor;
            String imageName = floorButtonImageName(floor, isSelected);

            yaml += String.format(Locale.US,
                "  - type: image\n" +
                "    title: %s\n" +
                "    image: %s.png%s\n" +
                "    tap_action:\n" +
                "      action: %s\n" +
                "    hold_action:\n" +
                "      action: none\n" +
                "    style:\n" +
                "      top: %.2f%%\n" +
                "      left: %.2f%%\n" +
                "      width: %.2f%%\n" +
                "      transform: none\n",
                Utils.yamlQuote(floor.getTitle()), imagePath(normalizePath(imageName)), imageVersionSuffix(imageName, true),
                isSelected ? "none" : "navigate\n      navigation_path: " + Utils.yamlQuote("#" + floorHash(floor)),
                (margin + i * (floorButtonHeight + gap)) * 100.0 / renderHeight, margin * 100.0 / renderWidth,
                floorButtonWidth * 100.0 / renderWidth);
        }

        return yaml;
    }

    /* The floor to display is selected according to the URL's hash, requires the state-switch custom card.
     * It's wrapped by a picture-elements card, which clips the state-switch's negative margins that would otherwise
     * overflow and add scroll bars when used in a panel view */
    private String generateFloorsSwitchYaml(Map<Floor, String> floorsYaml) throws IOException {
        Floor defaultFloor = floors.stream().filter(floor -> floor.getLevel() == home.getSelectedLevel()).findFirst().orElse(floors.get(0));
        String yaml = String.format(
            "type: picture-elements\n" +
            "image: %s.png%s\n" +
            "elements:\n" +
            "  - type: custom:state-switch\n" +
            "    entity: hash\n" +
            "    default: %s\n" +
            "    style:\n" +
            "      left: 50%%\n" +
            "      top: 50%%\n" +
            "      width: 100%%\n" +
            "    states:\n",
            imagePath(TRANSPARENT_IMAGE_NAME), imageVersionSuffix(TRANSPARENT_IMAGE_NAME, true), Utils.yamlQuote(floorHash(defaultFloor)));

        for (Floor floor : floors) {
            yaml += String.format("      %s:\n", Utils.yamlQuote(floorHash(floor)));
            yaml += floorsYaml.get(floor).replaceAll("(?m)^(?=.)", "        ");
        }

        return yaml;
    }

    private void generateTransparentImage(String fileName) throws IOException {
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0);
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
    }

    private BufferedImage generateImage(Floor floor, List<Entity> onLights, String name) throws IOException, InterruptedException {
        String fileName = outputRendersDirectoryName + File.separator + name + ".png";

        if (useExistingRenders && Files.exists(Paths.get(fileName))) {
            BufferedImage image = ImageIO.read(Files.newInputStream(Paths.get(fileName)));
            showExistingRender(image);
            propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, ++numberOfCompletedRenders);
            return image;
        }
        floor.setLightsPower(onLights);
        BufferedImage image = renderScene(floor.getCamera());
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, ++numberOfCompletedRenders);
        return image;
    }

    private void showExistingRender(final BufferedImage image) {
        final ImageObserver observer = renderObserver;
        if (observer == null || image == null)
            return;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                observer.imageUpdate(image, ImageObserver.ALLBITS, 0, 0, image.getWidth(), image.getHeight());
            }
        });
    }

    private BufferedImage renderScene(Camera camera) throws IOException, InterruptedException {
        Map<Renderer, String> rendererToClassName = new HashMap<Renderer, String>() {{
            put(Renderer.SUNFLOW, "com.eteks.sweethome3d.j3d.PhotoRenderer");
            put(Renderer.YAFARAY, "com.eteks.sweethome3d.j3d.YafarayRenderer");
        }};
        photoRenderer = AbstractPhotoRenderer.createInstance(
            rendererToClassName.get(renderer),
            home, null, this.quality == Quality.LOW ? AbstractPhotoRenderer.Quality.LOW : AbstractPhotoRenderer.Quality.HIGH);
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        photoRenderer.render(image, camera, renderObserver);
        if (photoRenderer != null) {
            photoRenderer.dispose();
            photoRenderer = null;
        }
        if (Thread.interrupted())
            throw new InterruptedException();

        return image;
    }

    private BufferedImage generateFloorPlanImage(BufferedImage baseImage, BufferedImage image, String name, boolean createOverlayImage) throws IOException {
        String imageExtension = createOverlayImage ? "png" : getFloorplanImageExtention();
        File floorPlanFile = new File(outputFloorplanDirectoryName + File.separator + name + "." + imageExtension);

        if (!createOverlayImage) {
            ImageIO.write(image, imageExtension, floorPlanFile);
            return image;
        }

        BufferedImage overlay = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for(int x = 0; x < baseImage.getWidth(); x++) {
            for(int y = 0; y < baseImage.getHeight(); y++) {
                int diff = pixelDifference(baseImage.getRGB(x, y), image.getRGB(x, y));
                overlay.setRGB(x, y, diff > sensitivity ? image.getRGB(x, y) : 0);
            }
        }

        ImageIO.write(overlay, "png", floorPlanFile);
        return overlay;
    }

    private int pixelDifference(int first, int second) {
        int diff =
            Math.abs((first & 0xff) - (second & 0xff)) +
            Math.abs(((first >> 8) & 0xff) - ((second >> 8) & 0xff)) +
            Math.abs(((first >> 16) & 0xff) - ((second >> 16) & 0xff));
        return diff / 3;
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

    private String imageVersionSuffix(String imageName) throws IOException {
        return imageVersionSuffix(imageName, false);
    }

    private String imageVersionSuffix(String imageName, boolean forcePng) throws IOException {
        if (!addImageVersionTags)
            return "";
        return "?version=" + renderHash(imageName, forcePng);
    }

    private String renderHash(String imageName, boolean forcePng) throws IOException {
        String imageExtension = forcePng ? "png" : getFloorplanImageExtention();
        byte[] content = Files.readAllBytes(Paths.get(outputFloorplanDirectoryName + File.separator + imageName + "." + imageExtension));
        try {
            return Utils.bytesToHex(MessageDigest.getInstance("MD5").digest(content));
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
            "        image: %s.%s%s\n" +
            "        filter: none\n" +
            "        style:\n" +
            "          left: 50%%\n" +
            "          top: 50%%\n" +
            "          width: 100%%\n%s",
            generateTitle(scene, onLights), conditions, imagePath(normalizePath(imageName)),
            getFloorplanImageExtention(), imageVersionSuffix(imageName),
            includeMixBlend && lightMixingMode == LightMixingMode.CSS ? "          mix-blend-mode: lighten\n" : "");
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
            "              '%s.png%s' :\n" +
            "              '%s.png%s' }\n" +
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
            imagePath(normalizePath(imageName)), imageVersionSuffix(imageName, true), imagePath(normalizePath(imageName) + ".red"), imageVersionSuffix(imageName + ".red", true));
    }

    private String normalizePath(String fileName) {
        if (File.separator.equals("/"))
            return fileName;
        return fileName.replace('\\', '/');
    }

    private void restoreEntityConfiguration() {
        floors.forEach(Floor::restoreEntityConfiguration);
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


    private String generateEntitiesYaml(Floor floor) {
        return floor.getEntities().stream()
            .map(Entity::buildYaml).collect(Collectors.joining());
    }

    private void repositionEntities() {
        floors.forEach(this::repositionEntities);
    }

    private void repositionEntities(Floor floor) {
        build3dProjection(floor.getCamera());
        calculateEntityPositions(floor);
        moveEntityIconsToAvoidIntersection(floor);
    }

    private void calculateEntityPositions(Floor floor) {
        floor.getEntities().stream()
            .forEach(entity -> {
                Point2d entityCenter = new Point2d();
                for (HomePieceOfFurniture piece : entity.getPiecesOfFurniture())
                    entityCenter.add(getFurniture2dLocation(piece));
                entityCenter.scale(1.0 / entity.getPiecesOfFurniture().size());

                entity.setPosition(entityCenter, false);
            });
    }

    private boolean doStateIconsIntersect(Entity first, Entity second) {
        final int STATE_ICON_DEFAULT_RADIUS = 20;
        final int MARGIN_BETWEEN_ICONS = 10;

        double requiredDistanceBetweenIcons = MARGIN_BETWEEN_ICONS +
            (STATE_ICON_DEFAULT_RADIUS * first.getScale() / 100.0) +
            (STATE_ICON_DEFAULT_RADIUS * second.getScale() / 100.0);

        Point2d firstPositionInPixels = new Point2d(first.getPosition().x / 100.0 * renderWidth, first.getPosition().y / 100 * renderHeight);
        Point2d secondPositionInPixels = new Point2d(second.getPosition().x / 100.0 * renderWidth, second.getPosition().y / 100 * renderHeight);

        double x = Math.pow(firstPositionInPixels.x - secondPositionInPixels.x, 2) + Math.pow(firstPositionInPixels.y - secondPositionInPixels.y, 2);


        return x <= Math.pow(requiredDistanceBetweenIcons, 2);
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

    private Optional<Entity> stateIconWithWhichStateIconIntersects(List<Entity> entities, Entity entity) {
        return entities.stream()
            .filter(other -> {
                if (entity == other)
                    return false;
                return doStateIconsIntersect(entity, other);
            }).findFirst();
    }

    private List<Set<Entity>> findIntersectingStateIcons(Floor floor) {
        List<Set<Entity>> intersectingStateIcons = new ArrayList<Set<Entity>>();
        List<Entity> entities = floor.getEntities();

        entities.stream()
            .forEach(entity -> {
                Set<Entity> interectingSet = setWithWhichStateIconIntersects(entity, intersectingStateIcons);
                if (interectingSet != null) {
                    interectingSet.add(entity);
                    return;
                }
                Optional<Entity> intersectingStateIcon = stateIconWithWhichStateIconIntersects(entities, entity);
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

    private void moveEntityIconsToAvoidIntersection(Floor floor) {
        for (int i = 0; i < 100; i++) {
            List<Set<Entity>> intersectingStateIcons = findIntersectingStateIcons(floor);
            if (intersectingStateIcons.size() == 0)
                break;
            for (Set<Entity> set : intersectingStateIcons)
                separateStateIcons(set);
        }
    }
};
