package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
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

    private JButton closeButton;
    private JButton resetToDefaultsButton;
    private ResourceBundle resource;

    public EntityOptionsPanel(UserPreferences preferences, Entity entity, Controller controller) {
        super(new GridBagLayout());
        this.entity = entity;
        this.controller = controller;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault());
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
                updateComboBoxEditorText(displayTypeComboBox, "HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", selectedType);
                showHideComponents();
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
        backgroundColorTextField.setText(entity.getBackgrounColor());
        backgroundColorTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                entity.setBackgrounColor(backgroundColorTextField.getText());
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
                entity.setClickableAreaType((Entity.ClickableAreaType) clickableAreaTypeComboBox.getSelectedItem());
                Entity.ClickableAreaType selectedType = (Entity.ClickableAreaType) clickableAreaTypeComboBox.getSelectedItem();
                updateComboBoxEditorText(clickableAreaTypeComboBox, "HomeAssistantFloorPlan.Panel.clickableAreaTypeComboBox.%s.text", selectedType);
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

        /* Background color */
        add(backgroundColorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        backgroundColorLabel.setHorizontalAlignment(labelAlignment);
        add(backgroundColorTextField, new GridBagConstraints( // Allow TextField to expand
            1, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
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
        // clickableAreaTypeLabel.setForeground(entity.isClickableAreaTypeModified() ? modifiedColor : Color.BLACK);
        furnitureDisplayStateLabel.setForeground(entity.isFurnitureDisplayConditionModified() ? modifiedColor : Color.BLACK);
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
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parentComponent), entity.getName());
        dialog.applyComponentOrientation(parentComponent != null ?
            parentComponent.getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        updateValueComboBoxesEnabledState(); // Ensure correct state on display
        showHideComponents();
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
        if (!(editorComp instanceof JTextField) || ((JTextField) editorComp).isEditable()) {
            // Only apply to our "fake" editable (but non-typable) combo boxes
            return;
        }

        final String textToSet = (selectedValue != null) ?
                                 resource.getString(String.format(resourceKeyPattern, selectedValue.name())) :
                                 "";

        SwingUtilities.invokeLater(() -> {
            Object currentEditorItem = comboBox.getEditor().getItem();
            // Only update if the text is actually different to avoid potential event loops or unnecessary updates
            if (currentEditorItem == null || !textToSet.equals(currentEditorItem.toString())) {
                comboBox.getEditor().setItem(textToSet);
            }
        });
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
}
