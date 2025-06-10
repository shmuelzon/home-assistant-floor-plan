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
import com.shmuelzon.HomeAssistantFloorPlan.Entity.DisplayOperator;


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

    // --- MODIFIED: This method now correctly uses the new operator/value fields from Entity.java ---
    private String getEntitiesToShowHideCondition() {
        String condition = "";

        for (Entity entity : entitiesToShowHide) {
            DisplayOperator op = entity.getFurnitureDisplayOperator();
            String value = entity.getFurnitureDisplayValue();

            // If the condition is not set, or is numeric (not supported by this render-layer logic), skip it.
            // Also skip if the operator is ALWAYS or NEVER, as these don't require a state condition in the YAML.
            if (op == null || value == null || value.trim().isEmpty() ||
                op == DisplayOperator.GREATER_THAN || op == DisplayOperator.LESS_THAN || op == DisplayOperator.ALWAYS || op == DisplayOperator.NEVER) {
                continue;
            }

            boolean isNotOperator = (op == DisplayOperator.IS_NOT);
            boolean shouldHide = !entitiesToShow.contains(entity);

            // This XOR logic correctly inverts the condition when necessary.
            // (e.g., if we need to HIDE when state IS 'on', the resulting YAML condition must be state_not: 'on')
            boolean useNot = isNotOperator ^ shouldHide; 
            
            String conditionType = useNot ? "_not" : "";
            
            condition += String.format(
                "      - condition: state\n" +
                "        entity: %s\n" +
                "        state%s: '%s'\n",
                entity.getName(),
                conditionType,
                value);
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
