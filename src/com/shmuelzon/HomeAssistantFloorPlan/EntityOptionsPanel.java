package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.Map; // Added for bounds map
import java.util.ResourceBundle;

import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import com.eteks.sweethome3d.model.Room; 
import javax.vecmath.Point2d;

import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.AutoCommitSpinner;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;

public class EntityOptionsPanel extends JPanel {
    private enum ActionType {CLOSE, RESET_TO_DEFAULTS}

    private Entity entity;
    private Controller controller; // Keep controller reference
    private JLabel displayTypeLabel;
    private JComboBox<Entity.DisplayType> displayTypeComboBox;
    
    // --- NEW: Components for the operator and value ---
    private JLabel displayStateLabel;
    private JComboBox<Entity.DisplayOperator> displayOperatorComboBox;
    private JComboBox<String> displayValueComboBox;

    private JLabel tapActionLabel;
    private JComboBox<Entity.Action> tapActionComboBox;
    private JTextField tapActionValueTextField;
    private JLabel doubleTapActionLabel;
    private JComboBox<Entity.Action> doubleTapActionComboBox;
    private JTextField doubleTapActionValueTextField;
    private JLabel holdActionLabel;
    private JComboBox<Entity.Action> holdActionComboBox;
    private JTextField holdActionValueTextField;
    private JLabel positionLabel;
    private JLabel positionLeftLabel;
    private JSpinner positionLeftSpinner;
    private JLabel positionTopLabel;
    private JSpinner positionTopSpinner;
    private JLabel opacityLabel;
    private JSpinner opacitySpinner;
    private JLabel scaleFactorLabel;
    private JSpinner scaleFactorSpinner;
    private JLabel blinkingLabel;
    private JCheckBox blinkingCheckbox;
    private JLabel backgroundColorLabel;
    private JTextField backgroundColorTextField;
    private JLabel alwaysOnLabel;
    private JCheckBox alwaysOnCheckbox;
    private JLabel isRgbLabel;
    private JCheckBox isRgbCheckbox;
    private JLabel clickableAreaTypeLabel;
    private JComboBox<Entity.ClickableAreaType> clickableAreaTypeComboBox;

    // --- NEW: Components for the furniture operator and value ---
    private JLabel furnitureDisplayStateLabel;
    private JComboBox<Entity.DisplayOperator> furnitureDisplayOperatorComboBox;
    private JComboBox<String> furnitureDisplayValueComboBox;

    // --- NEW: Components for ICON_AND_ANIMATED_FAN ---
    private JLabel associatedFanEntityIdLabel;
    private JComboBox<String> associatedFanEntityIdComboBox;
    private JLabel showFanWhenOffLabel;
    private JCheckBox showFanWhenOffCheckbox;
    private JLabel fanColorLabel;
    private JComboBox<Entity.FanColor> fanColorComboBox;
    private JLabel fanSizeLabel; // Added Fan Size Label
    private JComboBox<Entity.FanSize> fanSizeComboBox; // Added Fan Size ComboBox
    private JLabel showBorderAndBackgroundLabel;
    private JCheckBox showBorderAndBackgroundCheckbox;

    // --- NEW: Components for state-label specific options ---
    private JLabel labelColorLabel;
    private JComboBox<String> labelColorComboBox; // Changed from JTextField
    private JLabel labelTextShadowLabel;
    private JComboBox<String> labelTextShadowComboBox; // Changed from JTextField
    private JLabel labelFontWeightLabel;
    private JComboBox<String> labelFontWeightComboBox; // Changed from JTextField
    private JLabel labelSuffixLabel;
    private JComboBox<String> labelSuffixComboBox; // Changed from JTextField to editable JComboBox
    private JButton closeButton;
    private JButton resetToDefaultsButton;
    private ResourceBundle resource;
    private boolean isProgrammaticClickableAreaChange = false;

    // --- Constants for ComboBox population, moved to class level for wider access ---
    private static final String[] LABEL_COLOR_KEYS = {"BLACK", "WHITE", "RED", "BLUE", "GREEN", "YELLOW", "GRAY", "ORANGE", "PURPLE"}; // No "NONE"
    private static final String[] FONT_WEIGHT_KEYS = {"NORMAL", "BOLD"}; // Simplified
    private static final String DEGREE_SYMBOL_KEY = "DEGREE";

    public EntityOptionsPanel(UserPreferences preferences, Entity entity, Controller controller) {
        super(new GridBagLayout());
        this.entity = entity;
        this.controller = controller;

        resource = controller.getResourceBundle(); // Use controller's resource bundle
        createActions(preferences);
        createComponents();
        layoutComponents();
        markModified();
        showHideComponents(); // Ensure this is called to set initial visibility of action value fields
        updateValueComboBoxesEnabledState(); // Set initial state
    }

