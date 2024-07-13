package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    public enum Property {COMPLETED_RENDERS, LIGHT_MIXING_MODE}
    public enum LightMixingMode {CSS, OVERLAY, FULL}
    public enum Renderer {YAFARAY, SUNFLOW}
    public enum Quality {HIGH, LOW}

    private Home home;
    private Camera camera;
    private Map<String, List<HomeLight>> lights;
    private Map<String, Map<String, List<HomeLight>>> lightsGroups;
    private List<String> lightsNames;
    private Map<HomeLight, Float> lightsPower;
    private List<HomePieceOfFurniture> homeAssistantEntities;
    private Vector4d cameraPosition;
    private Transform3D perspectiveTransform;
    private PropertyChangeSupport propertyChangeSupport;
    private int numberOfCompletedRenders;
    private String outputDirectoryName = System.getProperty("user.home");
    private String outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
    private String outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
    private int sensitivity = 10;
    private LightMixingMode lightMixingMode = LightMixingMode.CSS;
    private int renderWidth = 1024;
    private int renderHeight = 576;
    private boolean useExistingRenders = true;
    private Renderer renderer = Renderer.YAFARAY;
    private Quality quality = Quality.HIGH;

    class StateIcon {
        public String name;
        public Point2d position;
        public String type;
        public String action;
        public String title;

        public StateIcon(String name, Point2d position, String type, String action, String title) {
            this.name = name;
            this.position = position;
            this.type = type;
            this.action = action;
            this.title = title;
        }
    }

    public Controller(Home home) {
        this.home = home;
        camera = home.getCamera().clone();
        propertyChangeSupport = new PropertyChangeSupport(this);
        lights = getEnabledLights();
        lightsGroups = getLightsGroups(lights);
        lightsNames = new ArrayList<String>(lights.keySet());
        lightsPower = getLightsPower(lights);
        homeAssistantEntities = getHomeAssistantEntities();
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

    public int getNumberOfTotalRenders() {
        int numberOfTotalRenders = 1;

        for (Map<String, List<HomeLight>> groupLights : lightsGroups.values()) {
            numberOfTotalRenders += (1 << groupLights.keySet().size()) - 1;
        }
        return numberOfTotalRenders;
    }

    public int getRenderHeight() {
        return renderHeight;
    }

    public void setRenderHeight(int renderHeight) {
        this.renderHeight = renderHeight;
    }

    public int getRenderWidth() {
        return renderWidth;
    }

    public void setRenderWidth(int renderWidth) {
        this.renderWidth = renderWidth;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    public LightMixingMode getLightMixingMode() {
        return lightMixingMode;
    }

    public void setLightMixingMode(LightMixingMode lightMixingMode) {
        LightMixingMode oldValue = this.lightMixingMode;
        this.lightMixingMode = lightMixingMode;
        lightsGroups = getLightsGroups(lights);
        propertyChangeSupport.firePropertyChange(Property.LIGHT_MIXING_MODE.name(), oldValue, lightMixingMode);
    }

    public String getOutputDirectory() {
        return outputDirectoryName;
    }

    public void setOutputDirectory(String outputDirectoryName) {
        this.outputDirectoryName = outputDirectoryName;
        outputRendersDirectoryName = outputDirectoryName + File.separator + "renders";
        outputFloorplanDirectoryName = outputDirectoryName + File.separator + "floorplan";
    }

    public boolean getUserExistingRenders() {
        return useExistingRenders;
    }

    public void setUserExistingRenders(boolean useExistingRenders) {
        this.useExistingRenders = useExistingRenders;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public boolean isProjectEmpty() {
        return home == null || home.getFurniture().isEmpty();
    }

    public void render() throws Exception {
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, 0);
        numberOfCompletedRenders = 0;

        try {
            new File(outputRendersDirectoryName).mkdirs();
            new File(outputFloorplanDirectoryName).mkdirs();

            build3dProjection();
            String yaml = generateBaseRender();
            BufferedImage baseImage = ImageIO.read(Files.newInputStream(Paths.get(outputFloorplanDirectoryName + File.separator + "base.png")));
            
            for (String group : lightsGroups.keySet())
                yaml += generateGroupRenders(group, baseImage);

            List<StateIcon> stateIcons = generateLightsStateIcons();
            stateIcons.addAll(generateSensorsStateIcons());
            moveStateIconsToAvoidIntersection(stateIcons);
            yaml += generateStateIconsYaml(stateIcons);

            Files.write(Paths.get(outputDirectoryName + File.separator + "floorplan.yaml"), yaml.getBytes());
        } catch (IOException e) {
            throw e;
        } finally {
            restorLightsPower(lightsPower);
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
            String roomName = room.getName() != null ? room.getName() : room.getId();
            for (List<HomeLight> subLights : lights.values()) {
                HomeLight subLight = subLights.get(0);
                if (room.containsPoint(subLight.getX(), subLight.getY(), 0)) {
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
            "camera.",
            "climate.",
            "cover.",
            "fan.",
            "media_player.",
            "sensor.",
            "switch.",
        };

        if (name == null)
            return false;

        for (String prefix : sensorPrefixes ) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private List<HomePieceOfFurniture> getHomeAssistantEntities() {
       List<HomePieceOfFurniture> homeAssistantEntities = new ArrayList<HomePieceOfFurniture>();

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (!isHomeAssistantEntity(piece.getName()) || !piece.isVisible() || piece instanceof HomeLight)
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

    private String generateBaseRender() throws IOException {
        generateImage(new ArrayList<String>(), outputFloorplanDirectoryName + File.separator + "base.png");
        return String.format(
            "type: picture-elements\n" +
            "image: /local/floorplan/base.png?version=%s\n" +
            "elements:\n", renderHash("base.png"));
    }

    private String generateGroupRenders(String group, BufferedImage baseImage) throws IOException {
        List<String> groupLights = new ArrayList<String>(lightsGroups.get(group).keySet());

        List<List<String>> lightCombinations = getCombinations(groupLights);
        String yaml = "";
        for (List<String> onLights : lightCombinations) {
            String fileName = String.join("_", onLights) + ".png";
            BufferedImage image = generateImage(onLights, outputRendersDirectoryName + File.separator + fileName);
            generateFloorPlanImage(baseImage, image, outputFloorplanDirectoryName + File.separator + fileName);
            yaml += generateLightYaml(groupLights, onLights, fileName);
        }
        return yaml;
    }

    private BufferedImage generateImage(List<String> onLights, String fileName) throws IOException {
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
            boolean isOn = onLights.contains(lightName);
            for (HomeLight light : lights.get(lightName)) {
                light.setPower(isOn ? lightsPower.get(light) : 0f);
            }
        }
    }

    private BufferedImage renderScene() throws IOException {
        Map<Renderer, String> rendererToClassName = new HashMap<Renderer, String>() {{
            put(Renderer.SUNFLOW, "com.eteks.sweethome3d.j3d.PhotoRenderer");
            put(Renderer.YAFARAY, "com.eteks.sweethome3d.j3d.YafarayRenderer");
        }};
        AbstractPhotoRenderer photoRenderer = AbstractPhotoRenderer.createInstance(
            rendererToClassName.get(renderer),
            home, null, this.quality == Quality.LOW ? AbstractPhotoRenderer.Quality.LOW : AbstractPhotoRenderer.Quality.HIGH);
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        photoRenderer.render(image, camera, null);

        return image;
    }

    private void generateFloorPlanImage(BufferedImage baseImage, BufferedImage image, String fileName) throws IOException {
        File floorPlanFile = new File(fileName);

        if (lightMixingMode != LightMixingMode.OVERLAY) {
            ImageIO.write(image, "png", floorPlanFile);
            return;
        }

        BufferedImage overlay = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for(int x = 0; x < baseImage.getWidth(); x++) {
            for(int y = 0; y < baseImage.getHeight(); y++) {
                int diff = pixelDifference(baseImage.getRGB(x, y), image.getRGB(x, y));
                overlay.setRGB(x, y, diff > sensitivity ? image.getRGB(x, y) : 0);
            }
        }
        ImageIO.write(overlay, "png", floorPlanFile);
    }

    private int pixelDifference(int first, int second) {
        int diff =
            Math.abs((first & 0xff) - (second & 0xff)) +
            Math.abs(((first >> 8) & 0xff) - ((second >> 8) & 0xff)) +
            Math.abs(((first >> 16) & 0xff) - ((second >> 16) & 0xff));
        return diff / 3;
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

    private void restorLightsPower(Map<HomeLight, Float> lightsPower) {
        for(HomeLight light : lightsPower.keySet()) {
            light.setPower(lightsPower.get(light));
        }
    }

    public List<List<String>> getCombinations(List<String> inputSet) {
        List<List<String>> combinations = new ArrayList<List<String>>();

        _getCombinations(new ArrayList<String>(inputSet), 0, new ArrayList<String>(), combinations);

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
        Vector4d objectPosition = new Vector4d(piece.getX(), ((piece.getElevation() * 2) + piece.getHeight()) / 2, piece.getY(), 0);

        objectPosition.sub(cameraPosition);
        perspectiveTransform.transform(objectPosition);
        objectPosition.scale(1 / objectPosition.w);

        return new Point2d((objectPosition.x * 0.5 + 0.5) * renderWidth, (objectPosition.y * 0.5 + 0.5) * renderHeight);
    }

    private String generateStateIconYaml(String name, Point2d position, String type, String action, String title) {
        String yaml = String.format(Locale.US,
            "  - type: state-%s\n" +
            "    entity: %s\n" +
            "    title: %s\n" +
            "    style:\n" +
            "      top: %.2f%%\n" +
            "      left: %.2f%%\n" +
            "      border-radius: 50%%\n" +
            "      text-align: center\n" +
            "      background-color: rgba(255, 255, 255, 0.3)\n",
            type, name, title, 100.0 * (position.y / renderHeight), 100.0 * (position.x / renderWidth));

        if (action != null) {
            yaml += String.format(
                "    tap_action:\n" +
                "      action: %s\n", action);
        }

        return yaml;
    }

    private String generateStateIconsYaml(List<StateIcon> stateIcons) {
        String yaml = "";

        for (StateIcon stateIcon : stateIcons)
            yaml += generateStateIconYaml(stateIcon.name, stateIcon.position, stateIcon.type, stateIcon.action, stateIcon.title);

        return yaml;
    }

    private List<StateIcon> generateLightsStateIcons() {
        List<StateIcon> stateIcons = new ArrayList<StateIcon>();

        for (List<HomeLight> lightsList : lights.values()) {
            Point2d lightsCenter = new Point2d();
            for (HomeLight light : lightsList )
                lightsCenter.add(getFurniture2dLocation(light));
            lightsCenter.scale(1.0 / lightsList.size());

            stateIcons.add(new StateIcon(lightsList.get(0).getName(), lightsCenter, "icon", "toggle",
                lightsList.get(0).getDescription()));
        }

        return stateIcons;
    }

    private boolean isHomeAssistantEntityActionable(String name) {
        String[] actionableEntityPrefixes = {
            "cover.",
            "fan.",
            "media_player.",
            "switch.",
        };

        for (String prefix : actionableEntityPrefixes ) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private List<StateIcon> generateSensorsStateIcons() {
        List<StateIcon> stateIcons = new ArrayList<StateIcon>();

        for (HomePieceOfFurniture piece : homeAssistantEntities) {
            Point2d location = getFurniture2dLocation(piece);
            stateIcons.add(new StateIcon(piece.getName(), location, piece.getName().startsWith("sensor.") ? "label" : "icon",
                isHomeAssistantEntityActionable(piece.getName()) ? "toggle" : null, piece.getDescription()));
        }

        return stateIcons;
    }

    private boolean doStateIconsIntersect(StateIcon first, StateIcon second) {
        final double STATE_ICON_RAIDUS_INCLUDING_MARGIN = 25.0;

        double x = Math.pow(first.position.x - second.position.x, 2) + Math.pow(first.position.y - second.position.y, 2);

        return x <= Math.pow(STATE_ICON_RAIDUS_INCLUDING_MARGIN * 2, 2);
    }

    private boolean doesStateIconIntersectWithSet(StateIcon stateIcon, Set<StateIcon> stateIcons) {
        for (StateIcon other : stateIcons) {
            if (doStateIconsIntersect(stateIcon, other))
                return true;
        }
        return false;
    }

    private Set<StateIcon> setWithWhichStateIconIntersects(StateIcon stateIcon, List<Set<StateIcon>> stateIcons) {
        for (Set<StateIcon> set : stateIcons) {
            if (doesStateIconIntersectWithSet(stateIcon, set))
                return set;
        }
        return null;
    }

    private StateIcon stateIconWithWhichStateIconIntersects(StateIcon stateIcon, List<StateIcon> stateIcons) {
        for (StateIcon other : stateIcons) {
            if (stateIcon == other)
                continue;
            if (doStateIconsIntersect(stateIcon, other))
                return other;
        }
        return null;
    }

    private List<Set<StateIcon>> findIntersectingStateIcons(List<StateIcon> stateIcons) {
        List<Set<StateIcon>> intersectingStateIcons = new ArrayList<Set<StateIcon>>();

        for (StateIcon stateIcon : stateIcons) {
            Set<StateIcon> interectingSet = setWithWhichStateIconIntersects(stateIcon, intersectingStateIcons);
            if (interectingSet != null) {
                interectingSet.add(stateIcon);
                continue;
            }
            StateIcon intersectingStateIcon = stateIconWithWhichStateIconIntersects(stateIcon, stateIcons);
            if (intersectingStateIcon == null)
                continue;
            Set<StateIcon> intersectingGroup = new HashSet<StateIcon>();
            intersectingGroup.add(stateIcon);
            intersectingGroup.add(intersectingStateIcon);
            intersectingStateIcons.add(intersectingGroup);
        }

        return intersectingStateIcons;
    }

    private Point2d getCenterOfStateIcons(Set<StateIcon> stateIcons) {
        Point2d centerPostition = new Point2d();
        for (StateIcon stateIcon : stateIcons )
            centerPostition.add(stateIcon.position);
        centerPostition.scale(1.0 / stateIcons.size());
        return centerPostition;
    }
    private void separateStateIcons(Set<StateIcon> stateIcons) {
        final double STEP_SIZE = 2.0;

        Point2d centerPostition = getCenterOfStateIcons(stateIcons);

        for (StateIcon stateIcon : stateIcons) {
            Vector2d direction = new Vector2d(stateIcon.position.x - centerPostition.x, stateIcon.position.y - centerPostition.y);
            direction.normalize();
            direction.scale(STEP_SIZE);
            stateIcon.position.add(direction);
        }

    }

    private void moveStateIconsToAvoidIntersection(List<StateIcon> stateIcons) {
        for (int i = 0; i < 100; i++) {
            List<Set<StateIcon>> intersectingStateIcons = findIntersectingStateIcons(stateIcons);
            if (intersectingStateIcons.size() == 0)
                break;
            for (Set<StateIcon> set : intersectingStateIcons)
                separateStateIcons(set);
        }
    }
};
