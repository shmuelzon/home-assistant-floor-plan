package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
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
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.DateFormatter;
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
    private UserPreferences preferences;
    private Controller controller;
    private ResourceBundle resource;
    private ExecutorService renderExecutor;
    private JTabbedPane tabbedPane;
    private JLabel lightsTab;
    private JLabel detectedLightsLabel;
    private JTree detectedLightsTree;
    private JLabel otherEntitiesTab;
    private JLabel otherEntitiesLabel;
    private JTree otherEntitiesTree;
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
    private JLabel renderTimeLabel;
    private JSpinner renderTimeSpinner;
    private JButton outputDirectoryBrowseButton;
    private FileContentManager outputDirectoryChooser;
    private JCheckBox useExistingRendersCheckbox;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton closeButton;

    private class EntityNode {
        public String name;
        public List<String> attributes;

        public EntityNode(String name, List<String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            if (attributes.size() == 0)
                return name;
            return name + " " + attributes.toString();
        }
    }

    public Panel(UserPreferences preferences, ClassLoader classLoader, Controller controller) {
        super(new GridBagLayout());
        this.preferences = preferences;
        this.controller = controller;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault(), classLoader);
        createActions(preferences);
        createComponents(preferences, this.controller.getLightsGroups());
        layoutComponents();
    }

    /* Method to handle mouse listeners for both detectedLightsTree and otherEntitiesTree */
    private void createListeners() {
        java.awt.event.MouseListener treeMouseListener = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                JTree sourceTree = (JTree) event.getSource();
                TreePath selectedPath = sourceTree.getSelectionPath();
                if (selectedPath == null)
                    return;
    
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                if (!node.isLeaf()) {
                    sourceTree.clearSelection();
                    return;
                }
    
                EntityNode entityNode = (EntityNode) node.getUserObject();
                openEntityOptionsPanel(entityNode.name);
            }
        };
    
        java.awt.event.MouseMotionListener treeMouseMotionListener = new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                JTree sourceTree = (JTree) e.getSource();
                TreePath path = sourceTree.getPathForLocation(e.getX(), e.getY());
    
                if (path != null && ((DefaultMutableTreeNode) path.getLastPathComponent()).isLeaf()) {
                    sourceTree.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    sourceTree.setSelectionPath(path);
                } else {
                    sourceTree.setCursor(java.awt.Cursor.getDefaultCursor());
                    sourceTree.clearSelection();
                }
            }
        };
    
        detectedLightsTree.addMouseListener(treeMouseListener);
        detectedLightsTree.addMouseMotionListener(treeMouseMotionListener);
        otherEntitiesTree.addMouseListener(treeMouseListener);
        otherEntitiesTree.addMouseMotionListener(treeMouseMotionListener);
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
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                setComponentsEnabled(true);
                                renderExecutor = null;
                            }
                        });
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

        tabbedPane = new JTabbedPane();
        lightsTab = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.lightsTab.text"));
        detectedLightsLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTreeLabel.text"));
        otherEntitiesTab = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTab.text"));
        otherEntitiesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTreeLabel.text"));        
        detectedLightsTree = new JTree(new DefaultMutableTreeNode(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTree.root.text"))) {
            @Override
            protected void setExpandedState(TreePath path, boolean state) {
                if (state) {
                    super.setExpandedState(path, state);
                }
            }
        };
        /* Other entities tree (for the "Other Entities" tab) */
        otherEntitiesTree = new JTree(new DefaultMutableTreeNode(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTree.root.text")));

        /* Call to create listeners after initializing trees */
        createListeners();

        buildLightsGroupsTree(lightsGroups);
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                buildLightsGroupsTree(controller.getLightsGroups());
                detectedLightsTree.repaint();
                otherEntitiesTree.repaint();
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
        otherEntitiesTree.putClientProperty("JTree.lineStyle", "Angled");
        otherEntitiesTree.setUI(new BasicTreeUI() {
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

        DefaultTreeCellRenderer otherRenderer = (DefaultTreeCellRenderer)otherEntitiesTree.getCellRenderer();
        otherRenderer.setLeafIcon(null);
        otherRenderer.setOpenIcon(null);
        otherRenderer.setClosedIcon(null);
        otherEntitiesTree.setBorder(LineBorder.createGrayLineBorder());        

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

        renderTimeLabel = new JLabel();
        renderTimeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.renderTimeLabel.text"));
        final SpinnerDateModel model = new SpinnerDateModel();
        renderTimeSpinner = new JSpinner(model);
        final JSpinner.DateEditor editor = new JSpinner.DateEditor(renderTimeSpinner);
        editor.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
        editor.getFormat().applyPattern("HH:mm dd/MM/yyyy");
        renderTimeSpinner.setEditor(editor);
        final DateFormatter formatter = (DateFormatter)editor.getTextField().getFormatter();
        formatter.setAllowsInvalid(false);
        formatter.setOverwriteMode(true);
        model.setValue(new Date(controller.getRenderDateTime()));
        renderTimeSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                controller.setRenderDateTime(((Date) renderTimeSpinner.getValue()).getTime());
            }
        });

        useExistingRendersCheckbox = new JCheckBox();
        useExistingRendersCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.useExistingRenders.text"));
        useExistingRendersCheckbox.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.useExistingRenders.tooltip"));
        useExistingRendersCheckbox.setSelected(controller.getUserExistingRenders());
        useExistingRendersCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                controller.setUserExistingRenders(useExistingRendersCheckbox.isSelected());
            }
        });

        outputDirectoryLabel = new JLabel();
        outputDirectoryLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.outputDirectoryLabel.text"));
        outputDirectoryTextField = new JTextField();
        outputDirectoryTextField.setText(controller.getOutputDirectory());
        outputDirectoryTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                startButton.setEnabled(!outputDirectoryTextField.getText().isEmpty());
                controller.setOutputDirectory(outputDirectoryTextField.getText());
            }
        });
        outputDirectoryBrowseButton = new JButton(actionMap.get(ActionType.BROWSE));
        outputDirectoryBrowseButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.browseButton.text"));
        outputDirectoryChooser = new FileContentManager(preferences);

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
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, new PropertyChangeListener() {
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
        int currentGridYIndex = 0;
        int lightsEntitiesGridYIndex = 0;
        int otherEntitiesGridYIndex = 0;
    
        /* Add tabbedPane to the main panel */
        add(tabbedPane, new GridBagConstraints(
            0, currentGridYIndex, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));
        currentGridYIndex++;
    
        /* First tab: Lights */
        JPanel lightsPanel = new JPanel(new GridBagLayout());
        tabbedPane.addTab(lightsTab.getText(), lightsPanel);
    
        /* Detected lights caption */
        lightsPanel.add(detectedLightsLabel, new GridBagConstraints(
            0, lightsEntitiesGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        lightsEntitiesGridYIndex++;

        /* Detected lights tree */
        lightsPanel.add(detectedLightsTree, new GridBagConstraints(
            0, lightsEntitiesGridYIndex, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));
    
        /* Second tab: Other Entities */
        JPanel otherEntitiesPanel = new JPanel(new GridBagLayout());
        tabbedPane.addTab(otherEntitiesTab.getText(), otherEntitiesPanel);
    
        /* Other entities caption */
        otherEntitiesPanel.add(otherEntitiesLabel, new GridBagConstraints(
            0, otherEntitiesGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        otherEntitiesGridYIndex++;
    
        /* Other entities tree */
        otherEntitiesPanel.add(otherEntitiesTree, new GridBagConstraints(
            0, otherEntitiesGridYIndex, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));

        /* Resolution */
        add(widthLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        widthLabel.setHorizontalAlignment(labelAlignment);
        add(widthSpinner, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(heightLabel, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        heightLabel.setHorizontalAlignment(labelAlignment);
        add(heightSpinner, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Light mixing mode and sensitivity */
        add(lightMixingModeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(lightMixingModeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivityLabel, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivitySpinner, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Renderer + Quality */
        add(rendererLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(rendererComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityLabel, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityComboBox, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Time selection */
        add(renderTimeLabel, new GridBagConstraints(
                0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(renderTimeSpinner, new GridBagConstraints(
                1, currentGridYIndex, 3, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Output directory */
        add(outputDirectoryLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryBrowseButton, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Options */
        add(useExistingRendersCheckbox, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Progress bar */
        add(progressBar, new GridBagConstraints(
            0, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.CENTER,
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

    private EntityNode generateLightEntityNode(String lightName) {
        List<String> attributes = new ArrayList<String>();

        if (controller.getEntityAlwaysOn(lightName))
            attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.alwaysOn.text"));

        return new EntityNode(lightName, attributes);
    }

    private void buildLightsGroupsTree(Map<String, Map<String, List<HomeLight>>> lightsGroups) {
        DefaultTreeModel model = (DefaultTreeModel)detectedLightsTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        root.removeAllChildren();
        model.reload();

        for (String group : lightsGroups.keySet()) {
            DefaultMutableTreeNode groupNode;
            if (lightsGroups.get(group).size() != 1 || lightsGroups.get(group).get(group) == null)
            {
                groupNode = new DefaultMutableTreeNode(group);
                for (String lightName : lightsGroups.get(group).keySet())
                    groupNode.add(new DefaultMutableTreeNode(generateLightEntityNode(lightName)));
            }
            else
                groupNode = new DefaultMutableTreeNode(generateLightEntityNode(group));
            model.insertNodeInto(groupNode, root, root.getChildCount());
        }

        for (int i = 0; i < detectedLightsTree.getRowCount(); i++) {
            detectedLightsTree.expandRow(i);
        }
    }

    /* Method to open the new panel */
    private void openEntityOptionsPanel(String entityName) {
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, controller, entityName);
        entityOptionsPanel.displayView(this);
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
