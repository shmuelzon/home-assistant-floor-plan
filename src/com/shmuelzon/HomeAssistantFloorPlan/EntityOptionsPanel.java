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

    private Controller controller;
    private Entity entity;
    private JLabel displayTypeLabel;
    private JComboBox<Entity.DisplayType> displayTypeComboBox;
    private JLabel iconOverrideLabel;
    private JTextField iconOverrideTextField;
    private JLabel displayConditionLabel;
    private JComboBox<Entity.DisplayCondition> displayConditionComboBox;
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
    private JLabel backgroundColorLabel;
    private JTextField backgroundColorTextField;
    private JLabel alwaysOnLabel;
    private JCheckBox alwaysOnCheckbox;
    private JLabel autoCropLabel;
    private JCheckBox autoCropCheckbox;
    private JLabel isRgbLabel;
    private JCheckBox isRgbCheckbox;
    private JLabel displayFurnitureConditionLabel;
    private JComboBox<Entity.DisplayFurnitureCondition> displayFurnitureConditionComboBox;
    private JTextField displayFurnitureConditionValueTextField;
    private JLabel openFurnitureConditionLabel;
    private JComboBox<Entity.OpenFurnitureCondition> openFurnitureConditionComboBox;
    private JTextField openFurnitureConditionValueTextField;
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
                close();
            }
        });
    }

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        displayTypeLabel = new JLabel();
        displayTypeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayTypeLabel.text"));
        displayTypeComboBox = new JComboBox<Entity.DisplayType>(Entity.DisplayType.values());
        displayTypeComboBox.setSelectedItem(entity.getDisplayType());
        displayTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", ((Entity.DisplayType)o).name())));
                return rendererComponent;
            }
        });
        displayTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                showHideComponents();
                entity.setDisplayType((Entity.DisplayType)displayTypeComboBox.getSelectedItem());
                markModified();
            }
        });

        iconOverrideLabel = new JLabel();
        iconOverrideLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.iconOverrideLabel.text"));
        iconOverrideTextField = new JTextField(10);
        iconOverrideTextField.setText(entity.getIconOverride());
        iconOverrideTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String iconOverride = iconOverrideTextField.getText();
                entity.setIconOverride(iconOverride);
                markModified();
            }
        });

        displayConditionLabel = new JLabel();
        displayConditionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayConditionLabel.text"));
        displayConditionComboBox = new JComboBox<Entity.DisplayCondition>(Entity.DisplayCondition.values());
        displayConditionComboBox.setSelectedItem(entity.getDisplayCondition());
        displayConditionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayConditionComboBox.%s.text", ((Entity.DisplayCondition)o).name())));
                return rendererComponent;
            }
        });
        displayConditionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                entity.setDisplayCondition((Entity.DisplayCondition)displayConditionComboBox.getSelectedItem());
                markModified();
            }
        });

        tapActionLabel = new JLabel();
        tapActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.tapActionLabel.text"));
        tapActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        tapActionComboBox.setSelectedItem(entity.getTapAction());
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
                showHideComponents();
                if (tapActionValueTextField.getText().isEmpty() && action == Entity.Action.NAVIGATE)
                    return;
                entity.setTapAction(action);
                markModified();
            }
        });
        tapActionValueTextField = new JTextField(10);
        tapActionValueTextField.setText(entity.getTapActionValue());
        tapActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String actionValue = tapActionValueTextField.getText();
                if (actionValue.isEmpty())
                    return;
                entity.setTapActionValue(actionValue);
                entity.setTapAction((Entity.Action)tapActionComboBox.getSelectedItem());
                markModified();
            }
        });

        doubleTapActionLabel = new JLabel();
        doubleTapActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.doubleTapActionLabel.text"));
        doubleTapActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        doubleTapActionComboBox.setSelectedItem(entity.getDoubleTapAction());
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
                showHideComponents();
                if (doubleTapActionValueTextField.getText().isEmpty() && action == Entity.Action.NAVIGATE)
                    return;
                entity.setDoubleTapAction(action);
                markModified();
            }
        });
        doubleTapActionValueTextField = new JTextField(10);
        doubleTapActionValueTextField.setText(entity.getDoubleTapActionValue());
        doubleTapActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String actionValue = doubleTapActionValueTextField.getText();
                if (actionValue.isEmpty())
                    return;
                entity.setDoubleTapActionValue(actionValue);
                entity.setDoubleTapAction((Entity.Action)doubleTapActionComboBox.getSelectedItem());
                markModified();
            }
        });

        holdActionLabel = new JLabel();
        holdActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.holdActionLabel.text"));
        holdActionComboBox = new JComboBox<Entity.Action>(Entity.Action.values());
        holdActionComboBox.setSelectedItem(entity.getHoldAction());
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
                showHideComponents();
                if (holdActionValueTextField.getText().isEmpty() && action == Entity.Action.NAVIGATE)
                    return;
                entity.setHoldAction(action);
                markModified();
            }
        });
        holdActionValueTextField = new JTextField(10);
        holdActionValueTextField.setText(entity.getHoldActionValue());
        holdActionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String actionValue = holdActionValueTextField.getText();
                if (actionValue.isEmpty())
                    return;
                entity.setHoldActionValue(actionValue);
                entity.setHoldAction((Entity.Action)holdActionComboBox.getSelectedItem());
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

        displayFurnitureConditionLabel = new JLabel();
        displayFurnitureConditionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayFurnitureConditionLabel.text"));
        displayFurnitureConditionComboBox = new JComboBox<Entity.DisplayFurnitureCondition>(Entity.DisplayFurnitureCondition.values());
        displayFurnitureConditionComboBox.setSelectedItem(entity.getDisplayFurnitureCondition());
        displayFurnitureConditionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayFurnitureConditionComboBox.%s.text", ((Entity.DisplayFurnitureCondition)o).name())));
                return rendererComponent;
            }
        });
        displayFurnitureConditionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Entity.DisplayFurnitureCondition condition = (Entity.DisplayFurnitureCondition)displayFurnitureConditionComboBox.getSelectedItem();
                showHideComponents();
                if (displayFurnitureConditionValueTextField.getText().isEmpty() && condition != Entity.DisplayFurnitureCondition.ALWAYS)
                    return;
                entity.setDisplayFurnitureCondition(condition);
                markModified();
            }
        });
        displayFurnitureConditionValueTextField = new JTextField(10);
        displayFurnitureConditionValueTextField.setText(entity.getDisplayFurnitureConditionValue());
        displayFurnitureConditionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String conditionValue = displayFurnitureConditionValueTextField.getText();
                if (conditionValue.isEmpty())
                    return;
                entity.setDisplayFurnitureConditionValue(conditionValue);
                entity.setDisplayFurnitureCondition((Entity.DisplayFurnitureCondition)displayFurnitureConditionComboBox.getSelectedItem());
                markModified();
            }
        });

        openFurnitureConditionLabel = new JLabel();
        openFurnitureConditionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.openFurnitureConditionLabel.text"));
        openFurnitureConditionComboBox = new JComboBox<Entity.OpenFurnitureCondition>(Entity.OpenFurnitureCondition.values());
        openFurnitureConditionComboBox.setSelectedItem(entity.getOpenFurnitureCondition());
        openFurnitureConditionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.openFurnitureConditionComboBox.%s.text", ((Entity.OpenFurnitureCondition)o).name())));
                return rendererComponent;
            }
        });
        openFurnitureConditionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Entity.OpenFurnitureCondition condition = (Entity.OpenFurnitureCondition)openFurnitureConditionComboBox.getSelectedItem();
                showHideComponents();
                if (openFurnitureConditionValueTextField.getText().isEmpty() && condition != Entity.OpenFurnitureCondition.ALWAYS)
                    return;
                entity.setOpenFurnitureCondition(condition);
                markModified();
            }
        });
        openFurnitureConditionValueTextField = new JTextField(10);
        openFurnitureConditionValueTextField.setText(entity.getOpenFurnitureConditionValue());
        openFurnitureConditionValueTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                String conditionValue = openFurnitureConditionValueTextField.getText();
                if (conditionValue.isEmpty())
                    return;
                entity.setOpenFurnitureConditionValue(conditionValue);
                entity.setOpenFurnitureCondition((Entity.OpenFurnitureCondition)openFurnitureConditionComboBox.getSelectedItem());
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
        int currentGridYIndex = 0;

        /* Display type */
        add(displayTypeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        displayTypeLabel.setHorizontalAlignment(labelAlignment);
        add(displayTypeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Icon override */
        add(iconOverrideLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        iconOverrideLabel.setHorizontalAlignment(labelAlignment);
        add(iconOverrideTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Display Condition */
        add(displayConditionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        displayConditionLabel.setHorizontalAlignment(labelAlignment);
        add(displayConditionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Tap action */
        add(tapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        tapActionLabel.setHorizontalAlignment(labelAlignment);
        add(tapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(tapActionValueTextField, new GridBagConstraints(
            3, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Double tap action */
        add(doubleTapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        doubleTapActionLabel.setHorizontalAlignment(labelAlignment);
        add(doubleTapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(doubleTapActionValueTextField, new GridBagConstraints(
            3, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Hold action */
        add(holdActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        holdActionLabel.setHorizontalAlignment(labelAlignment);
        add(holdActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(holdActionValueTextField, new GridBagConstraints(
            3, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
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
        add(opacitySpinner, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Background color */
        add(backgroundColorLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        backgroundColorLabel.setHorizontalAlignment(labelAlignment);
        add(backgroundColorTextField, new GridBagConstraints(
            1, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        if (entity.getIsLight())
            layoutLightSpecificComponents(labelAlignment, insets, currentGridYIndex);
        else if (entity.getIsDoorOrWindow())
            layoutDoorOrWindowSpecificComponents(labelAlignment, insets, currentGridYIndex);
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

    private void layoutDoorOrWindowSpecificComponents(int labelAlignment, Insets insets, int currentGridYIndex) {
        add(openFurnitureConditionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        openFurnitureConditionLabel.setHorizontalAlignment(labelAlignment);
        add(openFurnitureConditionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(openFurnitureConditionValueTextField, new GridBagConstraints(
            3, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
    }

    private void layoutNonLightSpecificComponents(int labelAlignment, Insets insets, int currentGridYIndex) {
        add(displayFurnitureConditionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        displayFurnitureConditionLabel.setHorizontalAlignment(labelAlignment);
        add(displayFurnitureConditionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(displayFurnitureConditionValueTextField, new GridBagConstraints(
            3, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
    }

    private void markModified() {
        Color modifiedColor = new Color(200, 0, 0);

        displayTypeLabel.setForeground(entity.isDisplayTypeModified() ? modifiedColor : Color.BLACK);
        iconOverrideLabel.setForeground(entity.isIconOverrideModified() ? modifiedColor : Color.BLACK);
        displayConditionLabel.setForeground(entity.isDisplayConditionModified() ? modifiedColor : Color.BLACK);
        tapActionLabel.setForeground(entity.isTapActionModified() ? modifiedColor : Color.BLACK);
        doubleTapActionLabel.setForeground(entity.isDoubleTapActionModified() ? modifiedColor : Color.BLACK);
        holdActionLabel.setForeground(entity.isHoldActionModified() ? modifiedColor : Color.BLACK);
        positionLabel.setForeground(entity.isPositionModified() ? modifiedColor : Color.BLACK);
        alwaysOnLabel.setForeground(entity.isAlwaysOnModified() ? modifiedColor : Color.BLACK);
        isRgbLabel.setForeground(entity.isIsRgbModified() ? modifiedColor : Color.BLACK);
        opacityLabel.setForeground(entity.isOpacityModified() ? modifiedColor : Color.BLACK);
        backgroundColorLabel.setForeground(entity.isBackgroundColorModified() ? modifiedColor : Color.BLACK);
        displayFurnitureConditionLabel.setForeground(entity.isDisplayFurnitureConditionModified() ? modifiedColor : Color.BLACK);
        openFurnitureConditionLabel.setForeground(entity.isOpenFurnitureConditionModified() ? modifiedColor : Color.BLACK);
    }

    private void showHideComponents() {
        iconOverrideLabel.setVisible((Entity.DisplayType)displayTypeComboBox.getSelectedItem() == Entity.DisplayType.ICON);
        iconOverrideTextField.setVisible((Entity.DisplayType)displayTypeComboBox.getSelectedItem() == Entity.DisplayType.ICON);
        tapActionValueTextField.setVisible((Entity.Action)tapActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
        doubleTapActionValueTextField.setVisible((Entity.Action)doubleTapActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
        holdActionValueTextField.setVisible((Entity.Action)holdActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
        displayFurnitureConditionValueTextField.setVisible((Entity.DisplayFurnitureCondition)displayFurnitureConditionComboBox.getSelectedItem() != Entity.DisplayFurnitureCondition.ALWAYS);
        openFurnitureConditionValueTextField.setVisible((Entity.OpenFurnitureCondition)openFurnitureConditionComboBox.getSelectedItem() != Entity.OpenFurnitureCondition.ALWAYS);

        SwingUtilities.getWindowAncestor(this).pack();
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parentComponent), entity.getName());
        dialog.applyComponentOrientation(parentComponent != null ?
            parentComponent.getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        showHideComponents();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void close() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window.isDisplayable())
            window.dispose();
    }
}
