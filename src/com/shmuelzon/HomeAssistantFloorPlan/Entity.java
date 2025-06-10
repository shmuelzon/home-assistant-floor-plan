package com.shmuelzon.HomeAssistantFloorPlan;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;


public class Entity implements Comparable<Entity> {
    public enum Property {ALWAYS_ON, DISPLAY_FURNITURE_CONDITION, IS_RGB, POSITION,}
    public enum DisplayType {BADGE, ICON, LABEL}
    public enum Action {MORE_INFO, NAVIGATE, NONE, TOGGLE}
    
    // Deprecated enums, kept for reference but logic is replaced by strings/new enums
    public enum DisplayFurnitureCondition {ALWAYS, STATE_EQUALS, STATE_NOT_EQUALS}
    public enum DisplayCondition {ALWAYS, NEVER, WHEN_ON, WHEN_OFF}

    // --- NEW: Enum for the different operators ---
    public enum DisplayOperator { IS, IS_NOT, GREATER_THAN, LESS_THAN, ALWAYS, NEVER }

    // --- Settings Constants ---
    private static final String SETTING_NAME_DISPLAY_TYPE = "displayType";
    private static final String SETTING_NAME_DISPLAY_OPERATOR = "displayOperator";
    private static final String SETTING_NAME_DISPLAY_VALUE = "displayValue";
    private static final String SETTING_NAME_FURNITURE_DISPLAY_OPERATOR = "furnitureDisplayOperator";
    private static final String SETTING_NAME_FURNITURE_DISPLAY_VALUE = "furnitureDisplayValue";
    
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

    // --- Fields ---
    private List<? extends HomePieceOfFurniture> piecesOfFurniture;
    private String id;
    private String name;
    private Point2d position;
    private int opacity;
    private String backgroundColor;
    private DisplayType displayType;
    private DisplayOperator displayOperator;
    private String displayValue;
    private DisplayOperator furnitureDisplayOperator;
    private String furnitureDisplayValue;
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
    private Map<HomeLight, Float> initialPower;
    private Settings settings;
    private boolean isUserDefinedPosition;
    private PropertyChangeSupport propertyChangeSupport;

    public Entity(Settings settings, List<? extends HomePieceOfFurniture> piecesOfFurniture) {
        this.settings = settings;
        this.piecesOfFurniture = piecesOfFurniture;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.initialPower = new HashMap<>();
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
    
    public DisplayOperator getDisplayOperator() {
        return displayOperator;
    }

    public void setDisplayOperator(DisplayOperator displayOperator) {
        this.displayOperator = displayOperator;
        settings.set(name + "." + SETTING_NAME_DISPLAY_OPERATOR, displayOperator.name());
    }
    
    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
        settings.set(name + "." + SETTING_NAME_DISPLAY_VALUE, displayValue);
    }
    
    public boolean isDisplayConditionModified() {
        return settings.get(name + "." + SETTING_NAME_DISPLAY_OPERATOR) != null 
            || settings.get(name + "." + SETTING_NAME_DISPLAY_VALUE) != null;
    }

    public DisplayOperator getFurnitureDisplayOperator() {
        return furnitureDisplayOperator;
    }

    public void setFurnitureDisplayOperator(DisplayOperator furnitureDisplayOperator) {
        this.furnitureDisplayOperator = furnitureDisplayOperator;
        settings.set(name + "." + SETTING_NAME_FURNITURE_DISPLAY_OPERATOR, furnitureDisplayOperator.name());
        propertyChangeSupport.firePropertyChange(Property.DISPLAY_FURNITURE_CONDITION.name(), null, furnitureDisplayOperator);
    }

    public String getFurnitureDisplayValue() {
        return furnitureDisplayValue;
    }

