package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.eteks.sweethome3d.model.HomeLight;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.AutoCommitSpinner;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.ResourceAction;
import com.eteks.sweethome3d.swing.SwingTools;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.ContentManager;
import com.eteks.sweethome3d.viewcontroller.DialogView;
import com.eteks.sweethome3d.viewcontroller.View;

@SuppressWarnings("serial")
public class Panel extends JPanel implements DialogView {
    private enum ActionType {BROWSE, START, STOP, CLOSE}

    private static Panel currentPanel;
    private Controller controller;
    private ResourceBundle resource;
    private ExecutorService renderExecutor;
    private JLabel detectedLightsLabel;
    private JTree detectedLightsTree;
    private JLabel widthLabel;
    private JSpinner widthSpinner;
    private JLabel heightLabel;
    private JSpinner heightSpinner;
    private JLabel lightMixingModeLabel;
    private JComboBox<Controller.LightMixingMode> lightMixingModeComboBox;
    private JLabel sensitivityLabel;
    private JSpinner sensitivitySpinner;
    private JLabel rendererLabel;
    private JComboBox<Controller.Renderer> rendererComboBox;
    private JLabel qualityLabel;
    private JComboBox<Controller.Quality> qualityComboBox;
    private JLabel outputDirectoryLabel;
    private JTextField outputDirectoryTextField;
    private JButton outputDirectoryBrowseButton;
    private FileContentManager outputDirectoryChooser;
    private JCheckBox useExistingRendersCheckbox;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton closeButton;

