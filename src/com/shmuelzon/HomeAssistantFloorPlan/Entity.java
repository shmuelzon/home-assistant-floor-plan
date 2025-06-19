package com.shmuelzon.HomeAssistantFloorPlan;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.io.IOException;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;


public class Entity implements Comparable<Entity> {
    public enum Property {ALWAYS_ON, IS_RGB, POSITION, SCALE_FACTOR, DISPLAY_CONDITION, FURNITURE_DISPLAY_CONDITION} // Added DISPLAY_CONDITION & FURNITURE_DISPLAY_CONDITION
    public enum DisplayType {BADGE, ICON, LABEL, ICON_AND_ANIMATED_FAN}
    public enum ClickableAreaType { ENTITY_SIZE, ROOM_SIZE } // Added missing enum definition
    public enum FanSize {SMALL, MEDIUM, LARGE} // Added FanSize enum
    public enum FanColor {WHITE, BLACK}
    public enum Action {MORE_INFO, NAVIGATE, NONE, TOGGLE, TOGGLE_FAN}

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
    private static final String SETTING_NAME_ASSOCIATED_FAN_ENTITY_ID = "associatedFanEntityId";
    private static final String SETTING_NAME_FAN_COLOR = "fanColor";
    private static final String SETTING_NAME_SHOW_FAN_WHEN_OFF = "showFanWhenOff";
    private static final String SETTING_NAME_FAN_SIZE = "fanSize"; // Added FanSize setting constant
    private static final String SETTING_NAME_SHOW_BORDER_AND_BACKGROUND = "showBorderAndBackground";
    private static final String SETTING_NAME_LABEL_COLOR = "labelColor";
    private static final String SETTING_NAME_LABEL_TEXT_SHADOW = "labelTextShadow";
    private static final String SETTING_NAME_LABEL_FONT_WEIGHT = "labelFontWeight";
    private static final String SETTING_NAME_LABEL_SUFFIX = "labelSuffix";

    // --- Fields ---
    private List<? extends HomePieceOfFurniture> piecesOfFurniture;
    private String id;
    private String name;
    private String attribute;
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
    private String associatedFanEntityId;
    private FanColor fanColor;
    private boolean showFanWhenOff;
    private FanSize fanSize; // Added FanSize field
    private boolean showBorderAndBackground;
    private boolean alwaysOn;
    private boolean isRgb;
    private Map<HomeLight, Float> initialPower;
    private Settings settings;
    private boolean isUserDefinedPosition;
    private PropertyChangeSupport propertyChangeSupport;
    private double defaultIconBadgeBaseSizePercent;
    private String labelColor;
    private String labelTextShadow;
    private String labelFontWeight;
    private String labelSuffix;

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

    // Helper method to create unique setting keys
    private String getSettingKey(String settingSuffix) {
        // this.name is the HA entity name (e.g., light.living_room)
        // this.id is the SH3D piece ID (e.g., "obj123")
        return this.name + "_" + this.id + "." + settingSuffix;
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

    public String getAttribute() {
        return attribute;
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
        settings.set(getSettingKey(SETTING_NAME_DISPLAY_TYPE), displayType.name());
    }

    public boolean isDisplayTypeModified() {
        return settings.get(getSettingKey(SETTING_NAME_DISPLAY_TYPE)) != null;
    }
    
    public DisplayOperator getDisplayOperator() {
        return displayOperator;
    }

    public void setDisplayOperator(DisplayOperator displayOperator) {
        this.displayOperator = displayOperator;
        settings.set(getSettingKey(SETTING_NAME_DISPLAY_OPERATOR), displayOperator.name());
    }
    
    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
        settings.set(getSettingKey(SETTING_NAME_DISPLAY_VALUE), displayValue);
    }
    
    public boolean isDisplayConditionModified() {
        return settings.get(getSettingKey(SETTING_NAME_DISPLAY_OPERATOR)) != null
            || settings.get(getSettingKey(SETTING_NAME_DISPLAY_VALUE)) != null;
    }

    public DisplayOperator getFurnitureDisplayOperator() {
        return furnitureDisplayOperator;
    }

