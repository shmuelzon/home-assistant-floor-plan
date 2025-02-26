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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;

import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.AutoCommitSpinner;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;

public class EntityOptionsPanel extends JPanel {
    private enum ActionType {CLOSE, RESET_TO_DEFAULTS}

    private Entity entity;
    private JLabel displayTypeLabel;
    private JComboBox<Entity.DisplayType> displayTypeComboBox;
    private JLabel displayConditionLabel;
    private JComboBox<Entity.DisplayCondition> displayConditionComboBox;
    private JLabel tapActionLabel;
    private JComboBox<Entity.Action> tapActionComboBox;
    private JLabel doubleTapActionLabel;
    private JComboBox<Entity.Action> doubleTapActionComboBox;
    private JLabel holdActionLabel;
    private JComboBox<Entity.Action> holdActionComboBox;
    private JLabel positionLabel;
    private JLabel positionLeftLabel;
    private JSpinner positionLeftSpinner;
    private JLabel positionTopLabel;
    private JSpinner positionTopSpinner;
    private JLabel opacityLabel;
    private JSpinner opacitySpinner;
    private JLabel alwaysOnLabel;
    private JCheckBox alwaysOnCheckbox;
    private JLabel isRgbLabel;
    private JCheckBox isRgbCheckbox;
    private JButton closeButton;
    private JButton resetToDefaultsButton;
    private ResourceBundle resource;

    public EntityOptionsPanel(UserPreferences preferences, Entity entity) {
        super(new GridBagLayout());
        this.entity = entity;

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
                entity.setDisplayType((Entity.DisplayType)displayTypeComboBox.getSelectedItem());
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
        currentGridYIndex++;

        /* Double tap action */
        add(doubleTapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        doubleTapActionLabel.setHorizontalAlignment(labelAlignment);
        add(doubleTapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
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

        if (!entity.getIsLight())
            return;

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

    private void markModified() {
        Color modifiedColor = new Color(200, 0, 0);

        displayTypeLabel.setForeground(entity.isDisplayTypeModified() ? modifiedColor : Color.BLACK);
        displayConditionLabel.setForeground(entity.isDisplayConditionModified() ? modifiedColor : Color.BLACK);
        tapActionLabel.setForeground(entity.isTapActionModified() ? modifiedColor : Color.BLACK);
        doubleTapActionLabel.setForeground(entity.isDoubleTapActionModified() ? modifiedColor : Color.BLACK);
        holdActionLabel.setForeground(entity.isHoldActionModified() ? modifiedColor : Color.BLACK);
        positionLabel.setForeground(entity.isPositionModified() ? modifiedColor : Color.BLACK);
        alwaysOnLabel.setForeground(entity.isAlwaysOnModified() ? modifiedColor : Color.BLACK);
        isRgbLabel.setForeground(entity.isIsRgbModified() ? modifiedColor : Color.BLACK);
        opacityLabel.setForeground(entity.isOpacityModified() ? modifiedColor : Color.BLACK);
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parentComponent), entity.getName());
        dialog.applyComponentOrientation(parentComponent != null ?
            parentComponent.getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void close() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window.isDisplayable())
            window.dispose();
    }
}
