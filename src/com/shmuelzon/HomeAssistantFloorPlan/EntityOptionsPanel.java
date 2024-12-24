package com.shmuelzon.HomeAssistantFloorPlan;

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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;

public class EntityOptionsPanel extends JPanel {
    private enum ActionType {CLOSE, RESET_TO_DEFAULTS}

    private Controller controller;
    private String entityName;
    private JLabel displayTypeLabel;
    private JComboBox<Controller.EntityDisplayType> displayTypeComboBox;
    private JLabel tapActionLabel;
    private JComboBox<Controller.EntityTapAction> tapActionComboBox;
    private JLabel alwaysOnLabel;
    private JCheckBox alwaysOnCheckbox;
    private JLabel isRgbLabel;
    private JCheckBox isRgbCheckbox;
    private JLabel topPositionLabel;
    private JTextField topPositionTextField;
    private JLabel leftPositionLabel;
    private JTextField leftPositionTextField;
    private JButton closeButton;
    private JButton resetToDefaultsButton;
    private ResourceBundle resource;

    public EntityOptionsPanel(UserPreferences preferences, Controller controller, String entityName, boolean isLight) {
        super(new GridBagLayout());
        this.controller = controller;
        this.entityName = entityName;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault());
        createActions(preferences);
        createComponents();
        layoutComponents(isLight);
    }

    public abstract class SimpleDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }
        public void removeUpdate(DocumentEvent e) {
            update(e);
        }
        public void changedUpdate(DocumentEvent e) {
            update(e);
        }
        public abstract void update(DocumentEvent e);
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
                controller.resetEntitySettings(entityName);
                close();
            }
        });
    }

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        displayTypeLabel = new JLabel();
        displayTypeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.displayTypeLabel.text"));
        displayTypeComboBox = new JComboBox<Controller.EntityDisplayType>(Controller.EntityDisplayType.values());
        displayTypeComboBox.setSelectedItem(controller.getEntityDisplayType(entityName));
        displayTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.displayTypeComboBox.%s.text", ((Controller.EntityDisplayType)o).name())));
                return rendererComponent;
            }
        });
        displayTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setEntityDisplayType(entityName, (Controller.EntityDisplayType)displayTypeComboBox.getSelectedItem());
            }
        });

        tapActionLabel = new JLabel();
        tapActionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.tapActionLabel.text"));
        tapActionComboBox = new JComboBox<Controller.EntityTapAction>(Controller.EntityTapAction.values());
        tapActionComboBox.setSelectedItem(controller.getEntityTapAction(entityName));
        tapActionComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.tapActionComboBox.%s.text", ((Controller.EntityTapAction)o).name())));
                return rendererComponent;
            }
        });
        tapActionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setEntityTapAction(entityName, (Controller.EntityTapAction)tapActionComboBox.getSelectedItem());
            }
        });

        alwaysOnLabel = new JLabel();
        alwaysOnLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.alwaysOnLabel.text"));
        alwaysOnCheckbox = new JCheckBox();
        alwaysOnCheckbox.setSelected(controller.getEntityAlwaysOn(entityName));
        alwaysOnCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setEntityAlwaysOn(entityName, alwaysOnCheckbox.isSelected());
            }
        });

        isRgbLabel = new JLabel();
        isRgbLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.isRgbLabel.text"));
        isRgbCheckbox = new JCheckBox();
        isRgbCheckbox.setSelected(controller.getEntityIsRgb(entityName));
        isRgbCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setEntityIsRgb(entityName, isRgbCheckbox.isSelected());
            }
        });

        topPositionLabel = new JLabel();
        topPositionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.topPosition.text"));
        topPositionTextField = new JTextField(20);
        topPositionTextField.setText(controller.getEntityTopPosition(entityName));
        topPositionTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                String topPosition = topPositionTextField.getText().trim();
                if (!topPosition.isEmpty()) {
                    controller.setEntityTopPosition(entityName, topPosition);
                }
            }
        });

        leftPositionLabel = new JLabel();
        leftPositionLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.leftPosition.text"));
        leftPositionTextField = new JTextField(20);
        leftPositionTextField.setText(controller.getEntityLeftPosition(entityName));
        leftPositionTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                String leftPosition = leftPositionTextField.getText().trim();
                if (!leftPosition.isEmpty()) {
                    controller.setEntityLeftPosition(entityName, leftPosition);
                }
            }
        });

        closeButton = new JButton(actionMap.get(ActionType.CLOSE));
        closeButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.closeButton.text"));

        resetToDefaultsButton = new JButton(actionMap.get(ActionType.RESET_TO_DEFAULTS));
        resetToDefaultsButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.resetToDefaultsButton.text"));
    }

    private void layoutComponents(boolean isLight) {
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
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Tap action */
        add(tapActionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        tapActionLabel.setHorizontalAlignment(labelAlignment);
        add(tapActionComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Top Position */
        add(topPositionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        topPositionLabel.setHorizontalAlignment(labelAlignment);
        add(topPositionTextField, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Left Position */
        add(leftPositionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        leftPositionLabel.setHorizontalAlignment(labelAlignment);
        add(leftPositionTextField, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        if (!isLight)
            return;

        /* Always on */
        add(alwaysOnLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        alwaysOnLabel.setHorizontalAlignment(labelAlignment);
        add(alwaysOnCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Is RGB */
        add(isRgbLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        isRgbLabel.setHorizontalAlignment(labelAlignment);
        add(isRgbCheckbox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = 
            optionPane.createDialog(SwingUtilities.getRootPane(parentComponent), entityName);
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
