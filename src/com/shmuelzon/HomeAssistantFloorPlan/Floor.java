package com.shmuelzon.HomeAssistantFloorPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.eteks.sweethome3d.model.Camera;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Level;
import com.eteks.sweethome3d.model.Room;


public class Floor {
    private Home home;
    private Level level;
    private Camera camera;
    private boolean includeAllLevels;
    private String name;
    private Settings settings;
    private List<Entity> lightEntities = new ArrayList<>();
    private List<Entity> otherEntities = new ArrayList<>();
    private List<Entity> otherLevelsEntities = new ArrayList<>();
    private Map<String, List<Entity>> lightsGroups = new HashMap<>();
    private Scenes scenes;

    /* The name is used as the floor's directory name, an empty name means no additional directory level */
    public Floor(Home home, Level level, Camera camera, boolean includeAllLevels, String name) {
        this(home, level, camera, includeAllLevels, name, true);
    }

    private Floor(Home home, Level level, Camera camera, boolean includeAllLevels, String name, boolean createEntities) {
        this.home = home;
        this.level = level;
        this.camera = camera;
        this.includeAllLevels = includeAllLevels;
        this.name = name;
        settings = new Settings(home, level);
        if (createEntities)
            createHomeAssistantEntities();
    }

    public Level getLevel() {
        return level;
    }

    public Camera getCamera() {
        return camera;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return level != null && level.getName() != null ? level.getName() : name;
    }

    public List<Entity> getLightEntities() {
        return lightEntities;
    }

    public List<Entity> getOtherEntities() {
        return otherEntities;
    }

    public List<Entity> getEntities() {
        return Stream.concat(lightEntities.stream(), otherEntities.stream()).collect(Collectors.toList());
    }

    public Map<String, List<Entity>> getLightsGroups() {
        return lightsGroups;
    }

    public Scenes getScenes() {
        return scenes;
    }

    private int getNumberOfControllableLights(List<Entity> lights) {
        int numberOfControllableLights = 0;

        for (Entity light : lights)
            numberOfControllableLights += light.getAlwaysOn() ? 0 : 1;

        return numberOfControllableLights;
    }

    public int getNumberOfTotalRenders() {
        int numberOfLightRenders = 1;

        if (scenes == null)
            return 0;

        for (List<Entity> groupLights : lightsGroups.values()) {
            numberOfLightRenders += (1 << getNumberOfControllableLights(groupLights)) - 1;
        }
        return numberOfLightRenders * scenes.size();
    }

    /* Levels without any Home Assistant entities, e.g., a roof, aren't considered as floors */
    public static Set<Level> getLevelsWithEntities(Home home) {
        Map<String, List<HomePieceOfFurniture>> furnitureByName = new HashMap<>();
        new Floor(home, null, null, true, "", false).addEligibleFurnitureToMap(furnitureByName, new ArrayList<>(), home.getFurniture());

        Set<Level> levels = new HashSet<>();
        furnitureByName.values().forEach(pieces -> pieces.forEach(piece -> levels.add(piece.getLevel())));
        return levels;
    }

    private boolean isOnFloor(Level pieceLevel) {
        return includeAllLevels || pieceLevel == level;
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
            if (!isOnFloor(piece.getLevel())) {
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
            if (!isOnFloor(room.getLevel()))
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

    public void buildLightsGroups(Controller.LightMixingMode lightMixingMode) {
        lightsGroups.clear();

        if (lightMixingMode == Controller.LightMixingMode.CSS)
            buildLightsGroupsByLight();
        else if (lightMixingMode == Controller.LightMixingMode.OVERLAY)
            buildLightsGroupsByRoom();
        else if (lightMixingMode == Controller.LightMixingMode.FULL)
            buildLightsGroupsByHome();
    }

    public void buildScenes(List<Long> renderDateTimes) {
        scenes = new Scenes(camera);
        scenes.setRenderingTimes(renderDateTimes);
        scenes.setEntitiesToShowOrHide(otherEntities.stream().filter(entity -> { return entity.getDisplayFurnitureCondition() != Entity.DisplayFurnitureCondition.ALWAYS; }).collect(Collectors.toList()));
        scenes.setEntitiesToOpenOrClose(otherEntities.stream().filter(entity -> { return entity.getOpenFurnitureCondition() != Entity.OpenFurnitureCondition.ALWAYS; }).collect(Collectors.toList()));
    }

    public void setLightsPower(List<Entity> onLights) {
        for (Entity light : lightEntities)
            light.setLightPower(onLights.contains(light) || light.getAlwaysOn());
    }

    public void turnOffLightsFromOtherLevels() {
        otherLevelsEntities.forEach(entity -> entity.setLightPower(false));
    }

    public void restoreEntityConfiguration() {
        Stream.of(lightEntities, otherEntities, otherLevelsEntities).flatMap(Collection::stream)
            .forEach(Entity::restoreConfiguration);
    }

    private static boolean isHomeAssistantEntity(String name) {
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
};
