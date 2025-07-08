package com.shmuelzon.HomeAssistantFloorPlan;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.media.j3d.BoundingBox;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3f;

import com.eteks.sweethome3d.j3d.HomePieceOfFurniture3D;
import com.eteks.sweethome3d.j3d.ModelManager;
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture;
import com.eteks.sweethome3d.model.HomeDoorOrWindow;
import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Transformation;


public class Entity implements Comparable<Entity> {
    public enum Property {ALWAYS_ON, OPEN_FURNITURE_CONDITION, DISPLAY_FURNITURE_CONDITION, IS_RGB, POSITION,}
    public enum DisplayType {BADGE, ICON, LABEL}
    public enum DisplayCondition {ALWAYS, NEVER, WHEN_ON, WHEN_OFF}
    public enum Action {MORE_INFO, NAVIGATE, NONE, TOGGLE}
    public enum DisplayFurnitureCondition {ALWAYS, STATE_EQUALS, STATE_NOT_EQUALS}
    public enum OpenFurnitureCondition {ALWAYS, STATE_EQUALS, STATE_NOT_EQUALS}

    private static final String SETTING_NAME_DISPLAY_TYPE = "displayType";
    private static final String SETTING_NAME_DISPLAY_CONDITION = "displayCondition";
    private static final String SETTING_NAME_TAP_ACTION = "tapAction";
    private static final String SETTING_NAME_TAP_ACTION_VALUE = "tapActionValue";
    private static final String SETTING_NAME_DOUBLE_TAP_ACTION = "doubleTapAction";
    private static final String SETTING_NAME_DOUBLE_TAP_ACTION_VALUE = "doubleTapActionValue";
    private static final String SETTING_NAME_HOLD_ACTION = "holdAction";
    private static final String SETTING_NAME_HOLD_ACTION_VALUE = "holdActionValue";
    private static final String SETTING_NAME_ALWAYS_ON = "alwaysOn";
    private static final String SETTING_NAME_IS_RGB = "isRgb";
    private static final String SETTING_NAME_LEFT_POSITION = "leftPosition";
    private static final String SETTING_NAME_TOP_POSITION = "topPosition";
    private static final String SETTING_NAME_OPACITY = "opacity";
    private static final String SETTING_NAME_BACKGROUND_COLOR = "backgroundColor";
    private static final String SETTING_NAME_DISPLAY_FURNITURE_CONDITION = "displayFurnitureCondition";
    private static final String SETTING_NAME_DISPLAY_FURNITURE_CONDITION_VALUE = "displayFurnitureConditionValue";
    private static final String SETTING_NAME_OPEN_FURNITURE_CONDITION = "openFurnitureCondition";
    private static final String SETTING_NAME_OPEN_FURNITURE_CONDITION_VALUE = "openFurnitureConditionValue";

    private List<? extends HomePieceOfFurniture> piecesOfFurniture;
    private String id;
    private String name;
    private Point2d position;
    private int opacity;
    private String backgroundColor;
    private DisplayType displayType;
    private DisplayCondition displayCondition;
    private Action tapAction;
    private String tapActionValue;
    private Action doubleTapAction;
    private String doubleTapActionValue;
    private Action holdAction;
    private String holdActionValue;
    private String title;
    private boolean isLight;
    private boolean alwaysOn;
    private boolean isRgb;
    private DisplayFurnitureCondition displayFurnitureCondition;
    private String displayFurnitureConditionValue;
    private OpenFurnitureCondition openFurnitureCondition;
    private String openFurnitureConditionValue;
    private Map<HomeLight, Float> initialPower;
    private boolean isDoorOrWindow;
    private Map<HomePieceOfFurniture, Point3f> initialLocation;
    private Map<HomePieceOfFurniture, Point3f> initialSize;
    private Map<HomePieceOfFurniture, Transformation[] > initialTransformations;

    private Settings settings;
    private boolean isUserDefinedPosition;
    private PropertyChangeSupport propertyChangeSupport;

