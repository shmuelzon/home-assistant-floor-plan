package com.shmuelzon.HomeAssistantFloorPlan;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;


public class Entity implements Comparable<Entity> {
    public enum Property {ALWAYS_ON, IS_RGB, POSITION, SCALE_FACTOR, DISPLAY_CONDITION, FURNITURE_DISPLAY_CONDITION} // Added DISPLAY_CONDITION & FURNITURE_DISPLAY_CONDITION
    public enum DisplayType {BADGE, ICON, LABEL} // Removed ROOM_SIZE from here, it's a ClickableAreaType
    public enum ClickableAreaType { ENTITY_SIZE, ROOM_SIZE } // Added missing enum definition
    public enum Action {MORE_INFO, NAVIGATE, NONE, TOGGLE}

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
    private static final String SETTING_NAME_BLINKING = "blinking";
    private static final String SETTING_NAME_OPACITY = "opacity";
    private static final String SETTING_NAME_BACKGROUND_COLOR = "backgroundColor";
    private static final String SETTING_NAME_SCALE_FACTOR = "scaleFactor"; // Added missing constant
    private static final String SETTING_NAME_CLICKABLE_AREA_TYPE = "clickableAreaType";

    // --- Fields ---
    private List<? extends HomePieceOfFurniture> piecesOfFurniture;
    private String id;
    private String name;
    private Point2d position;
    private boolean blinking;
    private int opacity;
    private double scaleFactor;
    private String backgroundColor;
    private DisplayType displayType;
    private ClickableAreaType clickableAreaType;
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
    private double defaultIconBadgeBaseSizePercent;

    public Entity(Settings settings, List<? extends HomePieceOfFurniture> piecesOfFurniture, ResourceBundle resourceBundle) {
        this.settings = settings;
        this.piecesOfFurniture = piecesOfFurniture;
        propertyChangeSupport = new PropertyChangeSupport(this);
        initialPower = new HashMap<>();

        try {
            String sizeStr = resourceBundle.getString("HomeAssistantFloorPlan.Entity.defaultIconBadgeBaseSizePercent");
            this.defaultIconBadgeBaseSizePercent = Double.parseDouble(sizeStr);
        } catch (MissingResourceException | NumberFormatException | NullPointerException e) {
            // Log a warning or use a hardcoded default if the resource bundle or key is not found, or if parsing fails
            System.err.println("Warning: Could not load 'HomeAssistantFloorPlan.Entity.defaultIconBadgeBaseSizePercent' from properties. Using hardcoded default (5.0%). " + e.getMessage());
            this.defaultIconBadgeBaseSizePercent = 5.0; // Hardcoded fallback
        }

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
        propertyChangeSupport.firePropertyChange(Property.FURNITURE_DISPLAY_CONDITION.name(), null, furnitureDisplayOperator);
    }

    public String getFurnitureDisplayValue() {
        return furnitureDisplayValue;
    }

