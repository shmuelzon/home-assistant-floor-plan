package com.shmuelzon.HomeAssistantFloorPlan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.eteks.sweethome3d.model.Camera;


public class Scenes implements Iterable<Scene> {
    private Camera camera;
    private List<Long> renderingTimes = new ArrayList<>();
    private List<Scene> scenes = new ArrayList<>();

    public Scenes(Camera camera) {
        this.camera = camera;
    }

    public int size() {
        return scenes.size();
    }

    @Override
    public Iterator<Scene> iterator() {
        return scenes.iterator();
    }

    public void setRenderingTimes(List<Long> renderingTimes) {
        this.renderingTimes = renderingTimes;
        buildScenes();
    }

    private void buildScenes() {
        scenes.clear();

        for (long renderingTime : renderingTimes) {
            scenes.add(new Scene(camera, renderingTimes, renderingTime));
        }
    }
};
