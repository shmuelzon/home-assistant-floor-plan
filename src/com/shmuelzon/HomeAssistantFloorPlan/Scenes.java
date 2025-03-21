package com.shmuelzon.HomeAssistantFloorPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.eteks.sweethome3d.model.Camera;


public class Scenes implements Iterable<Scene> {
    private Camera camera;
    private List<Long> renderingTimes = new ArrayList<>();
    private List<Entity> entitiesToShowOrHide = new ArrayList<>();
    private List<List<Entity>> entitiesToShowOrHideCombinations = new ArrayList<>();
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

    public void setEntitiesToShowOrHide(List<Entity> entitiesToShowOrHide) {
        this.entitiesToShowOrHide = new ArrayList<>(entitiesToShowOrHide);
        Collections.sort(this.entitiesToShowOrHide);
        entitiesToShowOrHideCombinations = getEntitiesCombinations();
        buildScenes();
    }

    private void buildScenes() {
        scenes.clear();

        for (long renderingTime : renderingTimes) {
            for (List<Entity> entitiesToShow : entitiesToShowOrHideCombinations) {
                scenes.add(new Scene(camera, renderingTimes, renderingTime, entitiesToShowOrHide, entitiesToShow));
            }
        }
    }

    public List<List<Entity>> getEntitiesCombinations() {
        List<List<Entity>> combinations = new ArrayList<>();
        List<Entity> inputList = new ArrayList<>(entitiesToShowOrHide);

        combinations.add(new ArrayList<>());
        _getCombinations(inputList, 0, new ArrayList<Entity>(), combinations);

        return combinations;
    }

    private void _getCombinations(List<Entity> inputList, int currentIndex, List<Entity> currentCombination, List<List<Entity>> combinations) {
        if (currentCombination.size() > 0)
            combinations.add(new ArrayList<>(currentCombination));

        for (int i = currentIndex; i < inputList.size(); i++) {
            currentCombination.add(inputList.get(i));
            _getCombinations(inputList, i + 1, currentCombination, combinations);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }
};
