package com.shmuelzon.HomeAssistantFloorPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.eteks.sweethome3d.model.Camera;


public class Scene {
    private String name;
    private Camera camera;
    private List<Long> renderingTimes;
    private long renderingTime;

    public Scene(Camera camera, List<Long> renderingTimes, long renderingTime) {
        this.camera = camera;
        this.renderingTimes = renderingTimes;
        this.renderingTime = renderingTime;

        name = buildName();
    }

    public String getName() {
        return name;
    }

    public void prepare() {
        camera.setTime(renderingTime);
    }

    public String getConditions() {
        String conditions = "";

        conditions += getRenderTimeCondition();

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

    private String timestampTo24HourString(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HHmm");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

    private String buildName() {
        List<String> nameParts = new ArrayList<>();

        if (renderingTimes.size() > 1) {
            nameParts.add(timestampTo24HourString(renderingTime));
        }

        return String.join("_", nameParts);
    }
};