    public void setFurnitureDisplayValue(String furnitureDisplayValue) {
        this.furnitureDisplayValue = furnitureDisplayValue;
        settings.set(name + "." + SETTING_NAME_FURNITURE_DISPLAY_VALUE, furnitureDisplayValue);
        propertyChangeSupport.firePropertyChange(Property.FURNITURE_DISPLAY_CONDITION.name(), null, furnitureDisplayValue);
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

    public boolean getBlinking() {
        return blinking;
    }

    public void setBlinking(boolean blinking) {
        this.blinking = blinking;
        settings.setBoolean(name + "." + SETTING_NAME_BLINKING, blinking);
    }

    public boolean isBlinkingModified() {
        return settings.get(name + "." + SETTING_NAME_BLINKING) != null;
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

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        double oldScaleFactor = this.scaleFactor;
        this.scaleFactor = scaleFactor;
        settings.setDouble(name + "." + SETTING_NAME_SCALE_FACTOR, scaleFactor);
        propertyChangeSupport.firePropertyChange(Property.SCALE_FACTOR.name(), oldScaleFactor, scaleFactor);
    }

    public boolean isScaleFactorModified() {
        return settings.get(name + "." + SETTING_NAME_SCALE_FACTOR) != null;
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

    public ClickableAreaType getClickableAreaType() {
        return clickableAreaType;
    }

    public void setClickableAreaType(ClickableAreaType clickableAreaType) {
        this.clickableAreaType = clickableAreaType;
        settings.set(name + "." + SETTING_NAME_CLICKABLE_AREA_TYPE, clickableAreaType.name());
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
        settings.set(name + "." + SETTING_NAME_BLINKING, null);
        settings.set(name + "." + SETTING_NAME_OPACITY, null);
        settings.set(name + "." + SETTING_NAME_BACKGROUND_COLOR, null);
        settings.set(name + "." + SETTING_NAME_SCALE_FACTOR, null); // Added reset for scale factor
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

    public String buildYaml(Controller controller) { // Pass controller to get room bounds
        final Map<DisplayType, String> displayTypeToYamlString = new HashMap<DisplayType, String>() {{
            put(DisplayType.BADGE, "state-badge");
            put(DisplayType.ICON, "state-icon");
            put(DisplayType.LABEL, "state-label");
        }};

        // Use the new displayOperator logic
        if (this.displayOperator == DisplayOperator.NEVER && !getAlwaysOn()) {
            return "";
        }

        StringBuilder styleProperties = new StringBuilder();
        styleProperties.append(String.format(Locale.US, "      top: %.2f%%\n", position.y));
        styleProperties.append(String.format(Locale.US, "      left: %.2f%%\n", position.x));

        // Base transform for centering the visual element
        String visualElementTransform = "translate(-50%, -50%)";
        styleProperties.append("      border-radius: 50%\n"); // Common for icons/badges
        styleProperties.append("      text-align: center\n"); // Common for labels
        styleProperties.append(String.format(Locale.US, "      background-color: %s\n", backgroundColor));

        if (blinking) {
            styleProperties.append("      animation: my-blink 1s linear infinite\n");
        // When blinking, opacity is controlled by animation, so we don't set static opacity.
        } else if (clickableAreaType == ClickableAreaType.ROOM_SIZE) {
            styleProperties.append(String.format(Locale.US, "      opacity: %d%%\n", opacity)); // Visual opacity
        } else {
            styleProperties.append(String.format(Locale.US, "      opacity: %d%%\n", opacity));
        }

        // Apply scaleFactor based on displayType AFTER basic transform and other common styles
        if (displayType == DisplayType.ICON || displayType == DisplayType.BADGE) {
            double elementSizePercent = scaleFactor * this.defaultIconBadgeBaseSizePercent;
            styleProperties.append(String.format(Locale.US, "      width: %.2f%%\n", elementSizePercent));
            styleProperties.append(String.format(Locale.US, "      height: %.2f%%\n", elementSizePercent));
            styleProperties.append(String.format(Locale.US, "      transform: %s;\n", visualElementTransform));
        } else { // LABEL
            visualElementTransform += String.format(Locale.US, " scale(%.2f)", scaleFactor);
            styleProperties.append(String.format(Locale.US, "      transform: %s;\n", visualElementTransform));
        }

        if (clickableAreaType == ClickableAreaType.ROOM_SIZE) {
            styleProperties.append("      pointer-events: none;\n"); // Make visual element non-clickable
        }
        

        String elementYaml = String.format(Locale.US, // Use elementYaml consistently
            "  - type: %s\n" +
            "    entity: %s\n" +
            (displayType == DisplayType.BADGE ? "" : "    title: " + title + "\n") + // Badges don't have titles
            "    style:\n" +
            "%s" + // Insert constructed style properties here
            "    tap_action:\n" +
      "      action: %s\n" +
            "    double_tap_action:\n" +
            "      action: %s\n" +
            "    hold_action:\n" +
            "      action: %s\n",
            displayTypeToYamlString.get(displayType), name, title,
            // title, // title is now conditionally added
            styleProperties.toString(), // This is the style for the visual element
            actionYaml(tapAction, tapActionValue),
            actionYaml(doubleTapAction, doubleTapActionValue),
            actionYaml(holdAction, holdActionValue));

        String clickableAreaYaml = "";
        if (clickableAreaType == ClickableAreaType.ROOM_SIZE && controller != null) {
            Map<String, Double> roomBounds = controller.getRoomBoundingBoxPercent(this);
            if (roomBounds != null) {
                clickableAreaYaml = String.format(Locale.US,
                    "  - type: image\n" + // Use a transparent image for the clickable area
                    "    entity: %s\n" +
                    "    image: /local/floorplan/transparent.png\n" + // Assuming transparent.png exists
                    "    tap_action:\n" +
                    "      action: %s\n" +
                    "    double_tap_action:\n" +
                    "      action: %s\n" +
                    "    hold_action:\n" +
                    "      action: %s\n" +
                    "    style:\n" +
                    "      top: %.2f%%\n" +
                    "      left: %.2f%%\n" +
                    "      width: %.2f%%\n" +
                    "      height: %.2f%%\n" +
                    "      opacity: 0;\n" + // Make it invisible
                    "      background-color: rgba(0,0,0,0.0);\n", // Explicitly transparent
                    name,
                    actionYaml(tapAction, tapActionValue), actionYaml(doubleTapAction, doubleTapActionValue), actionYaml(holdAction, holdActionValue),
                    roomBounds.get("top"), roomBounds.get("left"), roomBounds.get("width"), roomBounds.get("height"));
            }
        }
        elementYaml = clickableAreaYaml + elementYaml; // Prepend clickable area if it exists

        // Handle ALWAYS and NEVER operators explicitly
        if (this.displayOperator == DisplayOperator.ALWAYS || getAlwaysOn()) { // Consider alwaysOn as well
            return elementYaml;
        }

        if (this.displayOperator == DisplayOperator.NEVER && !getAlwaysOn()) { // If NEVER and not forced by alwaysOn
            // If the operator is NEVER for the entity itself, the element should not appear.
            // Returning an empty string prevents it from being added to the elements list.
            return "";
        }

        String conditionYaml;
        switch (this.displayOperator) {
            case IS:
                conditionYaml = String.format(
                    "    conditions:\n" + // Ensure conditions block starts here
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    "        state: '%s'",
                    name, this.displayValue);
                break;
            case IS_NOT:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    "        state_not: '%s'",
                    name, this.displayValue);
                break;
            case GREATER_THAN:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    "        above: %s",
                    name, this.displayValue);
                break;
            case LESS_THAN:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    "        below: %s",
                    name, this.displayValue);
                break;
            default:
                // Fallback for unhandled or ALWAYS/NEVER if logic changes (should be caught above)
                return elementYaml; // Or handle as error
        }
        return String.format(
            "  - type: conditional\n" +
            "%s\n" +
            "    elements:\n" +
            "%s",
            conditionYaml, // Use the generated conditionYaml
            elementYaml.replaceAll("(?m)^", "    ") // Indent the elementYaml
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
        clickableAreaType = getSavedEnumValue(ClickableAreaType.class, name + "." + SETTING_NAME_CLICKABLE_AREA_TYPE, ClickableAreaType.ENTITY_SIZE);

        tapAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_TAP_ACTION, defaultAction());
        tapActionValue = settings.get(name + "." + SETTING_NAME_TAP_ACTION_VALUE, "");
        doubleTapAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_DOUBLE_TAP_ACTION, Action.NONE);
        doubleTapActionValue = settings.get(name + "." + SETTING_NAME_DOUBLE_TAP_ACTION_VALUE, "");
        holdAction = getSavedEnumValue(Action.class, name + "." + SETTING_NAME_HOLD_ACTION, Action.MORE_INFO);
        holdActionValue = settings.get(name + "." + SETTING_NAME_HOLD_ACTION_VALUE, "");
        blinking = settings.getBoolean(name + "." + SETTING_NAME_BLINKING, false);
        title = firstPiece.getDescription();
        opacity = settings.getInteger(name + "." + SETTING_NAME_OPACITY, 100);
        backgroundColor = settings.get(name + "." + SETTING_NAME_BACKGROUND_COLOR, "rgba(255, 255, 255, 0.3)");
        scaleFactor = settings.getDouble(name + "." + SETTING_NAME_SCALE_FACTOR, 1.0);
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
