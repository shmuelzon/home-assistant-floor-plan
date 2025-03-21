package com.shmuelzon.HomeAssistantFloorPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.eteks.sweethome3d.model.Camera;


public class Scene {
    private String name;
    private Camera camera;
    private List<Long> renderingTimes;
    private long renderingTime;
    private List<Entity> entitiesToShowHide;
    private List<Entity> entitiesToShow;

    public Scene(Camera camera, List<Long> renderingTimes, long renderingTime, List<Entity> entitiesToShowHide, List<Entity> entitiesToShow) {
        this.camera = camera;
        this.renderingTimes = renderingTimes;
        this.renderingTime = renderingTime;
        this.entitiesToShowHide = entitiesToShowHide;
        this.entitiesToShow = entitiesToShow;

        name = buildName();
    }

    public String getName() {
        return name;
    }

    public void prepare() {
        camera.setTime(renderingTime);
        for (Entity entity : entitiesToShowHide)
            entity.setVisible(entitiesToShow.contains(entity));
    }

    public String getConditions() {
        String conditions = "";

        conditions += getRenderTimeCondition();
        conditions += getEntitiesToShowHideCondition();

        return conditions;
    }

    private String getRenderTimeCondition() {
        final long oneMinuteInMs = 60000;
        if (renderingTimes.size() <= 1)
            return "";

        int indexInTimes = renderingTimes.indexOf(renderingTime);
        int numberOfTimes = renderingTimes.size();
        boolean isLast = indexInTimes == numberOfTimes - 1;

        if (!isLast) {
            return String.format(
                "      - condition: numeric_state\n" +
                "        entity: sensor.time_as_number_utc\n" +
                "        above: %d\n" +
                "        below: %d\n",
                Integer.valueOf(timestampTo24HourString(renderingTimes.get(indexInTimes) - oneMinuteInMs)),
                Integer.valueOf(timestampTo24HourString(renderingTimes.get(indexInTimes + 1))));
        }

        return String.format(
            "      - condition: or\n" +
            "        conditions:\n" +
            "          - condition: numeric_state\n" +
            "            entity: sensor.time_as_number_utc\n" +
            "            above: %d\n" +
            "          - condition: numeric_state\n" +
            "            entity: sensor.time_as_number_utc\n" +
            "            below: %d\n",
            Integer.valueOf(timestampTo24HourString(renderingTimes.get(indexInTimes) - oneMinuteInMs)),
            Integer.valueOf(timestampTo24HourString(renderingTimes.get(0))));
    }

    private String getEntitiesToShowHideCondition() {
        final Map<List<Boolean>, String> displayConditionYaml = new HashMap<List<Boolean>, String>() {{
            put(Arrays.asList(true, true), "");
            put(Arrays.asList(true, false), "_not");
            put(Arrays.asList(false, true), "_not");
            put(Arrays.asList(false, false), "");
        }};
        String condition = "";

        for (Entity entity : entitiesToShowHide) {
            condition += String.format(
                "      - condition: state\n" +
                "        entity: %s\n" +
                "        state%s: '%s'\n",
                entity.getName(),
                displayConditionYaml.get(Arrays.asList(entitiesToShow.contains(entity), entity.getDisplayFurnitureCondition() == Entity.DisplayFurnitureCondition.STATE_EQUALS)),
                entity.getDisplayFurnitureConditionValue());
        }

        return condition;
    }

    private String timestampTo24HourString(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

    private String buildName() {
        List<String> nameParts = new ArrayList<>();

        if (renderingTimes.size() > 1)
            nameParts.add(timestampTo24HourString(renderingTime));
        for (Entity entity : entitiesToShowHide)
            nameParts.add(entity.getName() + "-" + (entitiesToShow.contains(entity) ? "visible" : "hidden"));

        return String.join("_", nameParts);
    }
};
