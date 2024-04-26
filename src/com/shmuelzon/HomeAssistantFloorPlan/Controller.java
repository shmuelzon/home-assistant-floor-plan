package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Vector4d;

import com.eteks.sweethome3d.j3d.AbstractPhotoRenderer;
import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Room;


public class Controller {
    public enum Property {COMPLETED_RENDERS}

    private Home home;
    private Map<String, List<HomeLight>> lights;
    private Map<String, Map<String, List<HomeLight>>> roomsWithLights;
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
    private String renderVersion = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss").format(LocalDateTime.now());
    private int sensitivity = 10;
    private int renderWidth = 1024;
    private int renderHeight = 576;
    private boolean useExistingRenders = true;

    public Controller(Home home) {
        this.home = home;
        propertyChangeSupport = new PropertyChangeSupport(this);
        lights = getEnabledLights();
        roomsWithLights = getRooms(lights);
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

    public Map<String, Map<String, List<HomeLight>>> getRoomsWithLights() {
        return roomsWithLights;
    }

    public int getNumberOfTotalRenders() {
        int numberOfTotalRenders = 1;

        for (Map<String, List<HomeLight>> roomLights : roomsWithLights.values()) {
            numberOfTotalRenders += (1 << roomLights.keySet().size()) - 1;
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

    public String getRenderVersion() {
        return renderVersion;
    }

    public void setRenderVersion(String renderVersion) {
        this.renderVersion = renderVersion;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
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

    public void render() throws Exception {
        propertyChangeSupport.firePropertyChange(Property.COMPLETED_RENDERS.name(), numberOfCompletedRenders, 0);
        numberOfCompletedRenders = 0;

        try {
            new File(outputRendersDirectoryName).mkdirs();
            new File(outputFloorplanDirectoryName).mkdirs();

            build3dProjection();
            String yaml = generateBaseRender();
            BufferedImage baseImage = ImageIO.read(Files.newInputStream(Paths.get(outputFloorplanDirectoryName + File.separator + "base.png")));
            
            for (String room : roomsWithLights.keySet())
                yaml += generateRoomRenders(room, baseImage);
            yaml += generateLightsToggleButtons();
            yaml += generateSensorsIndications();

            Files.write(Paths.get(outputDirectoryName + File.separator + "floorplan.yaml"), yaml.getBytes());
        } catch (IOException e) {
            throw e;
        } finally {
            restorLightsPower(lightsPower);
        }
    }

    private Map<String, List<HomeLight>> getEnabledLights() {
        Map<String, List<HomeLight>> lights = new HashMap<String, List<HomeLight>>();

        for (HomePieceOfFurniture piece : home.getFurniture()) {
            if (!(piece instanceof HomeLight))
                continue;
            HomeLight light = (HomeLight)piece;
            if (light.getPower() == 0f || !light.isVisible())
                continue;
            if (!lights.containsKey(light.getName()))
                lights.put(light.getName(), new ArrayList<HomeLight>());
            lights.get(light.getName()).add(light);
        }

        return lights;
    }

    private Map<String, Map<String, List<HomeLight>>> getRooms(Map<String, List<HomeLight>> lights)
    {
        Map<String, Map<String, List<HomeLight>>> rooms = new HashMap<String, Map<String, List<HomeLight>>>();
        List<Room> homeRooms = home.getRooms();

        for (Room room : homeRooms) {
            String roomName = room.getName() != null ? room.getName() : room.getId();
            for (List<HomeLight> subLights : lights.values()) {
                HomeLight subLight = subLights.get(0); 
                if (room.containsPoint(subLight.getX(), subLight.getY(), 0)) {
                    if (!rooms.containsKey(roomName))
                        rooms.put(roomName, new HashMap<String, List<HomeLight>>());
                    rooms.get(roomName).put(subLight.getName(), subLights);
                }
            }
        }

        return rooms;
    }

    private Map<HomeLight, Float> getLightsPower(Map<String, List<HomeLight>> lights)
    {
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
            if (!isHomeAssistantEntity(piece.getName()))
                continue;
            homeAssistantEntities.add(piece);
        }

        return homeAssistantEntities;
    }

    private void build3dProjection() {
        Camera camera = home.getCamera();
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
            "elements:\n", renderVersion);
    }

    private String generateRoomRenders(String room, BufferedImage baseImage) throws IOException {
        List<String> roomLights = new ArrayList<String>(roomsWithLights.get(room).keySet());

        List<List<String>> lightCombinations = getCombinations(roomLights);
        String yaml = "";
        for (List<String> onLights : lightCombinations) {
            String fileName = String.join("_", onLights) + ".png";
            BufferedImage image = generateImage(onLights, outputRendersDirectoryName + File.separator + fileName);
            generateOverlay(baseImage, image, outputFloorplanDirectoryName + File.separator + fileName);
            yaml += generateLightYaml(roomLights, onLights, fileName);
        }
        return yaml;
    }

    private BufferedImage generateImage(List<String> onLights, String fileName) throws IOException {
        if (useExistingRenders && Files.exists(Paths.get(fileName)))
            return ImageIO.read(Files.newInputStream(Paths.get(fileName)));
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
        AbstractPhotoRenderer photoRenderer = AbstractPhotoRenderer.createInstance(
            "com.eteks.sweethome3d.j3d.YafarayRenderer",
            home, null, AbstractPhotoRenderer.Quality.HIGH);
        BufferedImage image = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_RGB);
        photoRenderer.render(image, home.getCamera(), null);

        return image;
    }

   private void generateOverlay(BufferedImage baseImage, BufferedImage image, String fileName) throws IOException {
        BufferedImage overlay = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for(int x = 0; x < baseImage.getWidth(); x++) {
            for(int y = 0; y < baseImage.getHeight(); y++) {
                int diff = pixelDifference(baseImage.getRGB(x, y), image.getRGB(x, y));
                overlay.setRGB(x, y, diff > sensitivity ? image.getRGB(x, y) : 0);
            }
        }
        File overlayFile = new File(fileName);
        ImageIO.write(overlay, "png", overlayFile);
    }

    private int pixelDifference(int first, int second) {
        int diff =
            Math.abs((first & 0xff) - (second & 0xff)) +
            Math.abs(((first >> 8) & 0xff) - ((second >> 8) & 0xff)) +
            Math.abs(((first >> 16) & 0xff) - ((second >> 16) & 0xff));
        return diff / 3;
    }

    private String generateLightYaml(List<String> lightsNames, List<String> onLights, String fileName) {
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
            "          width: 100%%\n",
            conditions, entities, fileName, renderVersion);
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
        Vector4d objectPosition = new Vector4d(piece.getX(), piece.getElevation(), piece.getY(), 0);

        objectPosition.sub(cameraPosition);
        perspectiveTransform.transform(objectPosition);
        objectPosition.scale(1 / objectPosition.w);

        return new Point2d(objectPosition.x * 0.5 + 0.5, objectPosition.y * 0.5 + 0.5);
    }