    public Entity(Settings settings, List<? extends HomePieceOfFurniture> piecesOfFurniture) {
        this.settings = settings;
        this.piecesOfFurniture = piecesOfFurniture;
        propertyChangeSupport = new PropertyChangeSupport(this);
        initialPower = new HashMap<>();
        initialLocation = new HashMap<>();
        initialSize = new HashMap<>();
        initialTransformations = new HashMap<>();

        loadDefaultAttributes();
    }

    public void move(Vector2d direction) {
        if (isUserDefinedPosition)
            return;
        position.add(direction);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<? extends HomePieceOfFurniture> getPiecesOfFurniture() {
        return piecesOfFurniture;
    }

    public String getTitle() {
        return title;
    }

    public boolean getIsLight() {
        return isLight;
    }

    public boolean getIsDoorOrWindow() {
        return isDoorOrWindow;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
        settings.set(name + "." + SETTING_NAME_DISPLAY_TYPE, displayType.name());
    }

    public boolean isDisplayTypeModified() {
        return settings.get(name + "." + SETTING_NAME_DISPLAY_TYPE) != null;
    }

    public DisplayCondition getDisplayCondition() {
        return displayCondition;
    }

    public void setDisplayCondition(DisplayCondition displayCondition) {
        this.displayCondition = displayCondition;
        settings.set(name + "." + SETTING_NAME_DISPLAY_CONDITION, displayCondition.name());
    }

    public boolean isDisplayConditionModified() {
        return settings.get(name + "." + SETTING_NAME_DISPLAY_CONDITION) != null;
    }

    public Action getTapAction() {
        return tapAction;
    }

    public void setTapAction(Action tapAction) {
        this.tapAction = tapAction;
        settings.set(name + "." + SETTING_NAME_TAP_ACTION, tapAction.name());
    }

    public boolean isTapActionModified() {
        return settings.get(name + "." + SETTING_NAME_TAP_ACTION) != null;
    }

    public String getTapActionValue() {
        return tapActionValue;
    }

    public void setTapActionValue(String tapActionValue) {
        this.tapActionValue = tapActionValue;
        settings.set(name + "." + SETTING_NAME_TAP_ACTION_VALUE, tapActionValue);
    }

    public boolean isTapActionValueModified() {
        return settings.get(name + "." + SETTING_NAME_TAP_ACTION_VALUE) != null;
    }

    public Action getDoubleTapAction() {
        return doubleTapAction;
    }

    public void setDoubleTapAction(Action doubleTapAction) {
        this.doubleTapAction = doubleTapAction;
        settings.set(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION, doubleTapAction.name());
    }

    public boolean isDoubleTapActionModified() {
        return settings.get(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION) != null;
    }

    public String getDoubleTapActionValue() {
        return doubleTapActionValue;
    }

    public void setDoubleTapActionValue(String doubleTapActionValue) {
        this.doubleTapActionValue = doubleTapActionValue;
        settings.set(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION_VALUE, doubleTapActionValue);
    }

    public boolean isDoubleTapActionValueModified() {
        return settings.get(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION_VALUE) != null;
    }

    public Action getHoldAction() {
        return holdAction;
    }

    public void setHoldAction(Action holdAction) {
        this.holdAction = holdAction;
        settings.set(name + "." + SETTING_NAME_HOLD_ACTION, holdAction.name());
    }

    public boolean isHoldActionModified() {
        return settings.get(name + "." + SETTING_NAME_HOLD_ACTION) != null;
    }

    public String getHoldActionValue() {
        return holdActionValue;
    }

    public void setHoldActionValue(String holdActionValue) {
        this.holdActionValue = holdActionValue;
        settings.set(name + "." + SETTING_NAME_HOLD_ACTION_VALUE, holdActionValue);
    }

    public boolean isHoldActionValueModified() {
        return settings.get(name + "." + SETTING_NAME_HOLD_ACTION_VALUE) != null;
    }

    public boolean getAlwaysOn() {
        return alwaysOn;
    }

    public void setAlwaysOn(boolean alwaysOn) {
        boolean oldAlwaysOn = this.alwaysOn;
        this.alwaysOn = alwaysOn;
        settings.setBoolean(name + "." + SETTING_NAME_ALWAYS_ON, alwaysOn);
        propertyChangeSupport.firePropertyChange(Property.ALWAYS_ON.name(), oldAlwaysOn, alwaysOn);
    }

    public boolean isAlwaysOnModified() {
        return settings.get(name + "." + SETTING_NAME_ALWAYS_ON) != null;
    }

    public boolean getIsRgb() {
        return isRgb;
    }

    public void setIsRgb(boolean isRgb) {
        boolean oldIsRgb = this.isRgb;
        this.isRgb = isRgb;
        settings.setBoolean(name + "." + SETTING_NAME_IS_RGB, isRgb);
        propertyChangeSupport.firePropertyChange(Property.IS_RGB.name(), oldIsRgb, isRgb);
    }

    public boolean isIsRgbModified() {
        return settings.get(name + "." + SETTING_NAME_IS_RGB) != null;
    }

    public DisplayFurnitureCondition getDisplayFurnitureCondition() {
        return displayFurnitureCondition;
    }

    public void setDisplayFurnitureCondition(DisplayFurnitureCondition displayFurnitureCondition) {
        DisplayFurnitureCondition olddisplayFurnitureCondition = this.displayFurnitureCondition;
        this.displayFurnitureCondition = displayFurnitureCondition;
        settings.set(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION, displayFurnitureCondition.name());
        propertyChangeSupport.firePropertyChange(Property.DISPLAY_FURNITURE_CONDITION.name(), olddisplayFurnitureCondition, displayFurnitureCondition);
    }

    public boolean isDisplayFurnitureConditionModified() {
        return settings.get(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION) != null;
    }

    public String getDisplayFurnitureConditionValue() {
        return displayFurnitureConditionValue;
    }

    public void setDisplayFurnitureConditionValue(String displayFurnitureConditionValue) {
        this.displayFurnitureConditionValue = displayFurnitureConditionValue;
        settings.set(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION_VALUE, displayFurnitureConditionValue);
    }

    public boolean isDisplayFurnitureConditionValueModified() {
        return settings.get(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION_VALUE) != null;
    }

    public OpenFurnitureCondition getOpenFurnitureCondition() {
        return openFurnitureCondition;
    }

    public void setOpenFurnitureCondition(OpenFurnitureCondition openFurnitureCondition) {
        OpenFurnitureCondition oldOpenFurnitureCondition = this.openFurnitureCondition;
        this.openFurnitureCondition = openFurnitureCondition;
        settings.set(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION, openFurnitureCondition.name());
        propertyChangeSupport.firePropertyChange(Property.OPEN_FURNITURE_CONDITION.name(), oldOpenFurnitureCondition, openFurnitureCondition);
    }

    public boolean isOpenFurnitureConditionModified() {
        return settings.get(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION) != null;
    }

    public String getOpenFurnitureConditionValue() {
        return openFurnitureConditionValue;
    }

    public void setOpenFurnitureConditionValue(String openFurnitureConditionValue) {
        this.openFurnitureConditionValue = openFurnitureConditionValue;
        settings.set(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION_VALUE, openFurnitureConditionValue);
    }

    public boolean isOpenFurnitureConditionValueModified() {
        return settings.get(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION_VALUE) != null;
    }

    public Point2d getPosition() {
        return new Point2d(position);
    }

    public void setPosition(Point2d position, boolean savePersistent) {
        if (isUserDefinedPosition && !savePersistent)
            return;

        Point2d oldPosition = getPosition();
        this.position = new Point2d(position);

        if (!savePersistent)
            return;

        settings.setDouble(name + "." + SETTING_NAME_LEFT_POSITION, position.x);
        settings.setDouble(name + "." + SETTING_NAME_TOP_POSITION, position.y);
        isUserDefinedPosition = true;
        propertyChangeSupport.firePropertyChange(Property.POSITION.name(), oldPosition, position);
    }

    public boolean isPositionModified() {
        return settings.get(name + "." + SETTING_NAME_LEFT_POSITION) != null;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
        settings.setInteger(name + "." + SETTING_NAME_OPACITY, opacity);
    }

    public boolean isOpacityModified() {
        return settings.get(name + "." + SETTING_NAME_OPACITY) != null;
    }

    public String getBackgrounColor() {
        return backgroundColor;
    }

    public void setBackgrounColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        settings.set(name + "." + SETTING_NAME_BACKGROUND_COLOR, backgroundColor);
    }

