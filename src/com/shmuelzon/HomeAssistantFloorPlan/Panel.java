package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
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
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
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
    private JLabel detectedLightsLabel;
    private JTree detectedLightsTree;
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

    private JTree createTree(String rootName, final boolean isLight) {
        final JTree tree = new JTree(new DefaultMutableTreeNode(rootName)) {
            @Override
            protected void setExpandedState(TreePath path, boolean state) {
                if (state) {
                    super.setExpandedState(path, state);
                }
            }
        };
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent event) {
                TreePath selectedPath = tree.getSelectionPath();
                if (selectedPath == null)
                    return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
                if (!node.isLeaf()) {
                    tree.clearSelection();
                    return;
                }

                EntityNode entityNode = (EntityNode)node.getUserObject();
                openEntityOptionsPanel(entityNode.name, isLight);
            }
        });
        tree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());

                if (path != null && ((DefaultMutableTreeNode)path.getLastPathComponent()).isLeaf()) {
                    tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    tree.setSelectionPath(path);
                } else {
                    tree.setCursor(Cursor.getDefaultCursor());
                    tree.clearSelection();
                }
            }
        });
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.setUI(new BasicTreeUI() {
            @Override
            protected boolean shouldPaintExpandControl(TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
                return false;
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer)tree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        tree.setBorder(LineBorder.createGrayLineBorder());
        tree.setVisibleRowCount(20);

        return tree;
    }

    private void createComponents(UserPreferences preferences, Map<String, Map<String, List<HomeLight>>> lightsGroups) {
        final ActionMap actionMap = getActionMap();

        detectedLightsLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTreeLabel.text"));
        detectedLightsTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTree.root.text"), true);
        buildLightsGroupsTree(lightsGroups);
        PropertyChangeListener updateTreeOnProperyChanged = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                buildLightsGroupsTree(controller.getLightsGroups());
            }
        };
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, updateTreeOnProperyChanged);
        controller.addPropertyChangeListener(Controller.Property.ENTITY_ATTRIBUTE_CHANGED, updateTreeOnProperyChanged);

        otherEntitiesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTreeLabel.text"));
        otherEntitiesTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTree.root.text"), false);
        buildOtherEntitiesGroupsTree(controller.getHomeAssistantEntities());
        PropertyChangeListener updateEntitiesTreeOnProperyChanged = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                buildOtherEntitiesGroupsTree(controller.getHomeAssistantEntities());
            }
        };
        controller.addPropertyChangeListener(Controller.Property.ENTITY_ATTRIBUTE_CHANGED, updateEntitiesTreeOnProperyChanged);

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
        outputDirectoryTextField = new JTextField(20);
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

        /* Detected entities captions */
        add(detectedLightsLabel, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(otherEntitiesLabel, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Detected entities trees */
        JScrollPane detectedLightsScrollPane = new JScrollPane(detectedLightsTree);
        detectedLightsScrollPane.setPreferredSize(new Dimension(275, 350));
        add(detectedLightsScrollPane, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        JScrollPane otherEntitiesScrollPane = new JScrollPane(otherEntitiesTree);
        otherEntitiesScrollPane.setPreferredSize(new Dimension(275, 350));
        add(otherEntitiesScrollPane, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

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
        if (controller.getEntityIsRgb(lightName))
            attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.isRgb.text"));

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

    private void buildOtherEntitiesGroupsTree(List<HomePieceOfFurniture> otherEntities) {
        DefaultTreeModel model = (DefaultTreeModel)otherEntitiesTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        root.removeAllChildren();
        model.reload();

        for (HomePieceOfFurniture entity : otherEntities ) {
            String entityName = entity.getName();
            String type = entityName.split("\\.")[0];

            DefaultMutableTreeNode typeGroup = null;
            for (int i = 0; i < root.getChildCount(); i++) {
                if (root.getChildAt(i).toString().equals(type)) {
                    typeGroup = (DefaultMutableTreeNode)root.getChildAt(i);
                    break;
                }
            }

            if (typeGroup == null) {
                typeGroup = new DefaultMutableTreeNode(type);
                model.insertNodeInto(typeGroup, root, root.getChildCount());
            }

            typeGroup.add(new DefaultMutableTreeNode(generateLightEntityNode(entityName)));
        }

        for (int i = 0; i < otherEntitiesTree.getRowCount(); i++) {
            otherEntitiesTree.expandRow(i);
        }
    }

    /* Method to open the new panel */
    private void openEntityOptionsPanel(String entityName, boolean isLight) {
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, controller, entityName, isLight);
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