    private void createActions(UserPreferences preferences) {
        final ActionMap actions = getActionMap();
        actions.put(ActionType.CLOSE, new ResourceAction(preferences, Panel.class, ActionType.CLOSE.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                close();
            }
        });
        actions.put(ActionType.RESET_TO_DEFAULTS, new ResourceAction(preferences, Panel.class, ActionType.RESET_TO_DEFAULTS.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                entity.resetToDefaults();
                // After reset, update UI to reflect new entity state
                displayTypeComboBox.setSelectedItem(entity.getDisplayType());
                displayOperatorComboBox.setSelectedItem(entity.getDisplayOperator());
                displayValueComboBox.setSelectedItem(entity.getDisplayValue());
                scaleFactorSpinner.setValue(entity.getScaleFactor());
                blinkingCheckbox.setSelected(entity.getBlinking());
                associatedFanEntityIdComboBox.setSelectedItem(entity.getAssociatedFanEntityId());
                showFanWhenOffCheckbox.setSelected(entity.getShowFanWhenOff());
                fanColorComboBox.setSelectedItem(entity.getFanColor());
                fanSizeComboBox.setSelectedItem(entity.getFanSize()); // Update Fan Size ComboBox
                showBorderAndBackgroundCheckbox.setSelected(entity.getShowBorderAndBackground());
                setComboBoxSelectionFromEntityValue(labelColorComboBox, entity.getLabelColor(), LABEL_COLOR_KEYS, "HomeAssistantFloorPlan.Panel.labelColorComboBox.%s.text", LABEL_COLOR_KEYS[0]);
                labelTextShadowComboBox.setSelectedItem(entity.getLabelTextShadow()); // Update ComboBox
                setComboBoxSelectionFromEntityValue(labelFontWeightComboBox, entity.getLabelFontWeight(), FONT_WEIGHT_KEYS, "HomeAssistantFloorPlan.Panel.labelFontWeightComboBox.%s.text", "NORMAL");
                labelSuffixComboBox.setSelectedItem(entity.getLabelSuffix());
                clickableAreaTypeComboBox.setSelectedItem(entity.getClickableAreaType()); // Update new combo box
                // ... update other components as needed ...
                markModified(); // Reflect that fields are now at default (potentially "unmodified")
                updateValueComboBoxesEnabledState(); // Re-evaluate enabled states
                showHideComponents(); // Re-evaluate visibility
                close(); // Now close
            }
        });
    }

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        displayTypeLabel = new JLabel();
        displayTypeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayTypeLabel.text"));
        displayTypeComboBox = new JComboBox<Entity.DisplayType>(Entity.DisplayType.values());
        displayTypeComboBox.setSelectedItem(entity.getDisplayType());
        // Make it look like an editable combo box but prevent typing
        displayTypeComboBox.setEditable(true);
        JTextField displayTypeEditor = (JTextField) displayTypeComboBox.getEditor().getEditorComponent();
        displayTypeEditor.setEditable(false);
        displayTypeEditor.setFocusable(false); // Optional: prevent focus
        // Set initial editor text to the localized string
        if (entity.getDisplayType() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", entity.getDisplayType().name()));
            displayTypeEditor.setText(initialDisplayText);
        }
        displayTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", ((Entity.DisplayType)o).name())));
                return rendererComponent;
            }
        });
        displayTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                entity.setDisplayType((Entity.DisplayType)displayTypeComboBox.getSelectedItem());
                Entity.DisplayType selectedType = (Entity.DisplayType)displayTypeComboBox.getSelectedItem();
                if (selectedType == Entity.DisplayType.ICON_AND_ANIMATED_FAN) {
                    if (entity.getName().startsWith("fan.")) {
                        String currentFanId = entity.getAssociatedFanEntityId();
                        if (currentFanId == null || currentFanId.trim().isEmpty() || currentFanId.equals(entity.getName())) {
                            associatedFanEntityIdComboBox.setSelectedItem(entity.getName());
                        }
                    }
                }
                updateComboBoxEditorText(displayTypeComboBox, "HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", selectedType);
                showHideComponents();
                validateFanConfiguration();
                markModified();
            }
        });
        makeClickableToOpenDropdown(displayTypeComboBox); // Call after initial setup
        
        // --- NEW and MODIFIED component creation for entity display condition ---
        displayStateLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.displayStateLabel.text"));
        
        // Operator Dropdown
        displayOperatorComboBox = new JComboBox<>(Entity.DisplayOperator.values());
        displayOperatorComboBox.setSelectedItem(entity.getDisplayOperator());
        // Make it look like an editable combo box but prevent typing
        displayOperatorComboBox.setEditable(true);
        JTextField displayOperatorEditor = (JTextField) displayOperatorComboBox.getEditor().getEditorComponent();
        displayOperatorEditor.setEditable(false);
        displayOperatorEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getDisplayOperator() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayOperatorComboBox.%s.text", entity.getDisplayOperator().name()));
            displayOperatorEditor.setText(initialDisplayText);
        }
        displayOperatorComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Entity.DisplayOperator op = (Entity.DisplayOperator) value;
                setText(resource.getString("HomeAssistantFloorPlan.Panel.displayOperatorComboBox." + op.name() + ".text"));
                return this;
            }
        });
        displayOperatorComboBox.addActionListener(e -> {
            entity.setDisplayOperator((Entity.DisplayOperator) displayOperatorComboBox.getSelectedItem());
            Entity.DisplayOperator selectedOp = (Entity.DisplayOperator) displayOperatorComboBox.getSelectedItem();
            updateComboBoxEditorText(displayOperatorComboBox, "HomeAssistantFloorPlan.Panel.displayOperatorComboBox.%s.text", selectedOp);
            markModified();
            updateValueComboBoxesEnabledState(); 
            validateFanConfiguration();
        });
        makeClickableToOpenDropdown(displayOperatorComboBox); // Call after initial setup
        // Value Smart ComboBox
        displayValueComboBox = new JComboBox<>();
        displayValueComboBox.setEditable(true);
        String[] suggestedStates = controller.getSuggestedStatesForEntity(entity);
        for (String suggestion : suggestedStates) {
            if (suggestion != null && !suggestion.isEmpty()) { // Ensure no null/empty items
                displayValueComboBox.addItem(suggestion);
            }
        }
        displayValueComboBox.setSelectedItem(entity.getDisplayValue());
        displayValueComboBox.addActionListener(e -> {
            Object selectedItem = displayValueComboBox.getSelectedItem();
            if (selectedItem != null) {
                entity.setDisplayValue(selectedItem.toString());
                markModified();
            }
        });


        tapActionLabel = new JLabel();
        tapActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.tapActionLabel.text"));
        tapActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        tapActionComboBox.setSelectedItem(entity.getTapAction());
        // Make it look like an editable combo box but prevent typing
        tapActionComboBox.setEditable(true);
        JTextField tapActionEditor = (JTextField) tapActionComboBox.getEditor().getEditorComponent();
        tapActionEditor.setEditable(false);
        tapActionEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getTapAction() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", entity.getTapAction().name()));
            tapActionEditor.setText(initialDisplayText);
        }
        tapActionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", ((Entity.Action)o).name())));
                return rendererComponent;
            }
        });
        tapActionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Entity.Action action = (Entity.Action)tapActionComboBox.getSelectedItem();
                entity.setTapAction(action); // Set action immediately
                updateComboBoxEditorText(tapActionComboBox, "HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", action);
                showHideComponents();
                if (action != Entity.Action.NAVIGATE) { // Clear value if not NAVIGATE
                    tapActionValueTextField.setText("");
                }
                revalidate(); // Add revalidate
                repaint();    // Add repaint
                markModified();
            }
        });
        makeClickableToOpenDropdown(tapActionComboBox); // Call after initial setup
        tapActionValueTextField = new JTextField(10);
        tapActionValueTextField.setText(entity.getTapActionValue());
        tapActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                entity.setTapActionValue(tapActionValueTextField.getText());
                markModified();
            }
        });

        doubleTapActionLabel = new JLabel();
        doubleTapActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.doubleTapActionLabel.text"));
        doubleTapActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        doubleTapActionComboBox.setSelectedItem(entity.getDoubleTapAction());
        // Make it look like an editable combo box but prevent typing
        doubleTapActionComboBox.setEditable(true);
        JTextField doubleTapActionEditor = (JTextField) doubleTapActionComboBox.getEditor().getEditorComponent();
        doubleTapActionEditor.setEditable(false);
        doubleTapActionEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getDoubleTapAction() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", entity.getDoubleTapAction().name()));
            doubleTapActionEditor.setText(initialDisplayText);
        }
        doubleTapActionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", ((Entity.Action)o).name())));
                return rendererComponent;
            }
        });
        doubleTapActionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Entity.Action action = (Entity.Action)doubleTapActionComboBox.getSelectedItem();
                entity.setDoubleTapAction(action);
                updateComboBoxEditorText(doubleTapActionComboBox, "HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", action);
                showHideComponents();
                if (action != Entity.Action.NAVIGATE) {
                    doubleTapActionValueTextField.setText("");
                }
                revalidate(); // Add revalidate
                repaint();    // Add repaint
                markModified();
            }
        });
        makeClickableToOpenDropdown(doubleTapActionComboBox); // Call after initial setup
        doubleTapActionValueTextField = new JTextField(10);
        doubleTapActionValueTextField.setText(entity.getDoubleTapActionValue());
        doubleTapActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                entity.setDoubleTapActionValue(doubleTapActionValueTextField.getText());
                markModified();
            }
        });

        holdActionLabel = new JLabel();
        holdActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.holdActionLabel.text"));
        holdActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        holdActionComboBox.setSelectedItem(entity.getHoldAction());
        // Make it look like an editable combo box but prevent typing
        holdActionComboBox.setEditable(true);
        JTextField holdActionEditor = (JTextField) holdActionComboBox.getEditor().getEditorComponent();
        holdActionEditor.setEditable(false);
        holdActionEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getHoldAction() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", entity.getHoldAction().name()));
            holdActionEditor.setText(initialDisplayText);
        }
        holdActionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", ((Entity.Action)o).name())));
                return rendererComponent;
            }
        });
        holdActionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Entity.Action action = (Entity.Action)holdActionComboBox.getSelectedItem();
                entity.setHoldAction(action);
                updateComboBoxEditorText(holdActionComboBox, "HomeAssistantFloorPlan.Panel.actionComboBox.%s.text", action);
                showHideComponents();
                if (action != Entity.Action.NAVIGATE) {
                    holdActionValueTextField.setText("");
                }
                revalidate(); // Add revalidate
                repaint();    // Add repaint
                markModified();
            }
        });
        makeClickableToOpenDropdown(holdActionComboBox); // Call after initial setup
        holdActionValueTextField = new JTextField(10);
        holdActionValueTextField.setText(entity.getHoldActionValue());
        holdActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void executeUpdate(DocumentEvent e) {
                entity.setHoldActionValue(holdActionValueTextField.getText());
                markModified();
            }
        });

        final Point2d position = entity.getPosition();
        positionLabel = new JLabel();
        positionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.positionLabel.text"));
        positionLeftLabel = new JLabel();
        positionLeftLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.positionLeftLabel.text"));
        final SpinnerNumberModel positionLeftSpinnerModel = new SpinnerNumberModel(0, 0, 1, 0.001);
        positionLeftSpinner = new AutoCommitSpinner(positionLeftSpinnerModel);
        JSpinner.NumberEditor positionLeftEditor = new JSpinner.NumberEditor(positionLeftSpinner, "0.00 %");
        ((JSpinner.DefaultEditor)positionLeftEditor).getTextField().setColumns(5);
        positionLeftSpinner.setEditor(positionLeftEditor);
        positionLeftSpinnerModel.setValue(position.x / 100.0);
        positionLeftSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                final Point2d position = entity.getPosition();
                position.x = ((Number)positionLeftSpinnerModel.getValue()).doubleValue() * 100;
                entity.setPosition(position, true);
                markModified();
            }
        });
        positionTopLabel = new JLabel();
        positionTopLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.positionTopLabel.text"));
        final SpinnerNumberModel positionTopSpinnerModel = new SpinnerNumberModel(0, 0, 1, 0.001);
        positionTopSpinner = new AutoCommitSpinner(positionTopSpinnerModel);
        JSpinner.NumberEditor positionTopEditor = new JSpinner.NumberEditor(positionTopSpinner, "0.00 %");
        ((JSpinner.DefaultEditor)positionTopEditor).getTextField().setColumns(5);
        positionTopSpinner.setEditor(positionTopEditor);
        positionTopSpinnerModel.setValue(position.y / 100.0);
        positionTopSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                final Point2d position = entity.getPosition();
                position.y = ((Number)positionTopSpinnerModel.getValue()).doubleValue() * 100;
                entity.setPosition(position, true);
                markModified();
            }
        });

        opacityLabel = new JLabel();
        opacityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.opacityLabel.text"));
        final SpinnerNumberModel opacitySpinnerModel = new SpinnerNumberModel(0, 0, 1, 0.01);
        opacitySpinner = new AutoCommitSpinner(opacitySpinnerModel);
        JSpinner.NumberEditor opacityEditor = new JSpinner.NumberEditor(opacitySpinner, "0 %");
        ((JSpinner.DefaultEditor)opacityEditor).getTextField().setColumns(5);
        opacitySpinner.setEditor(opacityEditor);
        opacitySpinnerModel.setValue(entity.getOpacity() / 100.0);
        opacitySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                entity.setOpacity((int)(((Number)opacitySpinnerModel.getValue()).doubleValue() * 100));
                markModified();
            }
        });

        scaleFactorLabel = new JLabel();
        scaleFactorLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.scaleFactorLabel.text"));
        // Spinner model: initial value 1.0, min 0.1, max 10.0, step 0.1
        final SpinnerNumberModel scaleFactorSpinnerModel = new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1);
        scaleFactorSpinner = new AutoCommitSpinner(scaleFactorSpinnerModel);
        JSpinner.NumberEditor scaleFactorEditor = new JSpinner.NumberEditor(scaleFactorSpinner, "0.0#"); // Format for decimal
        ((JSpinner.DefaultEditor)scaleFactorEditor).getTextField().setColumns(5);
        scaleFactorSpinner.setEditor(scaleFactorEditor);
        scaleFactorSpinnerModel.setValue(entity.getScaleFactor());
        scaleFactorSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                entity.setScaleFactor(((Number)scaleFactorSpinnerModel.getValue()).doubleValue());
                markModified();
            }
        });

        blinkingLabel = new JLabel();
        blinkingLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.blinkingLabel.text"));
        blinkingCheckbox = new JCheckBox();
        blinkingCheckbox.setSelected(entity.getBlinking());
        blinkingCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                entity.setBlinking(blinkingCheckbox.isSelected());
                markModified();
            }
        });



        backgroundColorLabel = new JLabel();
        backgroundColorLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.backgroundColorLabel.text"));
        backgroundColorTextField = new JTextField(20);
        backgroundColorTextField.setText(entity.getBackgroundColor());
        backgroundColorTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                entity.setBackgroundColor(backgroundColorTextField.getText());
                markModified();
            }
        });

        alwaysOnLabel = new JLabel();
        alwaysOnLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.alwaysOnLabel.text"));
        alwaysOnCheckbox = new JCheckBox();
        alwaysOnCheckbox.setSelected(entity.getAlwaysOn());
        alwaysOnCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                entity.setAlwaysOn(alwaysOnCheckbox.isSelected());
                markModified();
            }
        });

        isRgbLabel = new JLabel();
        isRgbLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.isRgbLabel.text"));
        isRgbCheckbox = new JCheckBox();
        isRgbCheckbox.setSelected(entity.getIsRgb());
        isRgbCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                entity.setIsRgb(isRgbCheckbox.isSelected());
                markModified();
            }
        });

        clickableAreaTypeLabel = new JLabel();
        clickableAreaTypeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.clickableAreaTypeLabel.text"));
        clickableAreaTypeComboBox = new JComboBox<>(Entity.ClickableAreaType.values());
        clickableAreaTypeComboBox.setSelectedItem(entity.getClickableAreaType());
        // Make it look like an editable combo box but prevent typing
        clickableAreaTypeComboBox.setEditable(true);
        JTextField clickableAreaTypeEditor = (JTextField) clickableAreaTypeComboBox.getEditor().getEditorComponent();
        clickableAreaTypeEditor.setEditable(false);
        clickableAreaTypeEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getClickableAreaType() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.clickableAreaTypeComboBox.%s.text", entity.getClickableAreaType().name()));
            clickableAreaTypeEditor.setText(initialDisplayText);
        }
        clickableAreaTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Entity.ClickableAreaType type = (Entity.ClickableAreaType) value;
                setText(resource.getString("HomeAssistantFloorPlan.Panel.clickableAreaTypeComboBox." + type.name() + ".text"));
                return this;
            }
        });
        clickableAreaTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (isProgrammaticClickableAreaChange) {
                    return;
                }

                final Entity.ClickableAreaType selectedTypeFromEvent = (Entity.ClickableAreaType) clickableAreaTypeComboBox.getSelectedItem();
                Entity.ClickableAreaType previousType = entity.getClickableAreaType(); // The type before this action

                if (selectedTypeFromEvent == Entity.ClickableAreaType.ROOM_SIZE) {
                    // Temporarily set the entity's type to ROOM_SIZE to calculate its potential bounds
                    // This is important if getRoomBoundingBoxPercent's logic depends on the entity's current type
                    // Store the actual original type before this temporary modification
                    Entity.ClickableAreaType originalEntityTypeBeforeTempChange = entity.getClickableAreaType();
                    entity.setClickableAreaType(Entity.ClickableAreaType.ROOM_SIZE); // Temporarily set for calculation
                    Map<String, Double> currentEntityPotentialBounds = controller.getRoomBoundingBoxPercent(entity);
                    // Revert to the actual original type, not 'previousType' which might be ROOM_SIZE if re-entered.
                    entity.setClickableAreaType(originalEntityTypeBeforeTempChange); 

                    if (currentEntityPotentialBounds != null) {
                        System.out.println("DEBUG: Checking entity '" + entity.getName() + "' for ROOM_SIZE. Potential bounds: L=" + currentEntityPotentialBounds.get("left") + ", T=" + currentEntityPotentialBounds.get("top") + ", W=" + currentEntityPotentialBounds.get("width") + ", H=" + currentEntityPotentialBounds.get("height"));
                        Entity conflictingEntity = null;
                        for (Entity E : controller.getAllConfiguredEntities()) {
                            if (E == entity) continue; // Skip self

                            if (E.getClickableAreaType() == Entity.ClickableAreaType.ROOM_SIZE) {
                                Map<String, Double> otherEntityBounds = controller.getRoomBoundingBoxPercent(E);
                                if (otherEntityBounds != null) {
                                    System.out.println("DEBUG:   Comparing with other ROOM_SIZE entity '" + E.getName() + "'. Bounds: L=" + otherEntityBounds.get("left") + ", T=" + otherEntityBounds.get("top") + ", W=" + otherEntityBounds.get("width") + ", H=" + otherEntityBounds.get("height"));
                                    if (doAreasOverlap(currentEntityPotentialBounds, otherEntityBounds)) {
                                        System.out.println("DEBUG:     OVERLAP DETECTED with entity '" + E.getName() + "'.");
                                        conflictingEntity = E;
                                        break; 
                                    }
                                } else {
                                    System.err.println("DEBUG:   Could not get bounds for other ROOM_SIZE entity '" + E.getName() + "'. Skipping overlap check.");
                                }
                            }
                        }

                        if (conflictingEntity != null) { // A conflict was found
                            System.out.println("DEBUG: Displaying overlap error. Conflicting entity: '" + conflictingEntity.getName() + "'");
                            String message = String.format(Locale.US, 
                                resource.getString("HomeAssistantFloorPlan.Panel.error.overlappingRoomSize.text"), 
                                conflictingEntity.getName());

                            // Ensure dialog is shown on the EDT
                            // Wrap message in HTML for auto-wrapping in JOptionPane
                            String htmlMessage = String.format("<html><body width='%dpx'>%s</body></html>", 350, message.replace("\n", "<br>"));
                            // Show the dialog
                                JOptionPane.showMessageDialog(EntityOptionsPanel.this, // Parent component
                                        htmlMessage,
                                        resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                                        JOptionPane.WARNING_MESSAGE);
                            // After the dialog is dismissed, force ComboBox state update
                            SwingUtilities.invokeLater(() -> {
                                isProgrammaticClickableAreaChange = true;
                                clickableAreaTypeComboBox.setSelectedItem(previousType);
                                updateComboBoxEditorText(clickableAreaTypeComboBox, "HomeAssistantFloorPlan.Panel.clickableAreaTypeComboBox.%s.text", previousType);
                                clickableAreaTypeComboBox.hidePopup();
                                isProgrammaticClickableAreaChange = false;
                            });
                            return; 
                        }
                    } else {
                        System.err.println("DEBUG: Could not determine potential bounds for current entity '" + entity.getName() + "'. Cannot check for overlap.");
                    }
                }
                // If no conflict, or not ROOM_SIZE, proceed to set the type
                entity.setClickableAreaType(selectedTypeFromEvent); // Update the entity model

                // Defer UI update of the combo box editor text to ensure it runs after
                // any default JComboBox editor updates.
                SwingUtilities.invokeLater(() -> {
                    // Re-fetch the current selection from the combo box at the moment of execution
                    // to ensure we're displaying the text for its actual current state.
                    Entity.ClickableAreaType currentActualSelectionInComboBox = (Entity.ClickableAreaType) clickableAreaTypeComboBox.getSelectedItem();
                    updateComboBoxEditorText(clickableAreaTypeComboBox, "HomeAssistantFloorPlan.Panel.clickableAreaTypeComboBox.%s.text", currentActualSelectionInComboBox);
                });

                markModified();
            }
        });
        makeClickableToOpenDropdown(clickableAreaTypeComboBox); // Call after initial setup
        
        // --- NEW: Create the new smart combo box for furniture display state ---
        furnitureDisplayStateLabel = new JLabel();
        furnitureDisplayStateLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayFurnitureConditionLabel.text"));

        furnitureDisplayOperatorComboBox = new JComboBox<>(Entity.DisplayOperator.values());
        furnitureDisplayOperatorComboBox.setSelectedItem(entity.getFurnitureDisplayOperator());
        // Make it look like an editable combo box but prevent typing
        furnitureDisplayOperatorComboBox.setEditable(true);
        JTextField furnitureDisplayOperatorEditor = (JTextField) furnitureDisplayOperatorComboBox.getEditor().getEditorComponent();
        furnitureDisplayOperatorEditor.setEditable(false);
        furnitureDisplayOperatorEditor.setFocusable(false); // Optional
        // Set initial editor text
        if (entity.getFurnitureDisplayOperator() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayOperatorComboBox.%s.text", entity.getFurnitureDisplayOperator().name()));
            furnitureDisplayOperatorEditor.setText(initialDisplayText);
        }
        furnitureDisplayOperatorComboBox.setRenderer(new DefaultListCellRenderer() {
             @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Entity.DisplayOperator op = (Entity.DisplayOperator) value;
                setText(resource.getString("HomeAssistantFloorPlan.Panel.displayOperatorComboBox." + op.name() + ".text"));
                return this;
            }
        });
        furnitureDisplayOperatorComboBox.addActionListener(e -> {
            entity.setFurnitureDisplayOperator((Entity.DisplayOperator) furnitureDisplayOperatorComboBox.getSelectedItem());
            Entity.DisplayOperator selectedOp = (Entity.DisplayOperator) furnitureDisplayOperatorComboBox.getSelectedItem();
            updateComboBoxEditorText(furnitureDisplayOperatorComboBox, "HomeAssistantFloorPlan.Panel.displayOperatorComboBox.%s.text", selectedOp);
            markModified();
            updateValueComboBoxesEnabledState(); // Update state when operator changes
        });
        makeClickableToOpenDropdown(furnitureDisplayOperatorComboBox); // Call after initial setup
        furnitureDisplayValueComboBox = new JComboBox<>();
        furnitureDisplayValueComboBox.setEditable(true);
        String[] furnitureSuggestions = controller.getSuggestedStatesForFurniture(entity);
        for (String suggestion : furnitureSuggestions) {
            if (suggestion != null && !suggestion.isEmpty()) { // Ensure no null/empty items
                furnitureDisplayValueComboBox.addItem(suggestion);
            }
        }
        furnitureDisplayValueComboBox.setSelectedItem(entity.getFurnitureDisplayValue());
        furnitureDisplayValueComboBox.addActionListener(e -> {
            Object selected = furnitureDisplayValueComboBox.getSelectedItem();
            if (selected != null) {
                entity.setFurnitureDisplayValue(selected.toString());
                markModified();
            }
        });

        // --- NEW: Components for ICON_AND_ANIMATED_FAN ---
        associatedFanEntityIdLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.associatedFanEntityIdLabel.text"));
        associatedFanEntityIdComboBox = new JComboBox<>();
        associatedFanEntityIdComboBox.setEditable(true); // Allow typing
        // Populate with existing fan entities
        List<String> fanIds = controller.getFanEntityIds();
        for (String fanId : fanIds) {
            associatedFanEntityIdComboBox.addItem(fanId);
        }
        associatedFanEntityIdComboBox.setSelectedItem(entity.getAssociatedFanEntityId());
        associatedFanEntityIdComboBox.addActionListener(e -> {
            Object selectedItem = associatedFanEntityIdComboBox.getSelectedItem();
            if (selectedItem != null) {
                entity.setAssociatedFanEntityId(selectedItem.toString());
                markModified();
                validateFanConfiguration();
            }
        });

        showFanWhenOffLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.showFanWhenOffLabel.text"));
        showFanWhenOffCheckbox = new JCheckBox();
        showFanWhenOffCheckbox.setSelected(entity.getShowFanWhenOff());
        showFanWhenOffCheckbox.addActionListener(e -> {
            entity.setShowFanWhenOff(showFanWhenOffCheckbox.isSelected());
            markModified();
        });

        fanColorLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.fanColorLabel.text"));
        fanColorComboBox = new JComboBox<>(Entity.FanColor.values());
        fanColorComboBox.setSelectedItem(entity.getFanColor());
        fanColorComboBox.setEditable(true);
        JTextField fanColorEditor = (JTextField) fanColorComboBox.getEditor().getEditorComponent();
        fanColorEditor.setEditable(false);
        fanColorEditor.setFocusable(false);
        if (entity.getFanColor() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.fanColorComboBox.%s.text", entity.getFanColor().name()));
            fanColorEditor.setText(initialDisplayText);
        }
        fanColorComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.fanColorComboBox.%s.text", ((Entity.FanColor) value).name())));
                return this;
            }
        });
        fanColorComboBox.addActionListener(e -> {
            entity.setFanColor((Entity.FanColor) fanColorComboBox.getSelectedItem());
            updateComboBoxEditorText(fanColorComboBox, "HomeAssistantFloorPlan.Panel.fanColorComboBox.%s.text", (Entity.FanColor) fanColorComboBox.getSelectedItem());
            markModified();
        });
        makeClickableToOpenDropdown(fanColorComboBox);

        // --- NEW: Components for Fan Size ---
        fanSizeLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.fanSizeLabel.text"));
        fanSizeComboBox = new JComboBox<>(Entity.FanSize.values());
        fanSizeComboBox.setSelectedItem(entity.getFanSize());
        fanSizeComboBox.setEditable(true);
        JTextField fanSizeEditor = (JTextField) fanSizeComboBox.getEditor().getEditorComponent();
        fanSizeEditor.setEditable(false);
        fanSizeEditor.setFocusable(false);
        if (entity.getFanSize() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.fanSizeComboBox.%s.text", entity.getFanSize().name()));
            fanSizeEditor.setText(initialDisplayText);
        }
        fanSizeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.fanSizeComboBox.%s.text", ((Entity.FanSize) value).name())));
                return this;
            }
        });
        fanSizeComboBox.addActionListener(e -> {
            entity.setFanSize((Entity.FanSize) fanSizeComboBox.getSelectedItem());
            updateComboBoxEditorText(fanSizeComboBox, "HomeAssistantFloorPlan.Panel.fanSizeComboBox.%s.text", (Entity.FanSize) fanSizeComboBox.getSelectedItem());
            markModified();
        });
        makeClickableToOpenDropdown(fanSizeComboBox);

        showBorderAndBackgroundLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.showBorderAndBackgroundLabel.text"));
        showBorderAndBackgroundCheckbox = new JCheckBox();
        showBorderAndBackgroundCheckbox.setSelected(entity.getShowBorderAndBackground());
        showBorderAndBackgroundCheckbox.addActionListener(e -> {
            entity.setShowBorderAndBackground(showBorderAndBackgroundCheckbox.isSelected());
            showHideComponents(); // Update dependent component states
            markModified();
        });

        // --- NEW: Create components for state-label specific options ---
        labelColorLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.labelColorLabel.text"));
        labelColorComboBox = new JComboBox<>();
        for (String colorKey : LABEL_COLOR_KEYS) {
            labelColorComboBox.addItem(resource.getString("HomeAssistantFloorPlan.Panel.labelColorComboBox." + colorKey + ".text"));
        }
        setComboBoxSelectionFromEntityValue(labelColorComboBox, entity.getLabelColor(), LABEL_COLOR_KEYS, "HomeAssistantFloorPlan.Panel.labelColorComboBox.%s.text", LABEL_COLOR_KEYS[0]); // Default to first color
        labelColorComboBox.addActionListener(e -> {
            String selectedLocalizedColor = (String) labelColorComboBox.getSelectedItem();
            String colorValueToStore = getColorKeyFromLocalized(selectedLocalizedColor, LABEL_COLOR_KEYS, "HomeAssistantFloorPlan.Panel.labelColorComboBox.%s.text");
            entity.setLabelColor(colorValueToStore.toLowerCase()); // Store lowercase
            markModified();
        });

        labelFontWeightLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.labelFontWeightLabel.text"));
        labelFontWeightComboBox = new JComboBox<>();
        for (String weightKey : FONT_WEIGHT_KEYS) {
            labelFontWeightComboBox.addItem(resource.getString("HomeAssistantFloorPlan.Panel.labelFontWeightComboBox." + weightKey + ".text"));
        }
        setComboBoxSelectionFromEntityValue(labelFontWeightComboBox, entity.getLabelFontWeight(), FONT_WEIGHT_KEYS, "HomeAssistantFloorPlan.Panel.labelFontWeightComboBox.%s.text", "NORMAL");
        labelFontWeightComboBox.addActionListener(e -> {
            String selectedDisplayValue = (String) labelFontWeightComboBox.getSelectedItem();
            String valueToStore = getFontWeightValueFromDisplay(selectedDisplayValue, FONT_WEIGHT_KEYS);
            entity.setLabelFontWeight(valueToStore);
            markModified();
        });

        labelTextShadowLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.labelTextShadowLabel.text"));
        // --- MODIFIED: Initialize JComboBox for text shadow ---
        labelTextShadowComboBox = new JComboBox<>();
        // Populate with predefined colors + "None"
        // The actual value stored will be the color name (e.g., "black") or an empty string for "None"
        String[] shadowColors = {"NONE", "BLACK", "WHITE", "GRAY", "RED", "BLUE", "GREEN", "YELLOW"};
        for (String colorKey : shadowColors) {
            labelTextShadowComboBox.addItem(resource.getString("HomeAssistantFloorPlan.Panel.textShadowColorComboBox." + colorKey + ".text"));
        }
        // Set selected item based on entity's current value
        // If entity.getLabelTextShadow() is empty or null, select "None". Otherwise, find the matching localized string.
        String currentShadow = entity.getLabelTextShadow();
        if (currentShadow == null || currentShadow.trim().isEmpty()) {
            labelTextShadowComboBox.setSelectedItem(resource.getString("HomeAssistantFloorPlan.Panel.textShadowColorComboBox.NONE.text"));
        } else {
            // Find the localized string that corresponds to the stored color name (e.g., "black" -> "Black")
            // This assumes stored value is the uppercase key (BLACK, WHITE etc.)
            // If stored value is lowercase (black, white), adjust this logic. For now, assume uppercase key.
            labelTextShadowComboBox.setSelectedItem(resource.getString("HomeAssistantFloorPlan.Panel.textShadowColorComboBox." + currentShadow.toUpperCase() + ".text"));
        }

        labelTextShadowComboBox.addActionListener(e -> {
            String selectedLocalizedColor = (String) labelTextShadowComboBox.getSelectedItem();
            String colorValueToStore = getColorKeyFromLocalized(selectedLocalizedColor, shadowColors, "HomeAssistantFloorPlan.Panel.textShadowColorComboBox.%s.text");
            entity.setLabelTextShadow(colorValueToStore.equals("NONE") ? "" : colorValueToStore.toLowerCase()); // Store lowercase or empty
            markModified();
        });

        labelSuffixLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.labelSuffixLabel.text"));
        labelSuffixComboBox = new JComboBox<>();
        labelSuffixComboBox.setEditable(true);
        labelSuffixComboBox.addItem(""); // Default empty option
        labelSuffixComboBox.addItem(resource.getString("HomeAssistantFloorPlan.Panel.labelSuffixComboBox." + DEGREE_SYMBOL_KEY + ".text")); // Degree symbol
        // Set initial selection
        String currentSuffix = entity.getLabelSuffix();
        if (currentSuffix == null || currentSuffix.isEmpty()) {
            labelSuffixComboBox.setSelectedItem("");
        } else {
            labelSuffixComboBox.setSelectedItem(currentSuffix); // This handles "°" or any custom typed value
        }
        labelSuffixComboBox.addActionListener(e -> {
            entity.setLabelSuffix((String) labelSuffixComboBox.getSelectedItem());
            markModified();
        });


        closeButton = new JButton(actionMap.get(ActionType.CLOSE));
        closeButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.closeButton.text"));

        resetToDefaultsButton = new JButton(actionMap.get(ActionType.RESET_TO_DEFAULTS));
        resetToDefaultsButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.resetToDefaultsButton.text"));
    }

    private void layoutComponents() {
        int labelAlignment = OperatingSystem.isMacOSX() ? JLabel.TRAILING : JLabel.LEADING;
        int standardGap = Math.round(2 * SwingTools.getResolutionScale());
        Insets insets = new Insets(0, standardGap, 0, standardGap);
        int comboBoxInternalPaddingY = 2; // Adjust this value as needed (e.g., 2-4 pixels)
        int currentGridYIndex = 0;

        /* Display type */
        add(displayTypeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        displayTypeLabel.setHorizontalAlignment(labelAlignment);
        add(displayTypeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Display State Condition */
        add(displayStateLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        displayStateLabel.setHorizontalAlignment(labelAlignment);
        // Panel to hold both the operator and value combo boxes
        JPanel displayConditionPanel = new JPanel(new GridBagLayout());
        displayConditionPanel.add(displayOperatorComboBox, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
        displayConditionPanel.add(displayValueComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(displayConditionPanel, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Tap action */
        add(tapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        tapActionLabel.setHorizontalAlignment(labelAlignment);
        add(tapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(tapActionValueTextField, new GridBagConstraints( // Allow TextField to expand
            3, currentGridYIndex, 2, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Double tap action */
        add(doubleTapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        doubleTapActionLabel.setHorizontalAlignment(labelAlignment);
        add(doubleTapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(doubleTapActionValueTextField, new GridBagConstraints( // Allow TextField to expand
            3, currentGridYIndex, 2, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Hold action */
        add(holdActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        holdActionLabel.setHorizontalAlignment(labelAlignment);
        add(holdActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(holdActionValueTextField, new GridBagConstraints( // Allow TextField to expand
            3, currentGridYIndex, 2, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Position */
        add(positionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        positionLabel.setHorizontalAlignment(labelAlignment);
        add(positionLeftLabel, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(positionLeftSpinner, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(positionTopLabel, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(positionTopSpinner, new GridBagConstraints(
            4, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Opacity */
        add(opacityLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        opacityLabel.setHorizontalAlignment(labelAlignment);
        add(opacitySpinner, new GridBagConstraints( // Allow Spinner to expand
            1, currentGridYIndex, 2, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Scale Factor */
        add(scaleFactorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        scaleFactorLabel.setHorizontalAlignment(labelAlignment);
        add(scaleFactorSpinner, new GridBagConstraints( // Allow Spinner to expand
            1, currentGridYIndex, 2, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Blinking */
        add(blinkingLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        blinkingLabel.setHorizontalAlignment(labelAlignment);
        add(blinkingCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Clickable Area Type */
        add(clickableAreaTypeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        clickableAreaTypeLabel.setHorizontalAlignment(labelAlignment);
        add(clickableAreaTypeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START, // Span remaining columns
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Show Border and Background - Moved Before Background Color */
        add(showBorderAndBackgroundLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        showBorderAndBackgroundLabel.setHorizontalAlignment(labelAlignment);
        add(showBorderAndBackgroundCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.LINE_START, 
            GridBagConstraints.NONE, insets, 0, 0));
        currentGridYIndex++;

        /* Background color */
        add(backgroundColorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        backgroundColorLabel.setHorizontalAlignment(labelAlignment);
        add(backgroundColorTextField, new GridBagConstraints( // Allow TextField to expand
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        // --- NEW: Layout for ICON_AND_ANIMATED_FAN options ---
        add(associatedFanEntityIdLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        associatedFanEntityIdLabel.setHorizontalAlignment(labelAlignment);
        add(associatedFanEntityIdComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(showFanWhenOffLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        showFanWhenOffLabel.setHorizontalAlignment(labelAlignment);
        add(showFanWhenOffCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, insets, 0, 0));
        currentGridYIndex++;

        add(fanColorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        fanColorLabel.setHorizontalAlignment(labelAlignment);
        add(fanColorComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(fanSizeLabel, new GridBagConstraints( // Add Fan Size Label
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        fanSizeLabel.setHorizontalAlignment(labelAlignment);
        add(fanSizeComboBox, new GridBagConstraints( // Add Fan Size ComboBox
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        // --- NEW: Layout for state-label specific options ---
        add(labelColorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        labelColorLabel.setHorizontalAlignment(labelAlignment);
        add(labelColorComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0)); // Changed from labelColorTextField
        currentGridYIndex++;

        add(labelTextShadowLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        labelTextShadowLabel.setHorizontalAlignment(labelAlignment);
        add(labelTextShadowComboBox, new GridBagConstraints( // Add ComboBox to layout
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0)); // Changed from labelTextShadowTextField
        currentGridYIndex++;

        add(labelFontWeightLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        labelFontWeightLabel.setHorizontalAlignment(labelAlignment);
        add(labelFontWeightComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0)); // Changed from labelFontWeightTextField
        currentGridYIndex++;

        add(labelSuffixLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        labelSuffixLabel.setHorizontalAlignment(labelAlignment);
        add(labelSuffixComboBox, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0)); // Changed from labelSuffixTextField
        currentGridYIndex++;

        if (entity.getIsLight())
            layoutLightSpecificComponents(labelAlignment, insets, currentGridYIndex);
        else
            layoutNonLightSpecificComponents(labelAlignment, insets, currentGridYIndex);
    }

    private void layoutLightSpecificComponents(int labelAlignment, Insets insets, int currentGridYIndex) {
        /* Always on */
        add(alwaysOnLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        alwaysOnLabel.setHorizontalAlignment(labelAlignment);
        add(alwaysOnCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Is RGB */
        add(isRgbLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        isRgbLabel.setHorizontalAlignment(labelAlignment);
        add(isRgbCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
    }

    private void layoutNonLightSpecificComponents(int labelAlignment, Insets insets, int currentGridYIndex) {
        /* Display Furniture Condition */
        add(furnitureDisplayStateLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        furnitureDisplayStateLabel.setHorizontalAlignment(labelAlignment);
        // Panel to hold both the operator and value combo boxes
        JPanel furnitureConditionPanel = new JPanel(new GridBagLayout());
        furnitureConditionPanel.add(furnitureDisplayOperatorComboBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
        furnitureConditionPanel.add(furnitureDisplayValueComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(furnitureConditionPanel, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
    }

    private void markModified() {
        Color modifiedColor = new Color(200, 0, 0);

        displayTypeLabel.setForeground(entity.isDisplayTypeModified() ? modifiedColor : Color.BLACK);
        displayStateLabel.setForeground(entity.isDisplayConditionModified() ? modifiedColor : Color.BLACK);
        tapActionLabel.setForeground(entity.isTapActionModified() ? modifiedColor : Color.BLACK);
        doubleTapActionLabel.setForeground(entity.isDoubleTapActionModified() ? modifiedColor : Color.BLACK);
        holdActionLabel.setForeground(entity.isHoldActionModified() ? modifiedColor : Color.BLACK); // Keep this
        positionLabel.setForeground(entity.isPositionModified() ? modifiedColor : Color.BLACK);
        alwaysOnLabel.setForeground(entity.isAlwaysOnModified() ? modifiedColor : Color.BLACK); // Keep this
        isRgbLabel.setForeground(entity.isIsRgbModified() ? modifiedColor : Color.BLACK); // Keep this
        opacityLabel.setForeground(entity.isOpacityModified() ? modifiedColor : Color.BLACK);
        scaleFactorLabel.setForeground(entity.isScaleFactorModified() ? modifiedColor : Color.BLACK);
        blinkingLabel.setForeground(entity.isBlinkingModified() ? modifiedColor : Color.BLACK);
        backgroundColorLabel.setForeground(entity.isBackgroundColorModified() ? modifiedColor : Color.BLACK);
        // If you add isClickableAreaTypeModified() to Entity.java, you can uncomment/add this:
        clickableAreaTypeLabel.setForeground(entity.isClickableAreaTypeModified() ? modifiedColor : UIManager.getColor("Label.foreground")); // Assuming isClickableAreaTypeModified exists
        associatedFanEntityIdLabel.setForeground(entity.isAssociatedFanEntityIdModified() ? modifiedColor : Color.BLACK);
        showFanWhenOffLabel.setForeground(entity.isShowFanWhenOffModified() ? modifiedColor : Color.BLACK);
        fanColorLabel.setForeground(entity.isFanColorModified() ? modifiedColor : UIManager.getColor("Label.foreground"));
        showBorderAndBackgroundLabel.setForeground(entity.isShowBorderAndBackgroundModified() ? modifiedColor : UIManager.getColor("Label.foreground"));
        furnitureDisplayStateLabel.setForeground(entity.isFurnitureDisplayConditionModified() ? modifiedColor : Color.BLACK);
        labelColorLabel.setForeground(entity.isLabelColorModified() ? modifiedColor : UIManager.getColor("Label.foreground"));
        labelTextShadowLabel.setForeground(entity.isLabelTextShadowModified() ? modifiedColor : UIManager.getColor("Label.foreground"));
        labelFontWeightLabel.setForeground(entity.isLabelFontWeightModified() ? modifiedColor : UIManager.getColor("Label.foreground"));
        labelSuffixLabel.setForeground(entity.isLabelSuffixModified() ? modifiedColor : UIManager.getColor("Label.foreground"));

        validateFanConfiguration(); // Ensure fan config validation overrides color if needed
    }

    private void showHideComponents() {
        tapActionValueTextField.setVisible((Entity.Action)tapActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
        doubleTapActionValueTextField.setVisible((Entity.Action)doubleTapActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
        holdActionValueTextField.setVisible((Entity.Action)holdActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);

        // Furniture display condition is for non-lights
        boolean furnitureDisplayEnabled = !entity.getIsLight();
        furnitureDisplayStateLabel.setVisible(furnitureDisplayEnabled);
        furnitureDisplayOperatorComboBox.setVisible(furnitureDisplayEnabled);
        furnitureDisplayValueComboBox.setVisible(furnitureDisplayEnabled);
        if (furnitureDisplayEnabled) updateValueComboBoxesEnabledState(); // Ensure its value field is correctly enabled/disabled

        // Show/hide ICON_AND_ANIMATED_FAN specific components
        boolean fanComponentsVisible = (Entity.DisplayType)displayTypeComboBox.getSelectedItem() == Entity.DisplayType.ICON_AND_ANIMATED_FAN;
        associatedFanEntityIdLabel.setVisible(fanComponentsVisible);
        associatedFanEntityIdComboBox.setVisible(fanComponentsVisible);
        showFanWhenOffLabel.setVisible(fanComponentsVisible);
        showFanWhenOffCheckbox.setVisible(fanComponentsVisible);
        fanColorLabel.setVisible(fanComponentsVisible);
        fanColorComboBox.setVisible(fanComponentsVisible);
        fanSizeLabel.setVisible(fanComponentsVisible); // Show/hide Fan Size components
        fanSizeComboBox.setVisible(fanComponentsVisible);

        // Border and Background option is always visible
        showBorderAndBackgroundLabel.setVisible(true);
        showBorderAndBackgroundCheckbox.setVisible(true);
        // Enable/disable background color field based on checkbox
        boolean borderBgEnabled = showBorderAndBackgroundCheckbox.isSelected();
        backgroundColorLabel.setEnabled(borderBgEnabled);
        backgroundColorTextField.setEnabled(borderBgEnabled);

        // Show/hide state-label specific components
        boolean labelSpecificComponentsVisible = (Entity.DisplayType)displayTypeComboBox.getSelectedItem() == Entity.DisplayType.LABEL;
        labelColorLabel.setVisible(labelSpecificComponentsVisible);
        labelColorComboBox.setVisible(labelSpecificComponentsVisible);
        labelTextShadowLabel.setVisible(labelSpecificComponentsVisible);
        labelTextShadowComboBox.setVisible(labelSpecificComponentsVisible); // Show/hide ComboBox
        labelFontWeightLabel.setVisible(labelSpecificComponentsVisible);
        labelFontWeightComboBox.setVisible(labelSpecificComponentsVisible);
        labelSuffixLabel.setVisible(labelSpecificComponentsVisible);
        labelSuffixComboBox.setVisible(labelSpecificComponentsVisible);

        // Ensure the panel re-calculates its layout and repaints
        revalidate();
        repaint();

        // If the panel is already part of a visible dialog,
        // tell the dialog to re-pack itself to fit the new content size.
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog && window.isVisible()) {
            JDialog dialog = (JDialog) window;
            dialog.pack();
            // Re-center after packing, as pack might shift the dialog
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        }
    }

    // --- NEW: Method to update the enabled state of value combo boxes ---
    private void updateValueComboBoxesEnabledState() {
        Entity.DisplayOperator entityOperator = (Entity.DisplayOperator) displayOperatorComboBox.getSelectedItem();
        boolean entityValueEnabled = !(entityOperator == Entity.DisplayOperator.ALWAYS || entityOperator == Entity.DisplayOperator.NEVER);
        displayValueComboBox.setEnabled(entityValueEnabled);
        // Optionally clear the value when disabled
        if (!entityValueEnabled) {
             displayValueComboBox.setSelectedItem(""); // Or null, depending on desired behavior
        }

        Entity.DisplayOperator furnitureOperator = (Entity.DisplayOperator) furnitureDisplayOperatorComboBox.getSelectedItem();
        boolean furnitureValueEnabled = !(furnitureOperator == Entity.DisplayOperator.ALWAYS || furnitureOperator == Entity.DisplayOperator.NEVER);
        furnitureDisplayValueComboBox.setEnabled(furnitureValueEnabled);
        // Optionally clear the value when disabled
        if (!furnitureValueEnabled) {
            furnitureDisplayValueComboBox.setSelectedItem(""); // Or null, depending on desired behavior
        }
        validateFanConfiguration();
    }

    private void validateFanConfiguration() {
        boolean fanConfigIsValid = true;
        Color defaultLabelColor = UIManager.getColor("Label.foreground");
        Color modifiedColor = new Color(200, 0, 0); // Your modified color

        if (Entity.DisplayType.ICON_AND_ANIMATED_FAN.equals(displayTypeComboBox.getSelectedItem())) {
            String fanId = (String) associatedFanEntityIdComboBox.getSelectedItem();
            if (fanId == null || fanId.trim().isEmpty()) {
                fanConfigIsValid = false;
            } else {
                if (!isValidHomeAssistantEntityIdFormat(fanId)) {
                    fanConfigIsValid = false;
                } else {
                    String domain = fanId.substring(0, fanId.indexOf('.'));
                    if (!isValidOnOffDomain(domain)) {
                        fanConfigIsValid = false;
                    }
                }
            }
            associatedFanEntityIdLabel.setForeground(fanConfigIsValid ? (entity.isAssociatedFanEntityIdModified() ? modifiedColor : defaultLabelColor) : Color.RED);
        } else {
            // Reset color if not in fan mode, respecting modified state
            associatedFanEntityIdLabel.setForeground(entity.isAssociatedFanEntityIdModified() ? modifiedColor : defaultLabelColor);
        }
    }
    private boolean isValidHomeAssistantEntityIdFormat(String entityId) {
        if (entityId == null) return false;
        // Basic check for "domain.object_id" format. Allows letters, numbers, underscore.
        return entityId.matches("^[a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+$");
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parentComponent), entity.getName());
        dialog.applyComponentOrientation(parentComponent != null ?
            parentComponent.getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        
        // Update component states that might affect layout before packing
        updateValueComboBoxesEnabledState(); // Ensure correct state on display
        showHideComponents();
        validateFanConfiguration(); // And validate fan config on display

        // Pack the dialog.
        // showHideComponents() was called above, which calls revalidate() on 'this' (EntityOptionsPanel).
        // This ensures the panel has its correct preferred size before the dialog is packed.
        dialog.pack();
        
        dialog.setResizable(true); // Explicitly set resizable
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(parentComponent)); // Center it
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void close() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window.isDisplayable())
            window.dispose();
    }

    private <T extends Enum<T>> void updateComboBoxEditorText(JComboBox<T> comboBox, String resourceKeyPattern, T selectedValue) {
        // This helper is for JComboBoxes that are set to editable() but whose editor JTextField is set to non-editable.
        // It ensures the editor displays the localized string instead of the enum's toString().
        if (comboBox == null || !comboBox.isEditable()) {
            return;
        }
        Component editorComp = comboBox.getEditor().getEditorComponent();
        if (!(editorComp instanceof JTextField)) { // Ensure the editor component is a JTextField
            return;
        }
        JTextField editorTextField = (JTextField) editorComp;
        // Only apply if the JTextField itself is not editable (our "fake" setup)
        if (editorTextField.isEditable()) { 
            return;
        }

        String determinedTextValue; // Use a non-final temporary variable
        if (selectedValue != null) {
            if (resource != null) {
                try {
                    determinedTextValue = resource.getString(String.format(resourceKeyPattern, selectedValue.name()));
                } catch (java.util.MissingResourceException e) {
                    // Fallback if resource key is missing, log and use enum's default toString()
                    System.err.println("Warning: Missing resource for JComboBox editor text: " + String.format(resourceKeyPattern, selectedValue.name()) + ". Using default name.");
                    determinedTextValue = selectedValue.toString(); 
                }
            } else { // resource is null, but selectedValue is not
                determinedTextValue = selectedValue.toString(); // Fallback if resource bundle is somehow null
            }
        } else { // selectedValue is null
            determinedTextValue = ""; // Empty string for a null selectedValue
        }
        final String textToSet = determinedTextValue; // Assign to the final variable once

        // Directly set the text of the JTextField component of the editor.
        // Only update if the text is actually different to avoid potential event loops or unnecessary screen flicker.
        if (!editorTextField.getText().equals(textToSet)) {
            editorTextField.setText(textToSet);
        }
    }

    // --- NEW: Helper method to make the editor area of a non-typable editable combo box clickable ---
    private <T> void makeClickableToOpenDropdown(JComboBox<T> comboBox) {
        if (comboBox == null || !comboBox.isEditable()) {
            return; // Only apply to editable combos
        }
        Component editorComp = comboBox.getEditor().getEditorComponent();
        if (editorComp == null) {
            return;
        }
        

        if (editorComp instanceof JTextField && ((JTextField) editorComp).isEditable()) {
            return; // Only apply if the editor is a non-editable JTextField
        }

        editorComp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // For debugging, you can uncomment the line below:
                // System.out.println("Editor MousePressed on: " + comboBox.getClass().getSimpleName() + "@" + Integer.toHexString(comboBox.hashCode()) + " | Current PopupVisible: " + comboBox.isPopupVisible() + " | Enabled: " + comboBox.isEnabled());
                if (comboBox.isEnabled()) {
                    // Directly toggle the popup visibility state
                    // Using invokeLater to ensure it's queued after any default processing
                    SwingUtilities.invokeLater(() -> {
                        comboBox.setPopupVisible(!comboBox.isPopupVisible());
                    });
                }
            }
        });
    }

    private boolean isValidOnOffDomain(String domain) {
        if (domain == null) return false;
        // List of domains that typically have on/off states
        List<String> onOffDomains = Arrays.asList("fan", "light", "switch", "binary_sensor", "input_boolean", "media_player", "climate", "humidifier", "siren", "valve", "lock", "cover");
        return onOffDomains.contains(domain.toLowerCase());
    }

    private boolean doAreasOverlap(Map<String, Double> rectA, Map<String, Double> rectB) {
        if (rectA == null || rectB == null) {
            return false;
        }

        double aLeft = rectA.get("left");
        double aTop = rectA.get("top");
        double aWidth = rectA.get("width");
        double aHeight = rectA.get("height");

        double bLeft = rectB.get("left");
        double bTop = rectB.get("top");
        double bWidth = rectB.get("width");
        double bHeight = rectB.get("height");

        // Check for non-overlap conditions first for clarity
        if (aLeft + aWidth <= bLeft || aLeft >= bLeft + bWidth || aTop + aHeight <= bTop || aTop >= bTop + bHeight) {
            return false; // No overlap
        }
        return true; // Overlap
    }

    // Helper to get the original key (e.g., "BLACK") from a localized string (e.g., "Black")
    private String getColorKeyFromLocalized(String localizedValue, String[] keys, String resourcePattern) {
        if (localizedValue == null) return "NONE"; // Default to NONE if null
        for (String key : keys) {
            String localizedKeyString = resource.getString(String.format(resourcePattern, key));
            if (localizedValue.equals(localizedKeyString)) {
                return key; // Return the uppercase key (BLACK, WHITE, etc.)
            }
        }
        return "NONE"; // Fallback if no match found
    }

    // Helper to set ComboBox selection based on stored entity value
    private void setComboBoxSelectionFromEntityValue(JComboBox<String> comboBox, String entityValue, String[] keys, String resourcePattern, String defaultKeyIfNotFound) {
        if (entityValue == null || entityValue.trim().isEmpty()) {
            // If entity value is empty, try to select the item corresponding to defaultKeyIfNotFound
            String defaultLocalized = resource.getString(String.format(resourcePattern, defaultKeyIfNotFound));
            comboBox.setSelectedItem(defaultLocalized);
            return;
        }

        // Try to find a direct match for keys like "NORMAL", "BOLD", "BLACK", "WHITE"
        for (String key : keys) {
            // For font weights like "100", "700", the entityValue is the key itself.
            // For colors like "black", "white", entityValue is lowercase key.
            if (entityValue.equalsIgnoreCase(key) || entityValue.equals(key.toLowerCase())) {
                 String localized = resource.getString(String.format(resourcePattern, key.toUpperCase()));
                 comboBox.setSelectedItem(localized);
                 return;
            }
        }
        // If no direct key match, assume entityValue might be a custom typed value (e.g. for editable suffix)
        // or a value that doesn't have a direct key (like a specific hex color if we allowed that)
        // For non-editable combo boxes, this means we couldn't find a match.
        if (!comboBox.isEditable()) {
            String defaultLocalized = resource.getString(String.format(resourcePattern, defaultKeyIfNotFound.toUpperCase()));
            comboBox.setSelectedItem(defaultLocalized); // Fallback to default
        } else {
            comboBox.setSelectedItem(entityValue); // For editable, just set it
        }
    }

    // Helper to get the storable font-weight value from its display string
    private String getFontWeightValueFromDisplay(String displayValue, String[] keys) {
        if (displayValue == null) return "normal"; // Default
        for (String key : keys) {
            String localizedKeyString = resource.getString("HomeAssistantFloorPlan.Panel.labelFontWeightComboBox." + key + ".text");
            if (displayValue.equals(localizedKeyString)) {
                return key.toLowerCase(); // "normal", "bold", or "100", "700" etc.
            }
        }
        return displayValue; // Should not happen if populated correctly, but as a fallback
    }
}
