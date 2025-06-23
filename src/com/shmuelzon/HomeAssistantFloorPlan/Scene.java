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

        final int indexInTimes = renderingTimes.indexOf(renderingTime);
        final int numberOfTimes = renderingTimes.size();
        
        // Determine the start and end times for this scene's active period.
        final long startTime = renderingTimes.get(indexInTimes);
        // The end time is the next time in the list, or the first time if this is the last one.
        final long endTime = (indexInTimes == numberOfTimes - 1) 
                           ? renderingTimes.get(0) 
                           : renderingTimes.get(indexInTimes + 1);

        // Convert timestamps to numeric HHmm format for the condition.
        // The 'above' value is exclusive, so we subtract a minute from the start time to make it inclusive.
        final int startTimeNumeric = Integer.valueOf(timestampTo24HourStringUtc(startTime - oneMinuteInMs));
        final int endTimeNumeric = Integer.valueOf(timestampTo24HourStringUtc(endTime));

        // If the start time is less than the end time, it's a simple range within a single day.
        if (startTimeNumeric < endTimeNumeric) {
            return String.format(
                "      - condition: numeric_state\n" +
                "        entity: sensor.time_as_number_utc\n" +
                "        above: %d\n" +
                "        below: %d\n",
                startTimeNumeric,
                endTimeNumeric);
        } else { // Otherwise, the range wraps around midnight.
            return String.format(
                "      - condition: or\n" +
                "        conditions:\n" +
                "          - condition: numeric_state\n" +
                "            entity: sensor.time_as_number_utc\n" +
                "            above: %d\n" +
                "          - condition: numeric_state\n" +
                "            entity: sensor.time_as_number_utc\n" +
                "            below: %d\n",
                startTimeNumeric,
                endTimeNumeric);
        }
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

    /**
     * Formats a timestamp into "HHmm" string using the UTC time zone.
     * This is used for Home Assistant conditions that rely on UTC time sensors.
     */
    private String timestampTo24HourStringUtc(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    /**
     * Formats a timestamp into "HHmm" string using the system's default local time zone.
     * This is used for naming output folders to reflect local time.
     */
    private String timestampTo24HourStringLocal(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
        dateFormat.setTimeZone(TimeZone.getDefault()); // Use local time zone
        return dateFormat.format(date);
    }

    private String buildName() {
        List<String> nameParts = new ArrayList<>();

        if (renderingTimes.size() > 1)
            nameParts.add(timestampTo24HourStringLocal(renderingTime)); // Use local time for folder names
        for (Entity entity : entitiesToShowHide)
            nameParts.add(entity.getName() + "-" + (entitiesToShow.contains(entity) ? "visible" : "hidden"));

        return String.join("_", nameParts);
    }
};