    public boolean isBackgroundColorModified() {
        return settings.get(name + "." + SETTING_NAME_BACKGROUND_COLOR) != null;
    }

    public void resetToDefaults() {
        boolean oldAlwaysOn = alwaysOn;
        boolean oldIsRgb = isRgb;
        Point2d oldPosition = getPosition();

        settings.set(name + "." + SETTING_NAME_DISPLAY_TYPE, null);
        settings.set(name + "." + SETTING_NAME_DISPLAY_CONDITION, null);
        settings.set(name + "." + SETTING_NAME_TAP_ACTION, null);
        settings.set(name + "." + SETTING_NAME_TAP_ACTION_VALUE, null);
        settings.set(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION, null);
        settings.set(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION_VALUE, null);
        settings.set(name + "." + SETTING_NAME_HOLD_ACTION, null);
        settings.set(name + "." + SETTING_NAME_HOLD_ACTION_VALUE, null);
        settings.set(name + "." + SETTING_NAME_ALWAYS_ON, null);
        settings.set(name + "." + SETTING_NAME_IS_RGB, null);
        settings.set(name + "." + SETTING_NAME_LEFT_POSITION, null);
        settings.set(name + "." + SETTING_NAME_TOP_POSITION, null);
        settings.set(name + "." + SETTING_NAME_OPACITY, null);
        settings.set(name + "." + SETTING_NAME_BACKGROUND_COLOR, null);
        settings.set(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION, null);
        settings.set(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION_VALUE, null);
        settings.set(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION, null);
        settings.set(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION_VALUE, null);
        loadDefaultAttributes();

        propertyChangeSupport.firePropertyChange(Property.ALWAYS_ON.name(), oldAlwaysOn, alwaysOn);
        propertyChangeSupport.firePropertyChange(Property.IS_RGB.name(), oldIsRgb, isRgb);
        propertyChangeSupport.firePropertyChange(Property.POSITION.name(), oldPosition, position);
    }

