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
    private String title;
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

        generateNameAndTitle();
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
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
        if (renderingTimes.size() <= 1)
            return "";

        boolean isDayTimeRender = renderingTimes.indexOf(renderingTime) == 0;

        return String.format(
            "      - condition: state\n" +
            "        entity: sun.sun\n" +
            "        state: %s\n",
            isDayTimeRender ? "above_horizon" : "below_horizon");
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

    private void generateNameAndTitle() {
        List<String> nameParts = new ArrayList<>();
        List<String> titleParts = new ArrayList<>();

        if (renderingTimes.size() > 1) {
            nameParts.add(timestampTo24HourString(renderingTime));
            titleParts.add(renderingTimes.indexOf(renderingTime) == 0 ?  "Day-time" : "Night-time");
        }
        for (Entity entity : entitiesToShowHide) {
            nameParts.add(entity.getName() + "-" + (entitiesToShow.contains(entity) ? "visible" : "hidden"));
            titleParts.add((entitiesToShow.contains(entity) ? "With" : "Without") + " " + entity.getName());
        }

        name = String.join("_", nameParts);
        title = String.join(",", titleParts);
    }
};