    private String generateStateYaml(String name, Point2d position, String stateType, String action) {
        String yaml = String.format(Locale.US,
            "  - type: state-%s\n" +
            "    entity: %s\n" +
            "    title: null\n" +
            "    style:\n" +
            "      top: %.2f%%\n" +
            "      left: %.2f%%\n" +
            "      border-radius: 50%%\n" +
            "      text-align: center\n" +
            "      background-color: rgba(255, 255, 255, 0.3)\n",
            stateType, name, 100.0 * position.y, 100.0 * position.x);

        if (action != null) {
            yaml += String.format(
                "    tap_action:\n" +
                "      action: %s\n", action);
        }

        return yaml;
    }

    private String generateStateYaml(String name, Point2d position, String stateType) {
        return generateStateYaml(name, position, stateType, null);
    }

    private String generateLightsToggleButtons() {
        String toggleButtonsYaml = "";

        for (List<HomeLight> lightsList : lights.values()) {
            Point2d lightsCenter = new Point2d();
            for (HomeLight light : lightsList )
                lightsCenter.add(getFurniture2dLocation(light));
            lightsCenter.scale(1.0 / lightsList.size());

            toggleButtonsYaml += generateStateYaml(lightsList.get(0).getName(), lightsCenter, "icon", "toggle");
        }

        return toggleButtonsYaml;
    }

    private String generateSensorsIndications() {
        String sensorsIndicationsYaml = "";

        for (HomePieceOfFurniture piece : homeAssistantEntities) {
            Point2d location = getFurniture2dLocation(piece);
            sensorsIndicationsYaml += generateStateYaml(piece.getName(), location, piece.getName().startsWith("sensor.") ? "label" : "icon");
        }

        return sensorsIndicationsYaml;
    }
};