    public void setLightPower(boolean on) {
        if (!isLight)
            return;

        for (Map.Entry<HomeLight, Float> entry : initialPower.entrySet())
            entry.getKey().setPower(on ? entry.getValue() : 0);
    }

    public void setDoorOrWindowState(boolean open) {
        if (!isDoorOrWindow)
            return;

        if (!open) {
            for (HomePieceOfFurniture piece : initialTransformations.keySet())
                closeDoorOrWindow(piece);
            return;
        }

        for (HomePieceOfFurniture piece : initialTransformations.keySet()) {
            piece.setX(initialLocation.get(piece).getX());
            piece.setY(initialLocation.get(piece).getY());
            piece.setElevation(initialLocation.get(piece).getZ());

            piece.setWidth(initialSize.get(piece).getX());
            piece.setDepth(initialSize.get(piece).getY());
            piece.setHeight(initialSize.get(piece).getZ());

            piece.setModelTransformations(initialTransformations.get(piece));
        }
    }

    public void restoreConfiguration() {
        setVisible(true);

        if (isLight)
            setLightPower(true);

        if (isDoorOrWindow)
            setDoorOrWindowState(true);
    }

    public void setVisible(boolean visible) {
        for (HomePieceOfFurniture piece : piecesOfFurniture)
            piece.setVisible(visible);
    }

