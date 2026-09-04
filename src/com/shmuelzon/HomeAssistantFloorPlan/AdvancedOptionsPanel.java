package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.AutoCommitSpinner;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;

public class AdvancedOptionsPanel extends JPanel {
    private enum ActionType {CLOSE, RESET_TO_DEFAULTS}

    private Controller controller;
    private ResourceBundle resource;
    private JLabel sensitivityLabel;
    private JSpinner sensitivitySpinner;
    private JLabel homeAssistantPathLabel;
    private JTextField homeAssistantPathTextField;
    private JCheckBox addImageVersionTagsCheckbox;
    private JButton closeButton;
    private JButton resetToDefaultsButton;

    public AdvancedOptionsPanel(UserPreferences preferences, Controller controller) {
        super(new GridBagLayout());
        this.controller = controller;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault());
        createActions(preferences);
        createComponents();
        layoutComponents();
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
                controller.resetAdvancedOptionsToDefaults();
                close();
            }
        });
    }

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        sensitivityLabel = new JLabel();
        sensitivityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.sensitivityLabel.text"));
        final SpinnerNumberModel sensitivitySpinnerModel = new SpinnerNumberModel(10, 0, 100, 1);
        sensitivitySpinner = new AutoCommitSpinner(sensitivitySpinnerModel);
        sensitivitySpinnerModel.setValue(controller.getSensitivity());
        sensitivitySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                controller.setSensitivity(((Number)sensitivitySpinner.getValue()).intValue());
            }
        });

        homeAssistantPathLabel = new JLabel();
        homeAssistantPathLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.homeAssistantPathLabel.text"));
        homeAssistantPathTextField = new JTextField(25);
        homeAssistantPathTextField.setText(controller.getHomeAssistantPath());
        homeAssistantPathTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                controller.setHomeAssistantPath(homeAssistantPathTextField.getText());
            }
        });

        addImageVersionTagsCheckbox = new JCheckBox();
        addImageVersionTagsCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.addImageVersionTags.text"));
        addImageVersionTagsCheckbox.setSelected(controller.getAddImageVersionTags());
        addImageVersionTagsCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                controller.setAddImageVersionTags(addImageVersionTagsCheckbox.isSelected());
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

        add(sensitivityLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        sensitivityLabel.setHorizontalAlignment(labelAlignment);
        add(sensitivitySpinner, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(homeAssistantPathLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        homeAssistantPathLabel.setHorizontalAlignment(labelAlignment);
        add(homeAssistantPathTextField, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(addImageVersionTagsCheckbox, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
    }

    public void displayView(Component parentComponent) {
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object [] {closeButton, resetToDefaultsButton}, closeButton);
        final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parentComponent),
            resource.getString("HomeAssistantFloorPlan.Panel.advancedOptions.title"));
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