    public void setFurnitureDisplayOperator(DisplayOperator furnitureDisplayOperator) {
        this.furnitureDisplayOperator = furnitureDisplayOperator;
        settings.set(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_OPERATOR), furnitureDisplayOperator.name());
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
        return settings.get(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_OPERATOR)) != null
            || settings.get(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_VALUE)) != null;
    }

    public Action getTapAction() {
        return tapAction;
    }

    public void setTapAction(Action tapAction) {
        this.tapAction = tapAction;
        settings.set(getSettingKey(SETTING_NAME_TAP_ACTION), tapAction.name());
    }

    public boolean isTapActionModified() {
        return settings.get(getSettingKey(SETTING_NAME_TAP_ACTION)) != null;
    }

    public String getTapActionValue() {
        return tapActionValue;
    }

    public void setTapActionValue(String tapActionValue) {
        this.tapActionValue = tapActionValue;
        settings.set(getSettingKey(SETTING_NAME_TAP_ACTION_VALUE), tapActionValue);
    }

    public boolean isTapActionValueModified() {
        return settings.get(getSettingKey(SETTING_NAME_TAP_ACTION_VALUE)) != null;
    }

    public Action getDoubleTapAction() {
        return doubleTapAction;
    }

    public void setDoubleTapAction(Action doubleTapAction) {
        this.doubleTapAction = doubleTapAction;
        settings.set(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION), doubleTapAction.name());
    }

    public boolean isDoubleTapActionModified() {
        return settings.get(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION)) != null;
    }

    public String getDoubleTapActionValue() {
        return doubleTapActionValue;
    }

    public void setDoubleTapActionValue(String doubleTapActionValue) {
        this.doubleTapActionValue = doubleTapActionValue;
        settings.set(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION_VALUE), doubleTapActionValue);
    }

    public boolean isDoubleTapActionValueModified() {
        return settings.get(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION_VALUE)) != null;
    }

    public Action getHoldAction() {
        return holdAction;
    }

    public void setHoldAction(Action holdAction) {
        this.holdAction = holdAction;
        settings.set(getSettingKey(SETTING_NAME_HOLD_ACTION), holdAction.name());
    }

    public boolean isHoldActionModified() {
        return settings.get(getSettingKey(SETTING_NAME_HOLD_ACTION)) != null;
    }

    public String getHoldActionValue() {
        return holdActionValue;
    }

    public void setHoldActionValue(String holdActionValue) {
        this.holdActionValue = holdActionValue;
        settings.set(getSettingKey(SETTING_NAME_HOLD_ACTION_VALUE), holdActionValue);
    }

    public boolean isHoldActionValueModified() {
        return settings.get(getSettingKey(SETTING_NAME_HOLD_ACTION_VALUE)) != null;
    }

    public String getAssociatedFanEntityId() {
        return associatedFanEntityId;
    }

    public void setAssociatedFanEntityId(String associatedFanEntityId) {
        this.associatedFanEntityId = associatedFanEntityId;
        settings.set(getSettingKey(SETTING_NAME_ASSOCIATED_FAN_ENTITY_ID), associatedFanEntityId);
    }

    public boolean isAssociatedFanEntityIdModified() {
        return settings.get(getSettingKey(SETTING_NAME_ASSOCIATED_FAN_ENTITY_ID)) != null;
    }

    public boolean getShowFanWhenOff() {
        return showFanWhenOff;
    }

    public void setShowFanWhenOff(boolean showFanWhenOff) {
        this.showFanWhenOff = showFanWhenOff;
        settings.setBoolean(getSettingKey(SETTING_NAME_SHOW_FAN_WHEN_OFF), showFanWhenOff);
    }

    public boolean isShowFanWhenOffModified() {
        return settings.get(getSettingKey(SETTING_NAME_SHOW_FAN_WHEN_OFF)) != null;
    }

    public FanColor getFanColor() {
        return fanColor;
    }

    public void setFanColor(FanColor fanColor) {
        this.fanColor = fanColor;
        settings.set(getSettingKey(SETTING_NAME_FAN_COLOR), fanColor.name());
    }

    public boolean isFanColorModified() {
        return settings.get(name + "." + SETTING_NAME_FAN_COLOR) != null;
    }

    public boolean getShowBorderAndBackground() {
        return showBorderAndBackground;
    }

    public void setShowBorderAndBackground(boolean showBorderAndBackground) {
        this.showBorderAndBackground = showBorderAndBackground;
        settings.setBoolean(getSettingKey(SETTING_NAME_SHOW_BORDER_AND_BACKGROUND), showBorderAndBackground);
    }

    public boolean isShowBorderAndBackgroundModified() {
        return settings.get(getSettingKey(SETTING_NAME_SHOW_BORDER_AND_BACKGROUND)) != null;
    }

    public boolean getAlwaysOn() {
        return alwaysOn;
    }

    public void setAlwaysOn(boolean alwaysOn) {
        boolean oldAlwaysOn = this.alwaysOn;
        this.alwaysOn = alwaysOn;
        settings.setBoolean(getSettingKey(SETTING_NAME_ALWAYS_ON), alwaysOn);
        propertyChangeSupport.firePropertyChange(Property.ALWAYS_ON.name(), oldAlwaysOn, alwaysOn);
    }

    public boolean isAlwaysOnModified() {
        return settings.get(getSettingKey(SETTING_NAME_ALWAYS_ON)) != null;
    }

    public boolean getIsRgb() {
        return isRgb;
    }

    public void setIsRgb(boolean isRgb) {
        boolean oldIsRgb = this.isRgb;
        this.isRgb = isRgb;
        settings.setBoolean(getSettingKey(SETTING_NAME_IS_RGB), isRgb);
        propertyChangeSupport.firePropertyChange(Property.IS_RGB.name(), oldIsRgb, isRgb);
    }

    public boolean isIsRgbModified() {
        return settings.get(getSettingKey(SETTING_NAME_IS_RGB)) != null;
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

        settings.setDouble(getSettingKey(SETTING_NAME_LEFT_POSITION), position.x);
        settings.setDouble(getSettingKey(SETTING_NAME_TOP_POSITION), position.y);
        isUserDefinedPosition = true;
        propertyChangeSupport.firePropertyChange(Property.POSITION.name(), oldPosition, position);
    }

    public boolean isPositionModified() {
        return settings.get(getSettingKey(SETTING_NAME_LEFT_POSITION)) != null;
    }

    public boolean getBlinking() {
        return blinking;
    }

    public void setBlinking(boolean blinking) {
        this.blinking = blinking;
        settings.setBoolean(getSettingKey(SETTING_NAME_BLINKING), blinking);
    }

    public boolean isBlinkingModified() {
        return settings.get(getSettingKey(SETTING_NAME_BLINKING)) != null;
    }
    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
        settings.setInteger(getSettingKey(SETTING_NAME_OPACITY), opacity);
    }

    public boolean isOpacityModified() {
        return settings.get(getSettingKey(SETTING_NAME_OPACITY)) != null;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        double oldScaleFactor = this.scaleFactor;
        this.scaleFactor = scaleFactor;
        settings.setDouble(getSettingKey(SETTING_NAME_SCALE_FACTOR), scaleFactor);
        propertyChangeSupport.firePropertyChange(Property.SCALE_FACTOR.name(), oldScaleFactor, scaleFactor);
    }

    public boolean isScaleFactorModified() {
        return settings.get(getSettingKey(SETTING_NAME_SCALE_FACTOR)) != null;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        settings.set(getSettingKey(SETTING_NAME_BACKGROUND_COLOR), backgroundColor);
    }

    public boolean isBackgroundColorModified() {
        return settings.get(getSettingKey(SETTING_NAME_BACKGROUND_COLOR)) != null;
    }

    public ClickableAreaType getClickableAreaType() {
        return clickableAreaType;
    }

    public void setClickableAreaType(ClickableAreaType clickableAreaType) {
        this.clickableAreaType = clickableAreaType;
        settings.set(getSettingKey(SETTING_NAME_CLICKABLE_AREA_TYPE), clickableAreaType.name());
    }

    public boolean isClickableAreaTypeModified() {
        return settings.get(getSettingKey(SETTING_NAME_CLICKABLE_AREA_TYPE)) != null;
    }

    public FanSize getFanSize() {
        return fanSize;
    }

    public void setFanSize(FanSize fanSize) {
        this.fanSize = fanSize;
        settings.set(getSettingKey(SETTING_NAME_FAN_SIZE), fanSize.name());
    }

    public boolean isFanSizeModified() {
        return settings.get(getSettingKey(SETTING_NAME_FAN_SIZE)) != null;
    }


    public String getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(String labelColor) {
        this.labelColor = labelColor;
        settings.set(getSettingKey(SETTING_NAME_LABEL_COLOR), labelColor);
    }

    public boolean isLabelColorModified() {
        return settings.get(getSettingKey(SETTING_NAME_LABEL_COLOR)) != null;
    }

    public String getLabelTextShadow() {
        return labelTextShadow;
    }

    public void setLabelTextShadow(String labelTextShadow) {
        this.labelTextShadow = labelTextShadow;
        settings.set(getSettingKey(SETTING_NAME_LABEL_TEXT_SHADOW), labelTextShadow);
    }

    public boolean isLabelTextShadowModified() {
        return settings.get(getSettingKey(SETTING_NAME_LABEL_TEXT_SHADOW)) != null;
    }

    public String getLabelFontWeight() {
        return labelFontWeight;
    }

    public void setLabelFontWeight(String labelFontWeight) {
        this.labelFontWeight = labelFontWeight;
        settings.set(getSettingKey(SETTING_NAME_LABEL_FONT_WEIGHT), labelFontWeight);
    }

    public boolean isLabelFontWeightModified() {
        return settings.get(getSettingKey(SETTING_NAME_LABEL_FONT_WEIGHT)) != null;
    }

    public String getLabelSuffix() {
        return labelSuffix;
    }

    public void setLabelSuffix(String labelSuffix) {
        this.labelSuffix = labelSuffix;
        settings.set(getSettingKey(SETTING_NAME_LABEL_SUFFIX), labelSuffix);
    }

    public boolean isLabelSuffixModified() {
        return settings.get(getSettingKey(SETTING_NAME_LABEL_SUFFIX)) != null;
    }

    public void resetToDefaults() {
        boolean oldAlwaysOn = alwaysOn;
        boolean oldIsRgb = isRgb;
        Point2d oldPosition = getPosition();

        double oldScaleFactor = scaleFactor; // Store old scaleFactor

        settings.set(getSettingKey(SETTING_NAME_DISPLAY_TYPE), null);
        settings.set(getSettingKey(SETTING_NAME_DISPLAY_OPERATOR), null);
        settings.set(getSettingKey(SETTING_NAME_DISPLAY_VALUE), null);
        settings.set(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_OPERATOR), null);
        settings.set(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_VALUE), null);
        settings.set(getSettingKey(SETTING_NAME_TAP_ACTION), null);
        settings.set(getSettingKey(SETTING_NAME_TAP_ACTION_VALUE), null);
        settings.set(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION), null);
        settings.set(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION_VALUE), null);
        settings.set(getSettingKey(SETTING_NAME_HOLD_ACTION), null);
        settings.set(getSettingKey(SETTING_NAME_HOLD_ACTION_VALUE), null);
        settings.set(getSettingKey(SETTING_NAME_ALWAYS_ON), null);
        settings.set(getSettingKey(SETTING_NAME_IS_RGB), null);
        settings.set(getSettingKey(SETTING_NAME_LEFT_POSITION), null);
        settings.set(getSettingKey(SETTING_NAME_TOP_POSITION), null);
        settings.set(getSettingKey(SETTING_NAME_BLINKING), null);
        settings.set(getSettingKey(SETTING_NAME_OPACITY), null);
        settings.set(getSettingKey(SETTING_NAME_BACKGROUND_COLOR), null);
        settings.set(getSettingKey(SETTING_NAME_SCALE_FACTOR), null);
        settings.set(getSettingKey(SETTING_NAME_CLICKABLE_AREA_TYPE), null); // Reset clickable area type
        settings.set(getSettingKey(SETTING_NAME_ASSOCIATED_FAN_ENTITY_ID), null);
        settings.set(getSettingKey(SETTING_NAME_SHOW_FAN_WHEN_OFF), null);
        settings.set(getSettingKey(SETTING_NAME_FAN_COLOR), null);
        settings.set(getSettingKey(SETTING_NAME_FAN_SIZE), null); // Reset FanSize
        settings.set(getSettingKey(SETTING_NAME_SHOW_BORDER_AND_BACKGROUND), null);
        settings.set(getSettingKey(SETTING_NAME_LABEL_COLOR), null);
        settings.set(getSettingKey(SETTING_NAME_LABEL_TEXT_SHADOW), null);
        settings.set(getSettingKey(SETTING_NAME_LABEL_FONT_WEIGHT), null);
        settings.set(getSettingKey(SETTING_NAME_LABEL_SUFFIX), null);
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

    private String actionYaml(Action action, String value, String fanEntityId) {
        final Map<Action, String> actionToYamlString = new HashMap<Action, String>() {{
            put(Action.MORE_INFO, "more-info");
            put(Action.NAVIGATE, "navigate");
            put(Action.NONE, "none");
            put(Action.TOGGLE, "toggle");
            put(Action.TOGGLE_FAN, "call-service");
        }};

        String yaml = actionToYamlString.get(action);

        if (action == Action.NAVIGATE)
            yaml += String.format("\n" +
                "        navigation_path: %s", value);
        else if (action == Action.TOGGLE_FAN) {
            yaml += String.format("\n" +
                "      service: fan.toggle\n" +
                "      target:\n" +
                "        entity_id: %s", fanEntityId != null ? fanEntityId : "");
        }

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

        String elementYaml;

        if (displayType == DisplayType.ICON_AND_ANIMATED_FAN) {
            DisplayType iconPartDisplayType = DisplayType.ICON; // Default to ICON for the icon part

            String iconYamlString = displayTypeToYamlString.get(iconPartDisplayType);
            if (iconYamlString == null) iconYamlString = "state-icon"; // Fallback

            StringBuilder iconStyleProperties = new StringBuilder();
            iconStyleProperties.append(String.format(Locale.US, "      top: %.2f%%\n", position.y));
            iconStyleProperties.append(String.format(Locale.US, "      left: %.2f%%\n", position.x));
            // For ICON_AND_ANIMATED_FAN, the icon part might not always need a circular background if the fan is the main visual.
            if (this.showBorderAndBackground) {
                iconStyleProperties.append("      border-radius: 50%\n"); 
                iconStyleProperties.append(String.format(Locale.US, "      background-color: %s\n", backgroundColor));
            }
            iconStyleProperties.append("      text-align: center\n"); // Keep for potential text in future icon types
            
            String iconVisualElementTransform = "translate(-50%, -50%)";
            if (blinking) {
                iconStyleProperties.append("      animation: my-blink 1s linear infinite\n");
            } else {
                iconStyleProperties.append(String.format(Locale.US, "      opacity: %d%%\n", opacity));
            }

            // For ICON_AND_ANIMATED_FAN, let HA size the icon, apply scaleFactor via transform
            // No explicit width/height for the icon part
            if (Math.abs(scaleFactor - 1.0) > 0.001) { // Only add scale if not 1.0
                iconVisualElementTransform += String.format(Locale.US, " scale(%.2f)", scaleFactor);
            }
            iconStyleProperties.append(String.format(Locale.US, "      transform: %s\n", iconVisualElementTransform)); // No quotes, no semicolon

            
            if (clickableAreaType == ClickableAreaType.ROOM_SIZE) {
                 iconStyleProperties.append("      pointer-events: none;\n");
            }

            String iconElementYaml = String.format(Locale.US,
                "  - type: %s\n" +
                "    entity: %s\n" +
                (this.attribute != null && !this.attribute.isEmpty() ? "    attribute: " + this.attribute + "\n" : "") +
                (iconPartDisplayType == DisplayType.BADGE ? "" : "    title: " + (title != null ? title : "null") + "\n") +
                "    style:\n" +
                "%s" +
                "    tap_action:\n" +
                "      action: %s\n" +
                "    double_tap_action:\n" +
                "      action: %s\n" +
                "    hold_action:\n" +
                "      action: %s\n",
                iconYamlString, name,
                iconStyleProperties.toString(),
                actionYaml(tapAction, tapActionValue, this.associatedFanEntityId),
                actionYaml(doubleTapAction, doubleTapActionValue, this.associatedFanEntityId),
                actionYaml(holdAction, holdActionValue, this.associatedFanEntityId));

            String fanImageOn;
            String fanImageOffSuffix;
            if (this.fanColor == FanColor.WHITE) {
                fanImageOn = "/local/floorplan/animated_fan_grey.gif";
                fanImageOffSuffix = "/local/floorplan/animated_fan_still_grey.gif";
            } else { // BLACK or default
                fanImageOn = "/local/floorplan/animated_fan.gif";
                fanImageOffSuffix = "/local/floorplan/animated_fan_still.gif";
            }
            String fanImageOff = this.showFanWhenOff ? fanImageOffSuffix : "/local/floorplan/transparent.png";

            StringBuilder fanStyleProperties = new StringBuilder();
            fanStyleProperties.append(String.format(Locale.US, "      top: %.2f%%\n", position.y));
            fanStyleProperties.append(String.format(Locale.US, "      left: %.2f%%\n", position.x));
            // Calculate fan dimensions based on a 2:3 width:height aspect ratio
            // Use FanSize to determine dimensions
            double fanWidthPercent;
            double fanHeightPercent;
            switch (this.fanSize) {
                case SMALL: fanWidthPercent = 2.0; fanHeightPercent = 3.0; break;
                case MEDIUM: fanWidthPercent = 4.0; fanHeightPercent = 5.0; break;
                case LARGE: fanWidthPercent = 6.0; fanHeightPercent = 7.0; break;
                default: fanWidthPercent = 4.0; fanHeightPercent = 5.0; break; // Default to Medium
            }
            // Apply scaleFactor to the chosen size
            fanWidthPercent *= scaleFactor;
            fanHeightPercent *= scaleFactor;
            fanStyleProperties.append(String.format(Locale.US, "      width: %.2f%%\n", fanWidthPercent)); // Use calculated width
            fanStyleProperties.append(String.format(Locale.US, "      height: %.2f%%\n", fanHeightPercent));
            fanStyleProperties.append("      transform: translate(-50%, -50%);\n");
            fanStyleProperties.append("      pointer-events: none;\n");

            String fanImageElementYaml = "";
            if (this.associatedFanEntityId != null && !this.associatedFanEntityId.trim().isEmpty()) {
                fanImageElementYaml = String.format(Locale.US,
                    "  - type: image\n" +
                    "    entity: %s\n" +
                    "    title: null\n" +
                    "    state_image:\n" +
                    "      \"on\": \"%s\"\n" +
                    "      \"off\": \"%s\"\n" +
                    "    style:\n" +
                    "%s",
                    this.associatedFanEntityId,
                    fanImageOn,
                    fanImageOff,
                    fanStyleProperties.toString());
            }
            // The fan image should come before the icon in YAML for better layering
            // (icon on top of fan) and to address the request of having the
            // non-interactive image part appear before the interactive icon part.
            elementYaml = fanImageElementYaml + iconElementYaml;
        } else {
            StringBuilder styleProperties = new StringBuilder();
            styleProperties.append(String.format(Locale.US, "      top: %.2f%%\n", position.y));
            styleProperties.append(String.format(Locale.US, "      left: %.2f%%\n", position.x));
            String visualElementTransform = "translate(-50%, -50%)";
            if (this.showBorderAndBackground && (displayType == DisplayType.ICON || displayType == DisplayType.BADGE)) {
                styleProperties.append("      border-radius: 50%\n");
                styleProperties.append(String.format(Locale.US, "      background-color: %s\n", backgroundColor));
            }
            if (displayType == DisplayType.LABEL || displayType == DisplayType.BADGE) { // Badges can also have text
                styleProperties.append("      text-align: center\n");
            } // Note: background-color for LABEL might not be desired unless showBorderAndBackground is true.

            if (blinking) {
                styleProperties.append("      animation: my-blink 1s linear infinite\n");
            } else {
                 styleProperties.append(String.format(Locale.US, "      opacity: %d%%\n", opacity));
            }

            // For ICON and BADGE, let HA size the element, apply scaleFactor via transform
            // For LABEL, apply scaleFactor via transform and don't set explicit width/height
            if (Math.abs(scaleFactor - 1.0) > 0.001) { // Only add scale if not 1.0
                visualElementTransform += String.format(Locale.US, " scale(%.2f)", scaleFactor);
                styleProperties.append(String.format(Locale.US, "      transform: %s\n", visualElementTransform)); // No quotes, no semicolon
            } else if (displayType == DisplayType.ICON || displayType == DisplayType.BADGE || displayType == DisplayType.LABEL) {
                // Ensure transform is always present for centering, even if scale is 1.0
                styleProperties.append(String.format(Locale.US, "      transform: %s\n", visualElementTransform)); // No quotes, no semicolon
            }

            if (displayType == DisplayType.LABEL) {
                if (labelColor != null && !labelColor.trim().isEmpty()) {
                    styleProperties.append(String.format(Locale.US, "      color: %s\n", labelColor));
                }
                if (labelTextShadow != null && !labelTextShadow.trim().isEmpty()) {
                    styleProperties.append(String.format(Locale.US, "      text-shadow: 1px 1px 1px %s\n", labelTextShadow));
                }
                if (labelFontWeight != null && !labelFontWeight.trim().isEmpty()) {
                    styleProperties.append(String.format(Locale.US, "      font-weight: %s\n", labelFontWeight));
                }
            }
             if (clickableAreaType == ClickableAreaType.ROOM_SIZE && displayType != DisplayType.ICON_AND_ANIMATED_FAN) { // visual element non-clickable unless it's the room itself
                styleProperties.append("      pointer-events: none;\n");
            }

            // Prepare conditional parts as arguments for String.format
            // These will be substituted into %s placeholders.
            String attributeString = (this.attribute != null && !this.attribute.isEmpty())
                                   ? String.format("    attribute: %s\n", this.attribute) : "";

            String suffixString = "";
            if (displayType == DisplayType.LABEL) {
                if (labelSuffix != null && !labelSuffix.trim().isEmpty()) {
                    // For the inner String.format("suffix: '%s'", ...), if labelSuffix contains a literal '%'
                    // it should be escaped as '%%' if it were part of the format string.
                    // However, as an argument to %s, it's usually literal.
                    // The .replace("'", "''") is for YAML single-quoted strings containing single quotes.
                    suffixString = String.format("    suffix: '%s'\n", labelSuffix.replace("'", "''"));
                } else if (this.attribute != null && !this.attribute.isEmpty()) {
                    // Default suffix when attribute is present and no custom suffix
                    suffixString = "    suffix: '°'\n"; // Literal single quotes are fine here
                }
            }

            String titleString = (displayType == DisplayType.BADGE) ? ""
                               : String.format("    title: %s\n", (title != null ? title : "null"));

            elementYaml = String.format(Locale.US,
                "  - type: %s\n" +
                "    entity: %s\n" +
                "%s" + // attributeString
                "%s" + // suffixString
                "%s" + // titleString
                "    style:\n" +
                "%s" +
                "    tap_action:\n" +
                "      action: %s\n" +
                "    double_tap_action:\n" +
                "      action: %s\n" +
                "    hold_action:\n" +
                "      action: %s\n",
                displayTypeToYamlString.get(displayType), name,
                attributeString, suffixString, titleString, // Pass pre-formatted strings as arguments
                styleProperties.toString(),
                actionYaml(tapAction, tapActionValue, this.associatedFanEntityId),
                actionYaml(doubleTapAction, doubleTapActionValue, this.associatedFanEntityId),
                actionYaml(holdAction, holdActionValue, this.associatedFanEntityId));
        }

        String clickableAreaYaml = "";
        if (clickableAreaType == ClickableAreaType.ROOM_SIZE && controller != null) {
            Map<String, Double> roomBounds = controller.getRoomBoundingBoxPercent(this);
            if (roomBounds != null) {
                System.out.println(String.format(Locale.US, 
                    "DEBUG Entity.java: For entity '%s', Icon Center (L:%.2f%%, T:%.2f%%). Controller-provided (shrunken) bounds: (L:%.2f%%, T:%.2f%%, W:%.2f%%, H:%.2f%%)",
                    this.name, this.position.x, this.position.y,
                    roomBounds.get("left"), roomBounds.get("top"), roomBounds.get("width"), roomBounds.get("height")
                ));
                // Check if the icon's center is within the calculated room bounds
                double iconCenterY = this.position.y;
                double iconCenterX = this.position.x;

                double roomT = roomBounds.get("top");
                double roomL = roomBounds.get("left");
                double roomW = roomBounds.get("width");
                double roomH = roomBounds.get("height");
                double roomR = roomL + roomW;
                double roomB = roomT + roomH;

                boolean iconCenterIsInside =
                    iconCenterX >= roomL && iconCenterX <= roomR &&
                    iconCenterY >= roomT && iconCenterY <= roomB;

                if (!iconCenterIsInside) {
                    System.err.println(String.format(Locale.US,
                        "Entity.java Warning: Icon center for entity '%s' (at L:%.2f%%, T:%.2f%%) is outside its calculated room's clickable area (L:%.2f%%, T:%.2f%%, W:%.2f%%, H:%.2f%%). Adjusting clickable area to include icon center.",
                        this.name, iconCenterX, iconCenterY, roomL, roomT, roomW, roomH
                    ));

                    // Expand roomBounds to include the icon's center
                    double newTop = Math.min(roomT, iconCenterY);
                    double newLeft = Math.min(roomL, iconCenterX);
                    double newRight = Math.max(roomR, iconCenterX);
                    double newBottom = Math.max(roomB, iconCenterY);

                    // Update the map directly
                    roomBounds.put("top", newTop);
                    roomBounds.put("left", newLeft);
                    roomBounds.put("width", Math.max(0, newRight - newLeft));   // Ensure non-negative
                    roomBounds.put("height", Math.max(0, newBottom - newTop)); // Ensure non-negative
                }

                // Calculate dimensions for the transparent PNG based on aspect ratio
                double roomWidthPercent = roomBounds.get("width");
                double roomHeightPercent = roomBounds.get("height");

                final int BASE_PNG_DIMENSION = 20; // Base size for the longer side of the PNG (in pixels)
                int pngWidthPx;
                int pngHeightPx;

                if (roomWidthPercent <= 0 && roomHeightPercent <= 0) { // Handles zero or negative dimensions
                    pngWidthPx = 1;
                    pngHeightPx = 1;
                } else if (roomWidthPercent >= roomHeightPercent) {
                    pngWidthPx = BASE_PNG_DIMENSION;
                    pngHeightPx = (int) Math.round(BASE_PNG_DIMENSION * (roomHeightPercent / Math.max(0.001, roomWidthPercent)));
                } else {
                    pngHeightPx = BASE_PNG_DIMENSION;
                    pngWidthPx = (int) Math.round(BASE_PNG_DIMENSION * (roomWidthPercent / Math.max(0.001, roomHeightPercent)));
                }

                // Ensure minimum 1x1 pixel dimension
                pngWidthPx = Math.max(1, pngWidthPx);
                pngHeightPx = Math.max(1, pngHeightPx);

                String transparentImageBaseName = "transparent_" + this.name;
                try {
                    controller.ensureEntityTransparentImageGenerated(this.name, pngWidthPx, pngHeightPx);
                    String transparentImageHash = controller.renderHash(transparentImageBaseName, true);
                    String transparentImagePath = "/local/floorplan/" + transparentImageBaseName + ".png?version=" + transparentImageHash;

                    clickableAreaYaml = String.format(Locale.US,
                        "  - type: image\n" +
                        "    entity: %s\n" +
                        "    image: %s\n" +
                        "    tap_action:\n" +
                        "      action: %s\n" +
                        "    double_tap_action:\n" +
                        "      action: %s\n" +
                        "    hold_action:\n" +
                        "      action: %s\n" +
                        "    style:\n" +
                        "      top: %.2f%%\n" +
                        "      left: %.2f%%\n" +
                        "      width: %.2f%%\n" +  // Use original percentage from roomBounds
                        "      height: %.2f%%\n" + // Use original percentage from roomBounds
                        "      transform: translate(0%%, 0%%)\n" +
                        "      opacity: 0%%\n",
                        this.name,
                        transparentImagePath,
                        actionYaml(tapAction, tapActionValue, this.associatedFanEntityId),
                        actionYaml(doubleTapAction, doubleTapActionValue, this.associatedFanEntityId),
                        actionYaml(holdAction, holdActionValue, this.associatedFanEntityId),
                        roomBounds.get("top"), roomBounds.get("left"), roomWidthPercent, roomHeightPercent);
                } catch (IOException e) {
                    System.err.println("Error generating/hashing transparent image for " + this.name + " with dimensions " + pngWidthPx + "x" + pngHeightPx + ": " + e.getMessage());
                }
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

        String conditionAttributePart = (this.attribute != null && !this.attribute.isEmpty())
                                        ? String.format("        attribute: %s\n", this.attribute)
                                        : "";

        String conditionYaml;
        switch (this.displayOperator) {
            case IS:
                conditionYaml = String.format(
                    "    conditions:\n" + // Ensure conditions block starts here
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    conditionAttributePart +
                    "        state: '%s'",
                    name, this.displayValue);
                break;
            case IS_NOT:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: state\n" +
                    "        entity: %s\n" +
                    conditionAttributePart +
                    "        state_not: '%s'",
                    name, this.displayValue);
                break;
            case GREATER_THAN:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    conditionAttributePart +
                    "        above: %s",
                    name, this.displayValue);
                break;
            case LESS_THAN:
                conditionYaml = String.format(
                    "    conditions:\n" +
                    "      - condition: numeric_state\n" +
                    "        entity: %s\n" +
                    conditionAttributePart +
                    "        below: %s",
                    name, this.displayValue);
                break;
            default:
                // Fallback for unhandled or ALWAYS/NEVER if logic changes (should be caught above)
                // If no specific operator condition, but we have an attribute, the elementYaml already includes it.
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

        String rawPieceName = firstPiece.getName();
        if (rawPieceName != null && rawPieceName.contains("/")) {
            int slashIndex = rawPieceName.indexOf('/');
            this.name = rawPieceName.substring(0, slashIndex);
            this.attribute = rawPieceName.substring(slashIndex + 1);
        } else {
            this.name = rawPieceName;
            this.attribute = null;
        }
        position = loadPosition();
        displayType = getSavedEnumValue(DisplayType.class, getSettingKey(SETTING_NAME_DISPLAY_TYPE), defaultDisplayType());
        
        displayOperator = getSavedEnumValue(DisplayOperator.class, getSettingKey(SETTING_NAME_DISPLAY_OPERATOR), DisplayOperator.ALWAYS);
        displayValue = settings.get(getSettingKey(SETTING_NAME_DISPLAY_VALUE), "");
        
        furnitureDisplayOperator = getSavedEnumValue(DisplayOperator.class, getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_OPERATOR), DisplayOperator.ALWAYS);
        // --- MODIFIED: Default to an empty string to prevent OutOfMemoryError ---
        furnitureDisplayValue = settings.get(getSettingKey(SETTING_NAME_FURNITURE_DISPLAY_VALUE), "");
        clickableAreaType = getSavedEnumValue(ClickableAreaType.class, getSettingKey(SETTING_NAME_CLICKABLE_AREA_TYPE), ClickableAreaType.ENTITY_SIZE);

        tapAction = getSavedEnumValue(Action.class, getSettingKey(SETTING_NAME_TAP_ACTION), defaultAction());
        tapActionValue = settings.get(getSettingKey(SETTING_NAME_TAP_ACTION_VALUE), "");
        doubleTapAction = getSavedEnumValue(Action.class, getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION), Action.NONE);
        doubleTapActionValue = settings.get(getSettingKey(SETTING_NAME_DOUBLE_TAP_ACTION_VALUE), "");
        holdAction = getSavedEnumValue(Action.class, getSettingKey(SETTING_NAME_HOLD_ACTION), Action.MORE_INFO);
        holdActionValue = settings.get(getSettingKey(SETTING_NAME_HOLD_ACTION_VALUE), "");
        blinking = settings.getBoolean(getSettingKey(SETTING_NAME_BLINKING), false);
        title = firstPiece.getDescription();
        opacity = settings.getInteger(getSettingKey(SETTING_NAME_OPACITY), 100);
        backgroundColor = settings.get(getSettingKey(SETTING_NAME_BACKGROUND_COLOR), "rgba(255, 255, 255, 0.3)");
        scaleFactor = settings.getDouble(getSettingKey(SETTING_NAME_SCALE_FACTOR), 1.0);
        alwaysOn = settings.getBoolean(getSettingKey(SETTING_NAME_ALWAYS_ON), false);
        associatedFanEntityId = settings.get(getSettingKey(SETTING_NAME_ASSOCIATED_FAN_ENTITY_ID), "");
        fanColor = getSavedEnumValue(FanColor.class, getSettingKey(SETTING_NAME_FAN_COLOR), FanColor.BLACK);
        showFanWhenOff = settings.getBoolean(getSettingKey(SETTING_NAME_SHOW_FAN_WHEN_OFF), true);
        fanSize = getSavedEnumValue(FanSize.class, getSettingKey(SETTING_NAME_FAN_SIZE), FanSize.MEDIUM); // Load FanSize, default to Medium
        showBorderAndBackground = settings.getBoolean(getSettingKey(SETTING_NAME_SHOW_BORDER_AND_BACKGROUND), true);
        labelColor = settings.get(getSettingKey(SETTING_NAME_LABEL_COLOR), "black"); // Default to "black"
        labelTextShadow = settings.get(getSettingKey(SETTING_NAME_LABEL_TEXT_SHADOW), "");
        labelFontWeight = settings.get(getSettingKey(SETTING_NAME_LABEL_FONT_WEIGHT), "normal"); // Default to "normal"
        labelSuffix = settings.get(getSettingKey(SETTING_NAME_LABEL_SUFFIX), "");

        isRgb = settings.getBoolean(getSettingKey(SETTING_NAME_IS_RGB), false);
        
        // Determine if this Entity represents a light based on its HA name or if any associated SH3D piece is a HomeLight
        boolean hasAnySh3dLightPiece = false;
        if (piecesOfFurniture != null) { // piecesOfFurniture should not be null here
            for (HomePieceOfFurniture pof : piecesOfFurniture) {
                if (pof instanceof HomeLight) {
                    hasAnySh3dLightPiece = true;
                    break;
                }
            }
        }
        this.isLight = (name != null && (name.startsWith("light.") || name.startsWith("switch."))) || hasAnySh3dLightPiece;
        saveInitialLightPowerValues();

        // Apply specific defaults for binary_sensor entities
        // These will apply if no user-saved setting exists for these properties,
        // or when resetToDefaults() is called.
        if (name != null && name.startsWith("binary_sensor.") && name.contains("motion")) {
            this.displayType = DisplayType.ICON;
            this.displayOperator = DisplayOperator.IS;
            this.displayValue = "on"; // Common 'active' state for binary_sensors
            this.tapAction = Action.NONE;
            this.doubleTapAction = Action.NONE; // Already the general default, but explicit
            this.holdAction = Action.NONE;
            this.blinking = true;
            // Other defaults like opacity, scaleFactor, clickableAreaType, showBorderAndBackground, furnitureDisplayOperator are generally fine.
        }
    }

    private DisplayType defaultDisplayType() {
        if (name.startsWith("fan.")) {
            return DisplayType.ICON_AND_ANIMATED_FAN;
        } else {
            return name.startsWith("sensor.") ? DisplayType.LABEL : DisplayType.ICON;
        }
    }

    public int compareTo(Entity other) {
        int nameCompare = getName().compareTo(other.getName());
        if (nameCompare == 0) {
            return getId().compareTo(other.getId()); // getId() is the sh3dPieceId
        }
        return nameCompare;
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
        double leftPosition = settings.getDouble(getSettingKey(SETTING_NAME_LEFT_POSITION), -1);
        double topPosition = settings.getDouble(getSettingKey(SETTING_NAME_TOP_POSITION), -1);
        if (leftPosition != -1 || topPosition != -1) {
            isUserDefinedPosition = true;
            return new Point2d(leftPosition, topPosition);
        }

        isUserDefinedPosition = false;
        return new Point2d(0, 0);
    }
}