    private String actionYaml(Action action, String value) {
        final Map<Action, String> actionToYamlString = new HashMap<Action, String>() {{
            put(Action.MORE_INFO, "more-info");
            put(Action.NAVIGATE, "navigate");
            put(Action.NONE, "none");
            put(Action.TOGGLE, "toggle");
        }};

        String yaml = actionToYamlString.get(action);

        if (action == Action.NAVIGATE)
            yaml += String.format("\n" +
                "      navigation_path: %s", value);

        return yaml;
    }

    public String buildYaml() {
        final Map<DisplayType, String> displayTypeToYamlString = new HashMap<DisplayType, String>() {{
            put(DisplayType.BADGE, "state-badge");
            put(DisplayType.ICON, "state-icon");
            put(DisplayType.LABEL, "state-label");
        }};

        if (displayCondition == Entity.DisplayCondition.NEVER || getAlwaysOn())
            return "";

        String yaml = String.format(Locale.US,
            "  - type: %s\n" +
            "    entity: %s\n" +
            "    title: %s\n" +
            "    style:\n" +
            "      top: %.2f%%\n" +
            "      left: %.2f%%\n" +
            "      border-radius: 50%%\n" +
            "      text-align: center\n" +
            "      background-color: %s\n" +
            "      opacity: %d%%\n" +
            "    tap_action:\n" +
            "      action: %s\n" +
            "    double_tap_action:\n" +
            "      action: %s\n" +
            "    hold_action:\n" +
            "      action: %s\n",
            displayTypeToYamlString.get(displayType), name, title, position.y, position.x, backgroundColor, opacity,
            actionYaml(tapAction, tapActionValue), actionYaml(doubleTapAction, doubleTapActionValue), actionYaml(holdAction, holdActionValue));

        if (displayCondition == DisplayCondition.ALWAYS)
            return yaml;

        return String.format(
            "  - type: conditional\n" +
            "    conditions:\n" +
            "      - condition: state\n" +
            "        entity: %s\n" +
            "        state: '%s'\n" +
            "    elements:\n" +
            "%s",
            name, displayCondition == DisplayCondition.WHEN_ON ? "on" : "off",
            yaml.replaceAll(".*\\R", "    $0")
        );
    }

    private void saveInitialLightPowerValues() {
        if (!isLight)
            return;

        for (HomePieceOfFurniture piece : piecesOfFurniture) {
            HomeLight light = (HomeLight)piece;
            initialPower.put(light, light.getPower());
        }
    }

    private void saveInitialDoorOrWindowState() {
        if (!isDoorOrWindow)
            return;

        for (HomePieceOfFurniture piece : piecesOfFurniture) {
            initialLocation.put(piece, new Point3f(piece.getX(), piece.getY(), piece.getElevation()));
            initialSize.put(piece, new Point3f(piece.getWidth(), piece.getDepth(), piece.getHeight()));
            initialTransformations.put(piece, piece.getModelTransformations());
        }
    }

