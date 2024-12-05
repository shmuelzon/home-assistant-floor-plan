package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    public enum Property {COMPLETED_RENDERS, NUMBER_OF_RENDERS, ENTITY_ATTRIBUTE_CHANGED}
    public enum LightMixingMode {CSS, OVERLAY, FULL}
    public enum Renderer {YAFARAY, SUNFLOW}
    public enum Quality {HIGH, LOW}
    public enum EntityDisplayType {BADGE, ICON, LABEL, NONE}
    public enum EntityTapAction {MORE_INFO, NONE, TOGGLE}

    private static final String TRANSPARENT_FILE_NAME = "transparent.png";

    private static final String CONTROLLER_RENDER_WIDTH = "renderWidth";
    private static final String CONTROLLER_RENDER_HEIGHT = "renderHeigh";
    private static final String CONTROLLER_LIGHT_MIXING_MODE = "lightMixingMode";
    private static final String CONTROLLER_SENSITIVTY = "sensitivity";
    private static final String CONTROLLER_RENDERER = "renderer";
    private static final String CONTROLLER_QUALITY = "quality";
    private static final String CONTROLLER_RENDER_TIME = "renderTime";
    private static final String CONTROLLER_OUTPUT_DIRECTORY_NAME = "outputDirectoryName";
    private static final String CONTROLLER_USE_EXISTING_RENDERS = "useExistingRenders";
    private static final String CONTROLLER_ENTITY_DISPLAY_TYPE = "displayType";
    private static final String CONTROLLER_ENTITY_TAP_ACTION = "tapAction";
    private static final String CONTROLLER_ENTITY_ALWAYS_ON = "alwaysOn";
    private static final String CONTROLLER_ENTITY_IS_RGB = "isRgb";

    private Home home;
    private Settings settings;
    private Camera camera;
    private Map<String, List<HomeLight>> lights;
    private Map<String, Map<String, List<HomeLight>>> lightsGroups;
    private List<String> lightsNames;
    private Map<HomeLight, Float> lightsPower;
    private Map<String, Entity> homeAssistantEntities;
    private Vector4d cameraPosition;
    private Transform3D perspectiveTransform;
    private PropertyChangeSupport propertyChangeSupport;
    private int numberOfCompletedRenders;
    private AbstractPhotoRenderer photoRenderer;
    private int renderWidth;
    private int renderHeight;
    private LightMixingMode lightMixingMode;
    private int sensitivity;
    private Renderer renderer;
    private Quality quality;
    private long renderDateTime;
    private String outputDirectoryName;
    private String outputRendersDirectoryName;
    private String outputFloorplanDirectoryName;
    private boolean useExistingRenders;

    private class Entity {
        public String name;
        public Point2d position;
        public EntityDisplayType displayType;
        public EntityTapAction tapAction;
        public String title;
        public boolean alwaysOn;
        public boolean isRgb;

        public Entity(String name, Point2d position, EntityDisplayType defaultDisplayType, EntityTapAction defaultTapAction, String title) {
            this.name = name;
            this.position = position;
            this.displayType = EntityDisplayType.valueOf(settings.get(name + "." + CONTROLLER_ENTITY_DISPLAY_TYPE, defaultDisplayType.name()));
            this.tapAction = EntityTapAction.valueOf(settings.get(name + "." + CONTROLLER_ENTITY_TAP_ACTION, defaultTapAction.name()));
            this.title = title;
            this.alwaysOn = settings.getBoolean(name + "." + CONTROLLER_ENTITY_ALWAYS_ON, false);
            this.isRgb = settings.getBoolean(name + "." + CONTROLLER_ENTITY_IS_RGB, false);
        }
    }

    public Controller(Home home) {
        this.home = home;
        settings = new Settings(home);
        camera = home.getCamera().clone();
        propertyChangeSupport = new PropertyChangeSupport(this);
        lights = getEnabledLights();
        lightsNames = new ArrayList<String>(lights.keySet());
        loadDefaultSettings();
        lightsGroups = getLightsGroups(lights);
        lightsPower = getLightsPower(lights);
        homeAssistantEntities = new HashMap<String, Entity>();
        generateHomeAssistantEntities();
    }

    public void loadDefaultSettings() {
        renderWidth = settings.getInteger(CONTROLLER_RENDER_WIDTH, 1024);
        renderHeight = settings.getInteger(CONTROLLER_RENDER_HEIGHT, 576);
        lightMixingMode = LightMixingMode.valueOf(settings.get(CONTROLLER_LIGHT_MIXING_MODE, LightMixingMode.CSS.name()));
        sensitivity = settings.getInteger(CONTROLLER_SENSITIVTY, 10);
        renderer = Renderer.valueOf(settings.get(CONTROLLER_RENDERER, Renderer.YAFARAY.name()));
        quality = Quality.valueOf(settings.get(CONTROLLER_QUALITY, Quality.HIGH.name()));
        renderDateTime = settings.getLong(CONTROLLER_RENDER_TIME, camera.getTime());
        outputDirectoryName = settings.get(CONTROLLER_OUTPUT_DIRECTORY_NAME, System.getProperty("user.home"));
        outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
        outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
        useExistingRenders = settings.getBoolean(CONTROLLER_USE_EXISTING_RENDERS, true);
    }

    public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
    }

    public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
    }

    public Map<String, Map<String, List<HomeLight>>> getLightsGroups() {
        return lightsGroups;
    }

    private int getNumberOfControllableLights(Set<String> lights) {
        int numberOfControllableLights = 0;

        for (String lightName : lights)
            numberOfControllableLights += getEntityAlwaysOn(lightName) ? 0 : 1;

        return numberOfControllableLights;
    }

    public int getNumberOfTotalRenders() {
        int numberOfTotalRenders = 1;

        for (Map<String, List<HomeLight>> groupLights : lightsGroups.values()) {
            numberOfTotalRenders += (1 << getNumberOfControllableLights(groupLights.keySet())) - 1;
        }
        return numberOfTotalRenders;
    }

    public int getRenderHeight() {
        return renderHeight;
    }

    public void setRenderHeight(int renderHeight) {
        this.renderHeight = renderHeight;
        settings.setInteger(CONTROLLER_RENDER_HEIGHT, renderHeight);
        generateHomeAssistantEntities();
    }

    public int getRenderWidth() {
        return renderWidth;
    }

    public void setRenderWidth(int renderWidth) {
        this.renderWidth = renderWidth;
        settings.setInteger(CONTROLLER_RENDER_WIDTH, renderWidth);
        generateHomeAssistantEntities();
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        settings.setInteger(CONTROLLER_SENSITIVTY, sensitivity);
    }

    public LightMixingMode getLightMixingMode() {
        return lightMixingMode;
    }

    public void setLightMixingMode(LightMixingMode lightMixingMode) {
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        this.lightMixingMode = lightMixingMode;
        lightsGroups = getLightsGroups(lights);
        settings.set(CONTROLLER_LIGHT_MIXING_MODE, lightMixingMode.name());
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
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

    public long getRenderDateTime() {
        return renderDateTime;
    }

    public void setRenderDateTime(long renderDateTime) {
        this.renderDateTime = renderDateTime;
        settings.setLong(CONTROLLER_RENDER_TIME, renderDateTime);
    }

    public EntityDisplayType getEntityDisplayType(String entityName) {
        return homeAssistantEntities.get(entityName).displayType;
    }

    public void setEntityDisplayType(String entityName, EntityDisplayType displayType) {
        homeAssistantEntities.get(entityName).displayType = displayType;
        settings.set(entityName + "." + CONTROLLER_ENTITY_DISPLAY_TYPE, displayType.name());
    }

    public EntityTapAction getEntityTapAction(String entityName) {
        return homeAssistantEntities.get(entityName).tapAction;
    }

    public void setEntityTapAction(String entityName, EntityTapAction tapAction) {
        homeAssistantEntities.get(entityName).tapAction = tapAction;
        settings.set(entityName + "." + CONTROLLER_ENTITY_TAP_ACTION, tapAction.name());
    }

    public boolean getEntityAlwaysOn(String entityName) {
        return homeAssistantEntities.get(entityName).alwaysOn;
    }

    public void setEntityAlwaysOn(String entityName, boolean alwaysOn) {
        int oldNumberOfTotalRenders = getNumberOfTotalRenders();
        boolean oldAlwaysOn = homeAssistantEntities.get(entityName).alwaysOn;
        homeAssistantEntities.get(entityName).alwaysOn = alwaysOn;
        settings.setBoolean(entityName + "." + CONTROLLER_ENTITY_ALWAYS_ON, alwaysOn);
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotalRenders, getNumberOfTotalRenders());
        propertyChangeSupport.firePropertyChange(Property.ENTITY_ATTRIBUTE_CHANGED.name(), oldAlwaysOn, alwaysOn);
    }

    public boolean getEntityIsRgb(String entityName) {
        return homeAssistantEntities.get(entityName).isRgb;
    }

    public void setEntityIsRgb(String entityName, boolean isRgb) {
        boolean oldIsRgb = homeAssistantEntities.get(entityName).isRgb;
        homeAssistantEntities.get(entityName).isRgb = isRgb;
        settings.setBoolean(entityName + "." + CONTROLLER_ENTITY_IS_RGB, isRgb);
        propertyChangeSupport.firePropertyChange(Property.ENTITY_ATTRIBUTE_CHANGED.name(), oldIsRgb, isRgb);
    }

    public void resetEntitySettings(String entityName) {
        boolean oldAlwaysOn = homeAssistantEntities.get(entityName).alwaysOn;
        boolean oldIsRgb = homeAssistantEntities.get(entityName).isRgb;
        settings.set(entityName + "." + CONTROLLER_ENTITY_DISPLAY_TYPE, null);
        settings.set(entityName + "." + CONTROLLER_ENTITY_TAP_ACTION, null);
        settings.set(entityName + "." + CONTROLLER_ENTITY_ALWAYS_ON, null);
        settings.set(entityName + "." + CONTROLLER_ENTITY_IS_RGB, null);
        int oldNumberOfTotaleRenders = getNumberOfTotalRenders();
        generateHomeAssistantEntities();
        propertyChangeSupport.firePropertyChange(Property.NUMBER_OF_RENDERS.name(), oldNumberOfTotaleRenders, getNumberOfTotalRenders());
        propertyChangeSupport.firePropertyChange(Property.ENTITY_ATTRIBUTE_CHANGED.name(), oldAlwaysOn, getEntityAlwaysOn(entityName));
        propertyChangeSupport.firePropertyChange(Property.ENTITY_ATTRIBUTE_CHANGED.name(), oldIsRgb, getEntityIsRgb(entityName));
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
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, 0);
        numberOfCompletedRenders = 0;

        try {
            new File(outputRendersDirectoryName).mkdirs();
            new File(outputFloorplanDirectoryName).mkdirs();
            camera.setTime(renderDateTime);

            String yaml = generateBaseRender();
            generateTransparentImage(outputFloorplanDirectoryName + File.separator + TRANSPARENT_FILE_NAME);
            BufferedImage baseImage = ImageIO.read(Files.newInputStream(Paths.get(outputFloorplanDirectoryName + File.separator + "base.png")));
            
            for (String group : lightsGroups.keySet())
                yaml += generateGroupRenders(group, baseImage);

            yaml += generateEntitiesYaml();

            Files.write(Paths.get(outputDirectoryName + File.separator + "floorplan.yaml"), yaml.getBytes());
        } catch (ClosedByInterruptException e) {
            throw new InterruptedException();
        } catch (IOException e) {
            throw e;
        } finally {
            restoreLightsPower(lightsPower);
        }
    }

    private void addEnabledLightsInList(Map<String, List<HomeLight>> lights, List<HomePieceOfFurniture> furnitureList ) {
        for (HomePieceOfFurniture piece : furnitureList) {
            if (piece instanceof HomeFurnitureGroup) {
                addEnabledLightsInList(lights, ((HomeFurnitureGroup)piece).getFurniture());
                continue;
            }
            if (!(piece instanceof HomeLight))
                continue;
            HomeLight light = (HomeLight)piece;
            if (light.getPower() == 0f || !light.isVisible())
                continue;
            if (!home.getEnvironment().isAllLevelsVisible() && light.getLevel() != home.getSelectedLevel())
                continue;
            if (!lights.containsKey(light.getName()))
                lights.put(light.getName(), new ArrayList<HomeLight>());
            lights.get(light.getName()).add(light);
        }
    }

    private Map<String, List<HomeLight>> getEnabledLights() {
        Map<String, List<HomeLight>> lights = new HashMap<String, List<HomeLight>>();

        addEnabledLightsInList(lights, home.getFurniture());

        return lights;
    }

    private Map<String, Map<String, List<HomeLight>>> getLightsGroupsByRoom(Map<String, List<HomeLight>> lights) {
        Map<String, Map<String, List<HomeLight>>> lightsGroups = new HashMap<String, Map<String, List<HomeLight>>>();
        List<Room> homeRooms = home.getRooms();

        for (Room room : homeRooms) {
            if (!home.getEnvironment().isAllLevelsVisible() && room.getLevel() != home.getSelectedLevel())
                continue;
            String roomName = room.getName() != null ? room.getName() : room.getId();
            for (List<HomeLight> subLights : lights.values()) {
                HomeLight subLight = subLights.get(0);
                if (room.containsPoint(subLight.getX(), subLight.getY(), 0) && room.getLevel() == subLight.getLevel()) {
                    if (!lightsGroups.containsKey(roomName))
                        lightsGroups.put(roomName, new HashMap<String, List<HomeLight>>());
                    lightsGroups.get(roomName).put(subLight.getName(), subLights);
                }
            }
        }

        return lightsGroups;
    }

    private Map<String, Map<String, List<HomeLight>>> getLightsGroupsByLight(Map<String, List<HomeLight>> lights) {
        Map<String, Map<String, List<HomeLight>>> lightsGroups = new HashMap<String, Map<String, List<HomeLight>>>();

        for (String lightName : lights.keySet()) {
            lightsGroups.put(lightName, new HashMap<String, List<HomeLight>>());
            lightsGroups.get(lightName).put(lightName, lights.get(lightName));
        }

        return lightsGroups;
    }

    private Map<String, Map<String, List<HomeLight>>> getLightsGroupsByHome(Map<String, List<HomeLight>> lights) {
        Map<String, Map<String, List<HomeLight>>> lightsGroups = new HashMap<String, Map<String, List<HomeLight>>>();

        lightsGroups.put("Home", new HashMap<String, List<HomeLight>>());
        for (String lightName : lights.keySet())
            lightsGroups.get("Home").put(lightName, lights.get(lightName));

        return lightsGroups;
    }

    private Map<String, Map<String, List<HomeLight>>> getLightsGroups(Map<String, List<HomeLight>> lights)
    {
        if (lightMixingMode == LightMixingMode.CSS)
            return getLightsGroupsByLight(lights);
        if (lightMixingMode == LightMixingMode.OVERLAY)
            return getLightsGroupsByRoom(lights);
        if (lightMixingMode == LightMixingMode.FULL)
            return getLightsGroupsByHome(lights);

        return null;
    }

    private Map<HomeLight, Float> getLightsPower(Map<String, List<HomeLight>> lights) {
        Map<HomeLight, Float> lightsPower = new HashMap<HomeLight, Float>();

        for (List<HomeLight> lightsList : lights.values()) {
            for (HomeLight light : lightsList )
                lightsPower.put(light, light.getPower());
        }

        return lightsPower;
    }

    private boolean isHomeAssistantEntity(String name) {
        String[] sensorPrefixes = {
            "binary_sensor.",
            "button.",
            "camera.",
            "climate.",
            "cover.",
            "fan.",
            "input_boolean.",
            "input_button.",
            "lock.",
            "media_player.",
            "sensor.",
            "switch.",
            "vacuum.",
        };

        if (name == null)
            return false;

        for (String prefix : sensorPrefixes ) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    public List<HomePieceOfFurniture> getHomeAssistantEntities() {
       List<HomePieceOfFurniture> homeAssistantEntities = new ArrayList<HomePieceOfFurniture>();

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (!isHomeAssistantEntity(piece.getName()) || !piece.isVisible() || piece instanceof HomeLight)
                continue;
            if (!home.getEnvironment().isAllLevelsVisible() && piece.getLevel() != home.getSelectedLevel())
                continue;
            homeAssistantEntities.add(piece);
        }

        return homeAssistantEntities;
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

    private String generateBaseRender() throws IOException, InterruptedException {
        generateImage(new ArrayList<String>(), outputFloorplanDirectoryName + File.separator + "base.png");
        return String.format(
            "type: picture-elements\n" +
            "image: /local/floorplan/base.png?version=%s\n" +
            "elements:\n", renderHash("base.png"));
    }

    private String generateGroupRenders(String group, BufferedImage baseImage) throws IOException, InterruptedException {
        List<String> groupLights = new ArrayList<String>(lightsGroups.get(group).keySet());

        List<List<String>> lightCombinations = getCombinations(groupLights);
        String yaml = "";
        for (List<String> onLights : lightCombinations) {
            String fileName = String.join("_", onLights) + ".png";
            BufferedImage image = generateImage(onLights, outputRendersDirectoryName + File.separator + fileName);
            boolean createOverlayImage = lightMixingMode == LightMixingMode.OVERLAY || (lightMixingMode == LightMixingMode.CSS && getEntityIsRgb(onLights.get(0)));
            BufferedImage floorPlanImage = generateFloorPlanImage(baseImage, image, outputFloorplanDirectoryName + File.separator + fileName, createOverlayImage);
            if (getEntityIsRgb(onLights.get(0))) {
                String redTintedFileName = String.join("_", onLights) + ".red.png";
                generateRedTintedImage(floorPlanImage, outputFloorplanDirectoryName + File.separator + redTintedFileName);
                yaml += generateRgbLightYaml(onLights.get(0), fileName, redTintedFileName);
            }
            else
                yaml += generateLightYaml(groupLights, onLights, fileName);
        }
        return yaml;
    }

    private void generateTransparentImage(String fileName) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0);
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
    }

    private BufferedImage generateImage(List<String> onLights, String fileName) throws IOException, InterruptedException {
        if (useExistingRenders && Files.exists(Paths.get(fileName))) {
            propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, ++numberOfCompletedRenders);
            return ImageIO.read(Files.newInputStream(Paths.get(fileName)));
        }
        prepareScene(onLights);
        BufferedImage image = renderScene();
        File imageFile = new File(fileName);
        ImageIO.write(image, "png", imageFile);
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, ++numberOfCompletedRenders);
        return image;
    }

    private void prepareScene(List<String> onLights) {
        for (String lightName : lightsNames) {
            boolean isOn = onLights.contains(lightName) || getEntityAlwaysOn(lightName);
            for (HomeLight light : lights.get(lightName)) {
                light.setPower(isOn ? lightsPower.get(light) : 0f);
            }
        }
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

    private BufferedImage generateFloorPlanImage(BufferedImage baseImage, BufferedImage image, String fileName, boolean createOverlayImage) throws IOException {
        File floorPlanFile = new File(fileName);

        if (!createOverlayImage) {
            ImageIO.write(image, "png", floorPlanFile);
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

    private BufferedImage generateRedTintedImage(BufferedImage image, String fileName) throws IOException {
        File redTintedFile = new File(fileName);
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

    private String renderHash(String fileName) throws IOException {
        byte[] content = Files.readAllBytes(Paths.get(outputFloorplanDirectoryName + File.separator + fileName));
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(content);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Long.toString(System.currentTimeMillis() / 1000L);
        }
    }

    private String generateLightYaml(List<String> lightsNames, List<String> onLights, String fileName) throws IOException {
        String conditions = "";
        for (String lightName : lightsNames) {
            conditions += String.format(
                "      - entity: %s\n" +
                "        state: '%s'\n",
                lightName, onLights.contains(lightName) ? "on" : "off");
        }

        String entities = "";
        for (String lightName : lightsNames) {
            entities += String.format(
                "          - %s\n",
                lightName);
        }

        return String.format(
            "  - type: conditional\n" +
            "    conditions:\n%s" +
            "    elements:\n" +
            "      - type: image\n" +
            "        tap_action:\n" +
            "          action: none\n" +
            "        hold_action:\n" +
            "          action: none\n" +
            "        entity:\n%s" +
            "        image: /local/floorplan/%s?version=%s\n" +
            "        filter: none\n" +
            "        style:\n" +
            "          left: 50%%\n" +
            "          top: 50%%\n" +
            "          width: 100%%\n%s",
            conditions, entities, fileName, renderHash(fileName),
            lightMixingMode == LightMixingMode.CSS ? "          mix-blend-mode: lighten\n" : "");
    }

    private String generateRgbLightYaml(String lightName, String fileName, String redTintedFileName) throws IOException {
        return String.format(
            "  - type: custom:config-template-card\n" +
            "    variables:\n" +
            "      LIGHT_STATE: states['%s'].state\n" +
            "      COLOR_MODE: states['%s'].attributes.color_mode\n" +
            "      LIGHT_COLOR: states['%s'].attributes.hs_color\n" +
            "      BRIGHTNESS: states['%s'].attributes.brightness\n" +
            "    entities:\n" +
            "      - %s\n" +
            "    element:\n" +
            "      type: image\n" +
            "      image: /local/floorplan/%s?version=%s\n" +
            "      state_image:\n" +
            "        'on': >-\n" +
            "          ${COLOR_MODE === 'color_temp' || (COLOR_MODE === 'rgb' && LIGHT_COLOR && LIGHT_COLOR[0] == 0 && LIGHT_COLOR[1] == 0) ?\n" +
            "          '/local/floorplan/%s?version=%s' :\n" +
            "          '/local/floorplan/%s?version=%s' }\n" +
            "      entity: %s\n" +
            "    style:\n" +
            "      filter: '${ \"hue-rotate(\" + (LIGHT_COLOR ? LIGHT_COLOR[0] : 0) + \"deg)\"}'\n" +
            "      opacity: '${LIGHT_STATE === ''on'' ? (BRIGHTNESS / 255) : ''100''}'\n" +
            "      mix-blend-mode: lighten\n" +
            "      pointer-events: none\n" +
            "      left: 50%%\n" +
            "      top: 50%%\n" +
            "      width: 100%%\n",
            lightName, lightName, lightName, lightName, lightName, TRANSPARENT_FILE_NAME, renderHash(TRANSPARENT_FILE_NAME),
            fileName, renderHash(fileName), redTintedFileName, renderHash(redTintedFileName), lightName);
    }

    private void restoreLightsPower(Map<HomeLight, Float> lightsPower) {
        for(HomeLight light : lightsPower.keySet()) {
            light.setPower(lightsPower.get(light));
        }
    }

    private void removeAlwaysOnLights(List<String> inputList) {
        ListIterator<String> iter = inputList.listIterator();

        while (iter.hasNext()) {
            if (getEntityAlwaysOn(iter.next()))
                iter.remove();
        }
    }

    public List<List<String>> getCombinations(List<String> inputSet) {
        List<List<String>> combinations = new ArrayList<List<String>>();
        List<String> inputList = new ArrayList<String>(inputSet);

        removeAlwaysOnLights(inputList);
        _getCombinations(inputList, 0, new ArrayList<String>(), combinations);

        return combinations;
    }

    private void _getCombinations(List<String> inputList, int currentIndex, List<String> currentCombination, List<List<String>> combinations) {
        if (currentCombination.size() > 0)
            combinations.add(new ArrayList<String>(currentCombination));

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

        return new Point2d((objectPosition.x * 0.5 + 0.5) * renderWidth, (objectPosition.y * 0.5 + 0.5) * renderHeight);
    }

    private String generateEntityYaml(Entity entity) {
        final Map<EntityDisplayType, String> entityDisplayTypeToYamlString = new HashMap<EntityDisplayType, String>() {{
            put(EntityDisplayType.BADGE, "state-badge");
            put(EntityDisplayType.ICON, "state-icon");
            put(EntityDisplayType.LABEL, "state-label");
        }};
        final Map<EntityTapAction, String> entityTapActionToYamlString = new HashMap<EntityTapAction, String>() {{
            put(EntityTapAction.MORE_INFO, "more-info");
            put(EntityTapAction.NONE, "none");
            put(EntityTapAction.TOGGLE, "toggle");
        }};

        if (entity.displayType == EntityDisplayType.NONE || entity.alwaysOn)
            return "";

        String yaml = String.format(Locale.US,
            "  - type: %s\n" +
            "    entity: %s\n" +
            "    title: %s\n" +
            "    style:\n" +
            "      top: %.2f%%\n" +
            "      left: %.2f%%\n" +
            "      border-radius: 50%%\n" +
            "      text-align: center\n" +
            "      background-color: rgba(255, 255, 255, 0.3)\n" +
            "    tap_action:\n" +
            "      action: %s\n",
            entityDisplayTypeToYamlString.get(entity.displayType), entity.name, entity.title,
            100.0 * (entity.position.y / renderHeight), 100.0 * (entity.position.x / renderWidth),
            entityTapActionToYamlString.get(entity.tapAction));

        return yaml;
    }

    private String generateEntitiesYaml() {
        String yaml = "";

        for (Entity entity : homeAssistantEntities.values())
            yaml += generateEntityYaml(entity);

        return yaml;
    }

    private void generateHomeAssistantEntities() {
        homeAssistantEntities.clear();

        build3dProjection();
        generateLightEntities(homeAssistantEntities);
        generateSensorEntities(homeAssistantEntities);
        moveEntityIconsToAvoidIntersection();
    }

    private void generateLightEntities(Map<String, Entity> homeAssistantEntities) {
        for (List<HomeLight> lightsList : lights.values()) {
            Point2d lightsCenter = new Point2d();
            for (HomeLight light : lightsList )
                lightsCenter.add(getFurniture2dLocation(light));
            lightsCenter.scale(1.0 / lightsList.size());

            String name = lightsList.get(0).getName();
            homeAssistantEntities.put(name, new Entity(name, lightsCenter, EntityDisplayType.ICON, EntityTapAction.TOGGLE,
                lightsList.get(0).getDescription()));
        }
    }

    private boolean isHomeAssistantEntityActionable(String name) {
        String[] actionableEntityPrefixes = {
            "button.",
            "cover.",
            "fan.",
            "lock.",
            "media_player.",
            "switch.",
        };

        for (String prefix : actionableEntityPrefixes ) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private void generateSensorEntities(Map<String, Entity> homeAssistantEntities) {
        for (HomePieceOfFurniture piece : getHomeAssistantEntities()) {
            Point2d location = getFurniture2dLocation(piece);
            String name = piece.getName();

            homeAssistantEntities.put(name, new Entity(name, location,
                name.startsWith("sensor.") ? EntityDisplayType.LABEL : EntityDisplayType.ICON,
                isHomeAssistantEntityActionable(piece.getName()) ?  EntityTapAction.TOGGLE : EntityTapAction.MORE_INFO,
                piece.getDescription()));
        }
    }

    private boolean doStateIconsIntersect(Entity first, Entity second) {
        final double STATE_ICON_RAIDUS_INCLUDING_MARGIN = 25.0;

        double x = Math.pow(first.position.x - second.position.x, 2) + Math.pow(first.position.y - second.position.y, 2);

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

    private Entity stateIconWithWhichStateIconIntersects(Entity entity) {
        for (Entity other : homeAssistantEntities.values()) {
            if (entity == other)
                continue;
            if (doStateIconsIntersect(entity, other))
                return other;
        }
        return null;
    }

    private List<Set<Entity>> findIntersectingStateIcons() {
        List<Set<Entity>> intersectingStateIcons = new ArrayList<Set<Entity>>();

        for (Entity entity : homeAssistantEntities.values()) {
            Set<Entity> interectingSet = setWithWhichStateIconIntersects(entity, intersectingStateIcons);
            if (interectingSet != null) {
                interectingSet.add(entity);
                continue;
            }
            Entity intersectingStateIcon = stateIconWithWhichStateIconIntersects(entity);
            if (intersectingStateIcon == null)
                continue;
            Set<Entity> intersectingGroup = new HashSet<Entity>();
            intersectingGroup.add(entity);
            intersectingGroup.add(intersectingStateIcon);
            intersectingStateIcons.add(intersectingGroup);
        }

        return intersectingStateIcons;
    }

    private Point2d getCenterOfStateIcons(Set<Entity> entities) {
        Point2d centerPostition = new Point2d();
        for (Entity entity : entities )
            centerPostition.add(entity.position);
        centerPostition.scale(1.0 / entities.size());
        return centerPostition;
    }

    private void separateStateIcons(Set<Entity> entities) {
        final double STEP_SIZE = 2.0;

        Point2d centerPostition = getCenterOfStateIcons(entities);

        for (Entity entity : entities) {
            Vector2d direction = new Vector2d(entity.position.x - centerPostition.x, entity.position.y - centerPostition.y);
            direction.normalize();
            direction.scale(STEP_SIZE);
            entity.position.add(direction);
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
};