    public Panel(UserPreferences preferences, ClassLoader classLoader, Controller controller) {
        super(new GridBagLayout());
        this.controller = controller;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault(), classLoader);
        createActions(preferences);
        createComponents(preferences, this.controller.getLightsGroups());
        layoutComponents();
    }

    private void createActions(UserPreferences preferences) {
        final ActionMap actions = getActionMap();
        actions.put(ActionType.BROWSE, new ResourceAction(preferences, Panel.class, ActionType.BROWSE.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                showBrowseDialog();
            }
        });
        actions.put(ActionType.START, new ResourceAction(preferences, Panel.class, ActionType.START.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                renderExecutor = Executors.newSingleThreadExecutor();
                renderExecutor.execute(new Runnable() {
                    public void run() {
                        setComponentsEnabled(false);
                        try {
                            controller.render();
                            JOptionPane.showMessageDialog(null, resource.getString("HomeAssistantFloorPlan.Panel.info.finishedRendering.text"));
                        } catch (InterruptedException e) {
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(null, resource.getString("HomeAssistantFloorPlan.Panel.error.failedRendering.text") + " " + e);
                        }
                        setComponentsEnabled(true);
                        renderExecutor = null;
                    }
                });
            }
        });
        actions.put(ActionType.STOP, new ResourceAction(preferences, Panel.class, ActionType.STOP.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                stop();
            }
        });
        actions.put(ActionType.CLOSE, new ResourceAction(preferences, Panel.class, ActionType.CLOSE.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                stop();
                close();
            }
        });
    }

    private void showBrowseDialog() {
        final String selectedDirectory =
        outputDirectoryChooser.showSaveDialog(this, resource.getString("HomeAssistantFloorPlan.Panel.outputDirectory.title"), ContentManager.ContentType.PHOTOS_DIRECTORY, outputDirectoryTextField.getText());
        if (selectedDirectory != null)
            outputDirectoryTextField.setText(selectedDirectory);
    }

    private void createComponents(UserPreferences preferences, Map<String, Map<String, List<HomeLight>>> lightsGroups) {
        final ActionMap actionMap = getActionMap();

        detectedLightsLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTreeLabel.text"));
        detectedLightsTree = new JTree(new DefaultMutableTreeNode(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTree.root.text"))) {
            @Override
            protected void setExpandedState(TreePath path, boolean state) {
                if (state) {
                    super.setExpandedState(path, state);
                }
            }
        };
        buildLightsGroupsTree(lightsGroups);
        controller.addPropertyChangeListener(Controller.Property.LIGHT_MIXING_MODE, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                buildLightsGroupsTree(controller.getLightsGroups());
                detectedLightsTree.repaint();
                SwingUtilities.getWindowAncestor(Panel.this).pack();
            }
        });
        detectedLightsTree.putClientProperty("JTree.lineStyle", "Angled");
        detectedLightsTree.setUI(new BasicTreeUI() {
            @Override
            protected boolean shouldPaintExpandControl(TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
                return false;
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer)detectedLightsTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        detectedLightsTree.setBorder(LineBorder.createGrayLineBorder());

        widthLabel = new JLabel();
        widthLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.widthLabel.text"));
        final SpinnerNumberModel widthSpinnerModel = new SpinnerNumberModel(1024, 10, 10000, 10);
        widthSpinner = new AutoCommitSpinner(widthSpinnerModel);
        widthSpinnerModel.setValue(controller.getRenderWidth());
        widthSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                controller.setRenderWidth(((Number)widthSpinnerModel.getValue()).intValue());
            }
        });

        heightLabel = new JLabel();
        heightLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.heightLabel.text"));
        final SpinnerNumberModel heightSpinnerModel = new SpinnerNumberModel(576, 10, 10000, 10);
        heightSpinner = new AutoCommitSpinner(heightSpinnerModel);
        heightSpinnerModel.setValue(controller.getRenderHeight());
        heightSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              controller.setRenderHeight(((Number)heightSpinnerModel.getValue()).intValue());
            }
        });

        lightMixingModeLabel = new JLabel();
        lightMixingModeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.lightMixingModeLabel.text"));
        lightMixingModeLabel.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.lightMixingModeLabel.tooltip"));
        lightMixingModeComboBox = new JComboBox<Controller.LightMixingMode>(Controller.LightMixingMode.values());
        lightMixingModeComboBox.setSelectedItem(controller.getLightMixingMode());
        lightMixingModeComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.lightMixingModeComboBox.%s.text", ((Controller.LightMixingMode)o).name())));
                return rendererComponent;
            }
        });
        lightMixingModeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setLightMixingMode((Controller.LightMixingMode)lightMixingModeComboBox.getSelectedItem());
            }
        });

        sensitivityLabel = new JLabel();
        sensitivityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.sensitivityLabel.text"));
        final SpinnerNumberModel sensitivitySpinnerModel = new SpinnerNumberModel(15, 0, 100, 1);
        sensitivitySpinner = new AutoCommitSpinner(sensitivitySpinnerModel);
        sensitivitySpinnerModel.setValue(controller.getSensitivity());
        sensitivitySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              controller.setSensitivity(((Number)sensitivitySpinner.getValue()).intValue());
            }
        });

        rendererLabel = new JLabel();
        rendererLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.rendererLabel.text"));
        rendererComboBox = new JComboBox<Controller.Renderer>(Controller.Renderer.values());
        rendererComboBox.setSelectedItem(controller.getRenderer());
        rendererComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.rendererComboBox.%s.text", ((Controller.Renderer)o).name())));
                return rendererComponent;
            }
        });
        rendererComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setRenderer((Controller.Renderer)rendererComboBox.getSelectedItem());
            }
        });

        qualityLabel = new JLabel();
        qualityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.qualityLabel.text"));
        qualityComboBox = new JComboBox<Controller.Quality>(Controller.Quality.values());
        qualityComboBox.setSelectedItem(controller.getQuality());
        qualityComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.qualityComboBox.%s.text", ((Controller.Quality)o).name())));
                return rendererComponent;
            }
        });
        qualityComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setQuality((Controller.Quality)qualityComboBox.getSelectedItem());
            }
        });

        outputDirectoryLabel = new JLabel();
        outputDirectoryLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.outputDirectoryLabel.text"));
        outputDirectoryTextField = new JTextField();
        outputDirectoryTextField.setText(controller.getOutputDirectory());
        outputDirectoryTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                startButton.setEnabled(!outputDirectoryTextField.getText().isEmpty());
                controller.setOutputDirectory(outputDirectoryTextField.getText());
            }
            public void removeUpdate(DocumentEvent e) {
                startButton.setEnabled(!outputDirectoryTextField.getText().isEmpty());
                controller.setOutputDirectory(outputDirectoryTextField.getText());
            }
            public void changedUpdate(DocumentEvent e) {
                startButton.setEnabled(!outputDirectoryTextField.getText().isEmpty());
                controller.setOutputDirectory(outputDirectoryTextField.getText());
            }
        });
        outputDirectoryBrowseButton = new JButton(actionMap.get(ActionType.BROWSE));
        outputDirectoryBrowseButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.browseButton.text"));
        outputDirectoryChooser = new FileContentManager(preferences);

        useExistingRendersCheckbox = new JCheckBox();
        useExistingRendersCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.useExistingRenders.text"));
        useExistingRendersCheckbox.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.useExistingRenders.tooltip"));
        useExistingRendersCheckbox.setSelected(controller.getUserExistingRenders());
        useExistingRendersCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
              controller.setUserExistingRenders(useExistingRendersCheckbox.isSelected());
            }
        });

        progressBar = new JProgressBar() {
            @Override
            public String getString() {
                return String.format("%d/%d", getValue(), getMaximum());
            }
        };
        progressBar.setStringPainted(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(controller.getNumberOfTotalRenders());
        controller.addPropertyChangeListener(Controller.Property.COMPLETED_RENDERS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                progressBar.setValue(((Number)ev.getNewValue()).intValue());
            }
        });
        controller.addPropertyChangeListener(Controller.Property.LIGHT_MIXING_MODE, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                progressBar.setMaximum(controller.getNumberOfTotalRenders());
                progressBar.setValue(0);
                progressBar.repaint();
            }
        });

        startButton = new JButton(actionMap.get(ActionType.START));
        startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.startButton.text"));
        startButton.setEnabled(!outputDirectoryTextField.getText().isEmpty());
        closeButton = new JButton(actionMap.get(ActionType.CLOSE));
        closeButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.closeButton.text"));
    }

    private void setComponentsEnabled(boolean enabled) {
        widthSpinner.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);
        lightMixingModeComboBox.setEnabled(enabled);
        sensitivitySpinner.setEnabled(enabled);
        rendererComboBox.setEnabled(enabled);
        qualityComboBox.setEnabled(enabled);
        outputDirectoryTextField.setEnabled(enabled);
        outputDirectoryBrowseButton.setEnabled(enabled);
        if (enabled) {
            startButton.setAction(getActionMap().get(ActionType.START));
            startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.startButton.text"));
        } else {
            startButton.setAction(getActionMap().get(ActionType.STOP));
            startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.stopButton.text"));
        }
    }

    private void layoutComponents() {
        int labelAlignment = OperatingSystem.isMacOSX() ? JLabel.TRAILING : JLabel.LEADING;
        int standardGap = Math.round(2 * SwingTools.getResolutionScale());
        Insets insets = new Insets(0, standardGap, 0, standardGap);

        /* First row (Detected lights caption) */
        add(detectedLightsLabel, new GridBagConstraints(
            0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Second row (Detected lights tree) */
        add(detectedLightsTree, new GridBagConstraints(
            0, 1, 4, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Third row (Resolution) */
        add(widthLabel, new GridBagConstraints(
            0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        widthLabel.setHorizontalAlignment(labelAlignment);
        add(widthSpinner, new GridBagConstraints(
            1, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(heightLabel, new GridBagConstraints(
            2, 2, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        heightLabel.setHorizontalAlignment(labelAlignment);
        add(heightSpinner, new GridBagConstraints(
            3, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Fourth row (Light mixing mode and sensitivity) */
        add(lightMixingModeLabel, new GridBagConstraints(
            0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(lightMixingModeComboBox, new GridBagConstraints(
            1, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivityLabel, new GridBagConstraints(
            2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivitySpinner, new GridBagConstraints(
            3, 3, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Fifth row (Renderer + Quality) */
        add(rendererLabel, new GridBagConstraints(
            0, 4, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(rendererComboBox, new GridBagConstraints(
            1, 4, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityLabel, new GridBagConstraints(
            2, 4, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityComboBox, new GridBagConstraints(
            3, 4, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Sixth row (Output directory) */
        add(outputDirectoryLabel, new GridBagConstraints(
            0, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryTextField, new GridBagConstraints(
            1, 5, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryBrowseButton, new GridBagConstraints(
            3, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Seventh row (Options) */
        add(useExistingRendersCheckbox, new GridBagConstraints(
            0, 6, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));

        /* Eighth row (progress bar) */
        add(progressBar, new GridBagConstraints(
            0, 7, 4, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
    }

    public void displayView(View parentView) {
        if (controller.isProjectEmpty()) {
            JOptionPane.showMessageDialog(null, resource.getString("HomeAssistantFloorPlan.Panel.error.emptyProject.text"),
                resource.getString("HomeAssistantFloorPlan.Panel.error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (currentPanel == this) {
            SwingUtilities.getWindowAncestor(Panel.this).toFront();
            return;
        } 
        if (currentPanel != null)
            currentPanel.close();
        final JOptionPane optionPane = new JOptionPane(this,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                null, new Object [] {startButton, closeButton}, startButton);
        final JDialog dialog =
        optionPane.createDialog(SwingUtilities.getRootPane((Component)parentView), resource.getString("HomeAssistantFloorPlan.Plugin.NAME"));
        dialog.applyComponentOrientation(parentView != null ?
            ((JComponent)parentView).getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        dialog.setModal(false);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent ev) {
                currentPanel = null;
            }
            @Override
            public void windowClosing(WindowEvent ev) {
                stop();
            }
        });

        dialog.setVisible(true);
        currentPanel = this;
    }

    private void buildLightsGroupsTree(Map<String, Map<String, List<HomeLight>>> lightsGroups) {
        DefaultTreeModel model = (DefaultTreeModel)detectedLightsTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        root.removeAllChildren();
        model.reload();

        for (String group : lightsGroups.keySet()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
            if (lightsGroups.get(group).size() != 1 || lightsGroups.get(group).get(group) == null)
            {
                for (String lightName : lightsGroups.get(group).keySet())
                    groupNode.add(new DefaultMutableTreeNode(lightName));
            }
            model.insertNodeInto(groupNode, root, root.getChildCount());
        }

        for (int i = 0; i < detectedLightsTree.getRowCount(); i++) {
            detectedLightsTree.expandRow(i);
        }
    }

    private void stop() {
        if (renderExecutor != null)
            renderExecutor.shutdownNow();
        controller.stop();
    }

    private void close() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window.isDisplayable())
            window.dispose();
    }
};