    public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
    }

    public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
    }

    public String toString() {
        return name;
    }

    private <T extends Enum<T>> T getSavedEnumValue(Class<T> type, String name, T defaultValue) {
        try {
            return Enum.valueOf(type, settings.get(name, defaultValue.name()));
        } catch (IllegalArgumentException e) {
            settings.set(name, null);
        }
        return defaultValue;
    }

    private void loadDefaultAttributes() {
        HomePieceOfFurniture firstPiece = piecesOfFurniture.get(0);
        id = firstPiece.getId();
        name = firstPiece.getName();
        position = loadPosition();
        displayType = getSavedEnumValue(DisplayType.class, name + "." + SETTING_NAME_DISPLAY_TYPE, defaultDisplayType());
        displayCondition = getSavedEnumValue(DisplayCondition.class, name + "." + SETTING_NAME_DISPLAY_CONDITION, DisplayCondition.ALWAYS);
        tapAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_TAP_ACTION, defaultAction());
        tapActionValue = settings.get(name + "." + SETTING_NAME_TAP_ACTION_VALUE, "");
        doubleTapAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_DOUBLE_TAP_ACTION, Action.NONE);
        doubleTapActionValue = settings.get(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION_VALUE, "");
        holdAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_HOLD_ACTION, Action.MORE_INFO);
        holdActionValue = settings.get(name + "." + SETTING_NAME_HOLD_ACTION_VALUE, "");
        title = firstPiece.getDescription();
        opacity = settings.getInteger(name + "." + SETTING_NAME_OPACITY, 100);
        backgroundColor = settings.get(name + "." + SETTING_NAME_BACKGROUND_COLOR, "rgba(255, 255, 255, 0.3)");
        alwaysOn = settings.getBoolean(name + "." + SETTING_NAME_ALWAYS_ON, false);
        isRgb = settings.getBoolean(name + "." + SETTING_NAME_IS_RGB, false);
        displayFurnitureCondition = getSavedEnumValue(DisplayFurnitureCondition.class, name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION, DisplayFurnitureCondition.ALWAYS);
        displayFurnitureConditionValue = settings.get(name + "." + SETTING_NAME_DISPLAY_FURNITURE_CONDITION_VALUE, "");
        openFurnitureCondition = getSavedEnumValue(OpenFurnitureCondition.class, name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION, OpenFurnitureCondition.ALWAYS);
        openFurnitureConditionValue = settings.get(name + "." + SETTING_NAME_OPEN_FURNITURE_CONDITION_VALUE, "");

        isLight = firstPiece instanceof HomeLight;
        isDoorOrWindow = firstPiece instanceof HomeDoorOrWindow;
        saveInitialLightPowerValues();
        saveInitialDoorOrWindowState();
    }

    private DisplayType defaultDisplayType() {
        return name.startsWith("sensor.") ? DisplayType.LABEL : DisplayType.ICON;
    }

    public int compareTo(Entity other) {
        return getName().compareTo(other.getName());
    }

    private Action defaultAction() {
        String[] actionableEntityPrefixes = {
            "alarm_control_panel.",
            "button.",
            "climate.",
            "cover.",
            "fan.",
            "humidifier.",
            "lawn_mower.",
            "light.",
            "lock.",
            "media_player.",
            "switch.",
            "vacuum.",
            "valve.",
            "water_header.",
        };

        for (String prefix : actionableEntityPrefixes ) {
            if (name.startsWith(prefix))
                return Action.TOGGLE;
        }
        return Action.MORE_INFO;
    }

    private Point2d loadPosition() {
        double leftPosition = settings.getDouble(name + "." + SETTING_NAME_LEFT_POSITION, -1);
        double topPosition = settings.getDouble(name + "." + SETTING_NAME_TOP_POSITION, -1);
        if (leftPosition != -1 || topPosition != -1) {
            isUserDefinedPosition = true;
            return new Point2d(leftPosition, topPosition);
        }

        isUserDefinedPosition = false;
        return new Point2d(0, 0);
    }

    private void closeDoorOrWindow(HomePieceOfFurniture piece) {
        if (piece.getModelTransformations() == null) {
            return;
        }

        /* Build 3D scene with floor plan piece */
        ModelManager modelManager = ModelManager.getInstance();
        BranchGroup sceneTree = new BranchGroup();
        TransformGroup modelTransformGroup = new TransformGroup();
        BranchGroup modelRoot;

        sceneTree.addChild(modelTransformGroup);
        try {
            modelRoot = modelManager.loadModel(piece.getModel());
        } catch (Exception ex) {
            return;
        }

        if (modelRoot.numChildren() == 0)
            return;

        Vector3f size = piece.getWidth() < 0 ? modelManager.getSize(modelRoot) : new Vector3f(piece.getWidth(), piece.getHeight(), piece.getDepth());
        HomePieceOfFurniture modelPiece = new HomePieceOfFurniture(new CatalogPieceOfFurniture(null, null, piece.getModel(), size.x, size.z, size.y, 0, false, null, null, piece.getModelRotation(), piece.getModelFlags(), null, null, 0, 0, 1, false));
        modelPiece.setX(0);
        modelPiece.setY(0);
        modelPiece.setElevation(-modelPiece.getHeight() / 2);

        HomePieceOfFurniture3D piece3D = new HomePieceOfFurniture3D(modelPiece, null);
        modelTransformGroup.addChild(piece3D);

        modelPiece.setModelTransformations(piece.getModelTransformations());
        piece3D.update();

        /* Remove transformations from 3D model and update modelPiece's size and location */
        BoundingBox oldBounds = modelManager.getBounds(piece3D);
        Point3d oldLower = new Point3d();
        oldBounds.getLower(oldLower);
        Point3d oldUpper = new Point3d();
        oldBounds.getUpper(oldUpper);

        setNodeTransformations(piece3D, null);

        BoundingBox newBounds = modelManager.getBounds(piece3D);
        Point3d newLower = new Point3d();
        newBounds.getLower(newLower);
        Point3d newUpper = new Point3d();
        newBounds.getUpper(newUpper);
        modelPiece.setX(modelPiece.getX() + (float)(newUpper.x + newLower.x) / 2 - (float)(oldUpper.x + oldLower.x) / 2);
        modelPiece.setY(modelPiece.getY() + (float)(newUpper.z + newLower.z) / 2 - (float)(oldUpper.z + oldLower.z) / 2);
        modelPiece.setElevation(modelPiece.getElevation() + (float)(newLower.y - oldLower.y));
        modelPiece.setWidth((float)(newUpper.x - newLower.x));
        modelPiece.setDepth((float)(newUpper.z - newLower.z));
        modelPiece.setHeight((float)(newUpper.y - newLower.y));
        modelPiece.setModelTransformations(null);

        /* Update location and size of the floor plan piece */
        float modelX = piece.isModelMirrored() ? -modelPiece.getX() : modelPiece.getX();
        float modelY = modelPiece.getY();
        float pieceX = (float)(piece.getX() + modelX * Math.cos(piece.getAngle()) - modelY * Math.sin(piece.getAngle()));
        float pieceY = (float)(piece.getY() + modelX * Math.sin(piece.getAngle()) + modelY * Math.cos(piece.getAngle()));
        float pieceElevation = piece.getElevation() + modelPiece.getElevation() + piece.getHeight() / 2;
        piece.setModelTransformations(new Transformation [0]);
        piece.setX(pieceX);
        piece.setY(pieceY);
        piece.setElevation(pieceElevation);
        piece.setWidth(modelPiece.getWidth());
        piece.setDepth(modelPiece.getDepth());
        piece.setHeight(modelPiece.getHeight());
    }

    private void setNodeTransformations(Node node, Transformation [] transformations) {
        if (node instanceof Group) {
            if (node instanceof TransformGroup
                    && node.getUserData() instanceof String
                    && ((String)node.getUserData()).endsWith(ModelManager.DEFORMABLE_TRANSFORM_GROUP_SUFFIX)) {
                TransformGroup transformGroup = (TransformGroup)node;
                transformGroup.setTransform(new Transform3D());
                if (transformations != null) {
                    String transformationName = (String)node.getUserData();
                    transformationName = transformationName.substring(0, transformationName.length() - ModelManager.DEFORMABLE_TRANSFORM_GROUP_SUFFIX.length());
                    for (Transformation transformation : transformations) {
                        if (transformationName.equals(transformation.getName())) {
                            float [][] matrix = transformation.getMatrix();
                            Matrix4f transformMatrix = new Matrix4f();
                            transformMatrix.setRow(0, matrix[0]);
                            transformMatrix.setRow(1, matrix[1]);
                            transformMatrix.setRow(2, matrix[2]);
                            transformMatrix.setRow(3, new float [] {0, 0, 0, 1});
                            transformGroup.setTransform(new Transform3D(transformMatrix));
                        }
                    }
                }
            }
            Enumeration<?> enumeration = ((Group)node).getAllChildren();
            while (enumeration.hasMoreElements()) {
                setNodeTransformations((Node)enumeration.nextElement(), transformations);
            }
        }
    }

}