    public void setFurnitureDisplayValue(String furnitureDisplayValue) {
        this.furnitureDisplayValue = furnitureDisplayValue;
        settings.set(name + "." + SETTING_NAME_FURNITURE_DISPLAY_VALUE, furnitureDisplayValue);
        propertyChangeSupport.firePropertyChange(Property.DISPLAY_FURNITURE_CONDITION.name(), null, furnitureDisplayValue);
    }
    
    public boolean isFurnitureDisplayConditionModified() {
        return settings.get(name + "." + SETTING_NAME_FURNITURE_DISPLAY_OPERATOR) != null 
            || settings.get(name + "." + SETTING_NAME_FURNITURE_DISPLAY_VALUE) != null;
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
        settings.set(name + "." + SETTING_NAME_DISPLAY_OPERATOR, null);
        settings.set(name + "." + SETTING_NAME_DISPLAY_VALUE, null);
        settings.set(name + "." + SETTING_NAME_FURNITURE_DISPLAY_OPERATOR, null);
        settings.set(name + "." + SETTING_NAME_FURNITURE_DISPLAY_VALUE, null);
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

    public void restoreConfiguration() {
        setVisible(true);

        if (!isLight)
            return;

        for (Map.Entry<HomeLight, Float> entry : initialPower.entrySet())
            entry.getKey().setPower(entry.getValue());
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
                "        navigation_path: %s", value);

        return yaml;
    }

    public String buildYaml() {
        final Map<DisplayType, String> displayTypeToYamlString = new HashMap<DisplayType, String>() {{
            put(DisplayType.BADGE, "state-badge");
            put(DisplayType.ICON, "state-icon");
            put(DisplayType.LABEL, "state-label");
        }};
        
        String elementYaml = String.format(Locale.US,
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
            actionYaml(tapAction, tapActionValue), actionYaml(doubleTapAction, doubleTapActionValue), actionYaml(holdAction, holdActionValue)
        );

        // Handle ALWAYS and NEVER operators explicitly
        if (this.displayOperator == DisplayOperator.ALWAYS) {
            return elementYaml;
        }

        if (this.displayOperator == DisplayOperator.NEVER) {
            // If the operator is NEVER, the element should not appear in the YAML at all.
            // Returning an empty string prevents it from being added to the elements list.
            return "";
        }

        String conditionYaml;
        switch (this.displayOperator) {
            case IS:
                conditionYaml = String.format(
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    "        state: '%s'",
                    name, this.displayValue);
                break;
            case IS_NOT:
                conditionYaml = String.format(
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    "        state_not: '%s'",
                    name, this.displayValue);
                break;
            case GREATER_THAN:
                conditionYaml = String.format(
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    "        above: %s",
                    name, this.displayValue);
                break;
            case LESS_THAN:
                conditionYaml = String.format(
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    "        below: %s",
                    name, this.displayValue);
                break;
            default:
                // This case should ideally not be reached if the enum is handled,
                // but as a fallback, return empty string to avoid unexpected elements.
                return "";
        }
        return String.format(
            "  - type: conditional\n" +
            "    conditions:\n" +
            "%s\n" +
            "    elements:\n" +
            "%s",
            conditionYaml,
            elementYaml.replaceAll("(?m)^", "    ")
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
        
        displayOperator = getSavedEnumValue(DisplayOperator.class, name + "." + SETTING_NAME_DISPLAY_OPERATOR, DisplayOperator.ALWAYS);
        displayValue = settings.get(name + "." + SETTING_NAME_DISPLAY_VALUE, "");
        
        furnitureDisplayOperator = getSavedEnumValue(DisplayOperator.class, name + "." + SETTING_NAME_FURNITURE_DISPLAY_OPERATOR, DisplayOperator.ALWAYS);
        // --- MODIFIED: Default to an empty string to prevent OutOfMemoryError ---
        furnitureDisplayValue = settings.get(name + "." + SETTING_NAME_FURNITURE_DISPLAY_VALUE, "");

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

        isLight = firstPiece instanceof HomeLight;
        saveInitialLightPowerValues();
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
}
