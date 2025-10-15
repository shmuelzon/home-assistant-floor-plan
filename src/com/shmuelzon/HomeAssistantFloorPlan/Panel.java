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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.TreeSet;
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

    final TimeZone timeZone = TimeZone.getTimeZone("UTC");
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
    private JLabel rendererLabel;
    private JComboBox<Controller.Renderer> rendererComboBox;
    private JLabel qualityLabel;
    private JComboBox<Controller.Quality> qualityComboBox;
    private JLabel outputDirectoryLabel;
    private JTextField outputDirectoryTextField;
    private JLabel renderTimeLabel;
    private SpinnerDateModel renderTimeModel;
    private JSpinner renderTimeSpinner;
    private JCheckBox nightRenderCheckbox;
    private SpinnerDateModel nightRenderTimeModel;
    private JSpinner nightRenderTimeSpinner;
    private JLabel nightBaseCeilingLightsIntensityLabel;
    private JSpinner nightBaseCeilingLightsIntensitySpinner;
    private JLabel nightBaseOtherLightsIntensityLabel;
    private JSpinner nightBaseOtherLightsIntensitySpinner;
    private JLabel renderCeilingLightsIntensityLabel;
    private JSpinner renderCeilingLightsIntensitySpinner;
    private JLabel renderOtherLightsIntensityLabel;
    private JSpinner renderOtherLightsIntensitySpinner;

    private JLabel imageFormatLabel;
    private JComboBox<Controller.ImageFormat> imageFormatComboBox;
    private JButton outputDirectoryBrowseButton;
    private FileContentManager outputDirectoryChooser;
    private JCheckBox useExistingRendersCheckbox;
    private JCheckBox enableFloorPlanPostProcessingCheckbox;
    private JCheckBox maintainAspectRatioCheckbox;
    private JLabel transparencyThresholdLabel;
    private JSpinner transparencyThresholdSpinner;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton closeButton;

    private class EntityNode {
        public Entity entity;
        public List<String> attributes;

        public EntityNode(Entity entity) {
            this.entity = entity;
        }

        @Override
        public String toString() {
            List<String> attributes = attributesList();
            if (attributes.size() == 0)
                return entity.getName();
            return entity.getName() + " " + attributes.toString();
        }

        private List<String> attributesList() {
            attributes = new ArrayList<>();

            if (entity.getAlwaysOn())
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.alwaysOn.text"));
            if (entity.getIsRgb())
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.isRgb.text"));
            if (entity.getDisplayFurnitureCondition() != Entity.DisplayFurnitureCondition.ALWAYS)
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.displayByState.text"));
            if (entity.getOpenFurnitureCondition() != Entity.OpenFurnitureCondition.ALWAYS)
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.openByState.text"));

            return attributes;
        }
    }

    public Panel(UserPreferences preferences, ClassLoader classLoader, Controller controller) {
        super(new GridBagLayout());
        this.preferences = preferences;
        this.controller = controller;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault(), classLoader);
        createActions();
        createComponents();
        layoutComponents();
    }

    private void createActions() {
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

    private JTree createTree(String rootName) {
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
                if (!tree.isEnabled())
                    return;

                TreePath selectedPath = tree.getSelectionPath();
                if (selectedPath == null)
                    return;

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
                if (!node.isLeaf()) {
                    tree.clearSelection();
                    return;
                }

                EntityNode entityNode = (EntityNode)node.getUserObject();
                openEntityOptionsPanel(entityNode.entity);
            }
        });
        tree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!tree.isEnabled())
                    return;

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

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        renderCeilingLightsIntensityLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.renderCeilingLightsIntensityLabel.text"));
        renderCeilingLightsIntensityLabel.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.renderCeilingLightsIntensityLabel.tooltip"));
        final SpinnerNumberModel renderCeilingLightsIntensitySpinnerModel = new SpinnerNumberModel(20, 0, 100, 1);
        renderCeilingLightsIntensitySpinner = new AutoCommitSpinner(renderCeilingLightsIntensitySpinnerModel);
        JSpinner.NumberEditor renderCeilingLightsIntensityEditor = new JSpinner.NumberEditor(renderCeilingLightsIntensitySpinner, "0");
        ((JSpinner.DefaultEditor)renderCeilingLightsIntensityEditor).getTextField().setColumns(4);
        renderCeilingLightsIntensitySpinner.setEditor(renderCeilingLightsIntensityEditor);
        renderCeilingLightsIntensitySpinnerModel.setValue(controller.getRenderCeilingLightsIntensity());
        renderCeilingLightsIntensitySpinner.addChangeListener(e -> controller.setRenderCeilingLightsIntensity((int) renderCeilingLightsIntensitySpinnerModel.getValue()));

        renderOtherLightsIntensityLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.renderOtherLightsIntensityLabel.text"));
        renderOtherLightsIntensityLabel.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.renderOtherLightsIntensityLabel.tooltip"));
        final SpinnerNumberModel renderOtherLightsIntensitySpinnerModel = new SpinnerNumberModel(10, 0, 100, 1);
        renderOtherLightsIntensitySpinner = new AutoCommitSpinner(renderOtherLightsIntensitySpinnerModel);
        JSpinner.NumberEditor renderOtherLightsIntensityEditor = new JSpinner.NumberEditor(renderOtherLightsIntensitySpinner, "0");
        ((JSpinner.DefaultEditor)renderOtherLightsIntensityEditor).getTextField().setColumns(4);
        renderOtherLightsIntensitySpinner.setEditor(renderOtherLightsIntensityEditor);
        renderOtherLightsIntensitySpinnerModel.setValue(controller.getRenderOtherLightsIntensity());
        renderOtherLightsIntensitySpinner.addChangeListener(e -> controller.setRenderOtherLightsIntensity((int) renderOtherLightsIntensitySpinnerModel.getValue()));

        detectedLightsLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTreeLabel.text"));
        detectedLightsTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTree.root.text"));
        buildEntitiesGroupsTree(detectedLightsTree, controller.getLightsGroups());

        otherEntitiesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTreeLabel.text"));
        otherEntitiesTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTree.root.text"));
        Map<String, List<Entity>> otherEntitiesGroupedByType = controller.getOtherEntities().stream()
            .collect(Collectors.groupingBy(entity -> entity.getName().split("\\.")[0]));
        buildEntitiesGroupsTree(otherEntitiesTree, otherEntitiesGroupedByType);

        PropertyChangeListener updateTreeOnProperyChanged = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                buildEntitiesGroupsTree(detectedLightsTree, controller.getLightsGroups());
                buildEntitiesGroupsTree(otherEntitiesTree, otherEntitiesGroupedByType);
            }
        };
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, updateTreeOnProperyChanged);
        for (Entity light : controller.getLightEntities()) {
            light.addPropertyChangeListener(Entity.Property.ALWAYS_ON, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.IS_RGB, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.DISPLAY_FURNITURE_CONDITION, updateTreeOnProperyChanged);
        }

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

        List<Long> renderingTimes = controller.getRenderDateTimes();
        renderTimeLabel = new JLabel();
        renderTimeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.renderTimeLabel.text"));
        ChangeListener renderTimeChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                List<Long> renderingTimes = new ArrayList<>();
                LocalDate date = ((Date)renderTimeSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalDate();
                long timestamp = ((Date)renderTimeSpinner.getValue()).getTime();

                renderingTimes.add(timestamp);
                if (nightRenderTimeSpinner.isVisible()) {
                    LocalTime nightTime = ((Date)nightRenderTimeSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalTime();
                    long nightTimestamp = date.atTime(nightTime).atZone(timeZone.toZoneId()).toInstant().toEpochMilli();
                    if (timestamp != nightTimestamp)
                        renderingTimes.add(nightTimestamp);
                }

                controller.setRenderDateTimes(new ArrayList<>(renderingTimes));
            }
        };
        renderTimeModel = new SpinnerDateModel();
        renderTimeSpinner = new JSpinner(renderTimeModel);
        final JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(renderTimeSpinner);
        timeEditor.getFormat().setTimeZone(timeZone);
        timeEditor.getFormat().applyPattern("dd/MM/yyyy HH:mm");
        timeEditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        renderTimeSpinner.setEditor(timeEditor);
        final DateFormatter timeFormatter = (DateFormatter)timeEditor.getTextField().getFormatter();
        timeFormatter.setAllowsInvalid(false);
        timeFormatter.setOverwriteMode(true);
        renderTimeModel.setValue(new Date(renderingTimes.get(0)));
        renderTimeSpinner.addChangeListener(renderTimeChangeListener);
        nightRenderCheckbox = new JCheckBox();
        nightRenderCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.nightRender.text"));
        nightRenderCheckbox.setBorder(null);
        nightRenderCheckbox.setSelected(renderingTimes.size() > 1);
        nightRenderCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                nightRenderTimeSpinner.setVisible(nightRenderCheckbox.isSelected());
                nightBaseCeilingLightsIntensitySpinner.setVisible(nightRenderCheckbox.isSelected());
                nightBaseOtherLightsIntensitySpinner.setVisible(nightRenderCheckbox.isSelected());
                renderTimeChangeListener.stateChanged(null);
            }
        });
        nightRenderTimeModel = new SpinnerDateModel();
        nightRenderTimeSpinner = new JSpinner(nightRenderTimeModel);
        final JSpinner.DateEditor nightTimeEditor = new JSpinner.DateEditor(nightRenderTimeSpinner);
        nightTimeEditor.getFormat().setTimeZone(timeZone);
        nightTimeEditor.getFormat().applyPattern("HH:mm");
        nightTimeEditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        nightRenderTimeSpinner.setEditor(nightTimeEditor);
        final DateFormatter nightTimeFormatter = (DateFormatter)nightTimeEditor.getTextField().getFormatter();
        nightTimeFormatter.setAllowsInvalid(false);
        nightTimeFormatter.setOverwriteMode(true);
        nightRenderTimeModel.setValue(new Date(renderingTimes.get(renderingTimes.size() - 1)));
        nightRenderTimeSpinner.setVisible(nightRenderCheckbox.isSelected());
        nightRenderTimeSpinner.addChangeListener(renderTimeChangeListener);

        nightBaseCeilingLightsIntensityLabel = new JLabel();
        nightBaseCeilingLightsIntensityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.nightBaseCeilingLightsIntensityLabel.text"));
        nightBaseCeilingLightsIntensityLabel.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.nightBaseCeilingLightsIntensityLabel.tooltip"));
        final SpinnerNumberModel nightBaseCeilingLightsIntensitySpinnerModel = new SpinnerNumberModel(8, 0, 100, 1);
        nightBaseCeilingLightsIntensitySpinner = new AutoCommitSpinner(nightBaseCeilingLightsIntensitySpinnerModel);
        JSpinner.NumberEditor nightBaseCeilingLightsIntensityEditor = new JSpinner.NumberEditor(nightBaseCeilingLightsIntensitySpinner, "0");
        ((JSpinner.DefaultEditor)nightBaseCeilingLightsIntensityEditor).getTextField().setColumns(4);
        nightBaseCeilingLightsIntensitySpinner.setEditor(nightBaseCeilingLightsIntensityEditor);
        nightBaseCeilingLightsIntensitySpinnerModel.setValue(controller.getNightBaseCeilingLightsIntensity());
        nightBaseCeilingLightsIntensitySpinner.setVisible(nightRenderCheckbox.isSelected());
        nightBaseCeilingLightsIntensitySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                controller.setNightBaseCeilingLightsIntensity(((Number)nightBaseCeilingLightsIntensitySpinnerModel.getValue()).intValue());
            }
        });

        nightBaseOtherLightsIntensityLabel = new JLabel();
        nightBaseOtherLightsIntensityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.nightBaseOtherLightsIntensityLabel.text"));
        nightBaseOtherLightsIntensityLabel.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.nightBaseOtherLightsIntensityLabel.tooltip"));
        final SpinnerNumberModel nightBaseOtherLightsIntensitySpinnerModel = new SpinnerNumberModel(3, 0, 100, 1);
        nightBaseOtherLightsIntensitySpinner = new AutoCommitSpinner(nightBaseOtherLightsIntensitySpinnerModel);
        JSpinner.NumberEditor nightBaseOtherLightsIntensityEditor = new JSpinner.NumberEditor(nightBaseOtherLightsIntensitySpinner, "0");
        ((JSpinner.DefaultEditor)nightBaseOtherLightsIntensityEditor).getTextField().setColumns(4);
        nightBaseOtherLightsIntensitySpinner.setEditor(nightBaseOtherLightsIntensityEditor);
        nightBaseOtherLightsIntensitySpinnerModel.setValue(controller.getNightBaseOtherLightsIntensity());
        nightBaseOtherLightsIntensitySpinner.setVisible(nightRenderCheckbox.isSelected());
        nightBaseOtherLightsIntensitySpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                controller.setNightBaseOtherLightsIntensity(((Number)nightBaseOtherLightsIntensitySpinnerModel.getValue()).intValue());
            }
        });

        imageFormatLabel = new JLabel();
        imageFormatLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.imageFormatLabel.text"));
        imageFormatComboBox = new JComboBox<Controller.ImageFormat>(Controller.ImageFormat.values());
        imageFormatComboBox.setSelectedItem(controller.getImageFormat());
        imageFormatComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.imageFormatComboBox.%s.text", ((Controller.ImageFormat)o).name())));
                return rendererComponent;
            }
        });
        imageFormatComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                controller.setImageFormat((Controller.ImageFormat)imageFormatComboBox.getSelectedItem());
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

        enableFloorPlanPostProcessingCheckbox = new JCheckBox();
        enableFloorPlanPostProcessingCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.enableFloorPlanPostProcessing.text"));
        enableFloorPlanPostProcessingCheckbox.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.enableFloorPlanPostProcessing.tooltip"));
        enableFloorPlanPostProcessingCheckbox.setSelected(controller.getEnableFloorPlanPostProcessing());
        enableFloorPlanPostProcessingCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                controller.setEnableFloorPlanPostProcessing(enableFloorPlanPostProcessingCheckbox.isSelected());
                showHidePostProcessingOptions();
            }
        });

        maintainAspectRatioCheckbox = new JCheckBox();
        maintainAspectRatioCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.maintainAspectRatio.text"));
        maintainAspectRatioCheckbox.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.maintainAspectRatio.tooltip"));
        maintainAspectRatioCheckbox.setSelected(controller.getMaintainAspectRatio());
        maintainAspectRatioCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                controller.setMaintainAspectRatio(maintainAspectRatioCheckbox.isSelected());
            }
        });

        transparencyThresholdLabel = new JLabel();
        transparencyThresholdLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.transparencyThresholdLabel.text"));
        final SpinnerNumberModel transparencyThresholdSpinnerModel = new SpinnerNumberModel(30, 0, 255, 1);
        transparencyThresholdSpinner = new AutoCommitSpinner(transparencyThresholdSpinnerModel);
        transparencyThresholdSpinnerModel.setValue(controller.getTransparencyThreshold());
        transparencyThresholdSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              controller.setTransparencyThreshold(((Number)transparencyThresholdSpinner.getValue()).intValue());
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
        detectedLightsTree.setEnabled(enabled);
        otherEntitiesTree.setEnabled(enabled);
        widthSpinner.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);
        renderCeilingLightsIntensitySpinner.setEnabled(enabled);
        renderOtherLightsIntensitySpinner.setEnabled(enabled);
        rendererComboBox.setEnabled(enabled);
        qualityComboBox.setEnabled(enabled);
        renderTimeSpinner.setEnabled(enabled);
        nightRenderCheckbox.setEnabled(enabled);
        nightRenderTimeSpinner.setEnabled(enabled);
        nightBaseCeilingLightsIntensitySpinner.setEnabled(enabled);
        nightBaseOtherLightsIntensitySpinner.setEnabled(enabled);
        imageFormatComboBox.setEnabled(enabled);
        outputDirectoryTextField.setEnabled(enabled);
        outputDirectoryBrowseButton.setEnabled(enabled);
        useExistingRendersCheckbox.setEnabled(enabled);
        enableFloorPlanPostProcessingCheckbox.setEnabled(enabled);
        transparencyThresholdSpinner.setEnabled(enabled);
        maintainAspectRatioCheckbox.setEnabled(enabled);
        if (enabled) {
            startButton.setAction(getActionMap().get(ActionType.START));
            startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.startButton.text"));
        } else {
            startButton.setAction(getActionMap().get(ActionType.STOP));
            startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.stopButton.text"));
        }
        showHidePostProcessingOptions();
    }

    private void showHidePostProcessingOptions() {
        boolean postProcessingEnabled = enableFloorPlanPostProcessingCheckbox.isSelected();
        maintainAspectRatioCheckbox.setVisible(postProcessingEnabled);
        transparencyThresholdLabel.setVisible(postProcessingEnabled);
        transparencyThresholdSpinner.setVisible(postProcessingEnabled);
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

        JPanel generalSettingsPanel = new JPanel(new GridBagLayout());
        generalSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resource.getString("HomeAssistantFloorPlan.Panel.generalSettingsSection.title")));
        add(generalSettingsPanel, new GridBagConstraints(0, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;
        int generalSettingsPanelGridYIndex = 0;

        generalSettingsPanel.add(widthLabel, new GridBagConstraints(
            0, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(widthSpinner, new GridBagConstraints(
            1, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(heightLabel, new GridBagConstraints(
            2, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(heightSpinner, new GridBagConstraints(
            3, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanelGridYIndex++;

        generalSettingsPanel.add(rendererLabel, new GridBagConstraints(
            0, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(rendererComboBox, new GridBagConstraints(
            1, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(qualityLabel, new GridBagConstraints(
            2, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(qualityComboBox, new GridBagConstraints(
            3, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanelGridYIndex++;

        generalSettingsPanel.add(renderTimeLabel, new GridBagConstraints(
            0, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(renderTimeSpinner, new GridBagConstraints(
            1, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanelGridYIndex++;

        generalSettingsPanel.add(imageFormatLabel, new GridBagConstraints(
            0, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(imageFormatComboBox, new GridBagConstraints(
            1, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanelGridYIndex++;

        generalSettingsPanel.add(transparencyThresholdLabel, new GridBagConstraints(
            0, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanel.add(transparencyThresholdSpinner, new GridBagConstraints(
            1, generalSettingsPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        generalSettingsPanelGridYIndex++;


        JPanel renderImagesPanel = new JPanel(new GridBagLayout());
        renderImagesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resource.getString("HomeAssistantFloorPlan.Panel.renderImagesSection.title")));
        add(renderImagesPanel, new GridBagConstraints(0, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        int renderImagesPanelGridYIndex = 0;

        renderImagesPanel.add(renderCeilingLightsIntensityLabel, new GridBagConstraints(
            0, renderImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        renderImagesPanel.add(renderCeilingLightsIntensitySpinner, new GridBagConstraints(
            1, renderImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        renderImagesPanel.add(renderOtherLightsIntensityLabel, new GridBagConstraints(
            2, renderImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        renderImagesPanel.add(renderOtherLightsIntensitySpinner, new GridBagConstraints(
            3, renderImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        renderImagesPanelGridYIndex++;


        JPanel baseImagesPanel = new JPanel(new GridBagLayout());
        baseImagesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(resource.getString("HomeAssistantFloorPlan.Panel.baseImagesSection.title")));
        add(baseImagesPanel, new GridBagConstraints(0, currentGridYIndex, 4, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        int baseImagesPanelGridYIndex = 0;

        baseImagesPanel.add(nightRenderCheckbox, new GridBagConstraints(
            0, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanel.add(nightRenderTimeSpinner, new GridBagConstraints(
            1, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanelGridYIndex++;

        baseImagesPanel.add(nightBaseCeilingLightsIntensityLabel, new GridBagConstraints(
            0, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanel.add(nightBaseCeilingLightsIntensitySpinner, new GridBagConstraints(
            1, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanel.add(nightBaseOtherLightsIntensityLabel, new GridBagConstraints(
            2, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanel.add(nightBaseOtherLightsIntensitySpinner, new GridBagConstraints(
            3, baseImagesPanelGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        baseImagesPanelGridYIndex++;

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
        add(enableFloorPlanPostProcessingCheckbox, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Post-processing options */
        add(maintainAspectRatioCheckbox, new GridBagConstraints(
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

    private void buildEntitiesGroupsTree(JTree tree, Map<String, List<Entity>> entityGroups) {
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        root.removeAllChildren();
        model.reload();

        for (String group : new TreeSet<String>(entityGroups.keySet())) {
            DefaultMutableTreeNode groupNode;
            if (entityGroups.get(group).size() != 1 || entityGroups.get(group).get(0).getName() != group)
            {
                groupNode = new DefaultMutableTreeNode(group);
                for (Entity light : new TreeSet<>(entityGroups.get(group)))
                    groupNode.add(new DefaultMutableTreeNode(new EntityNode(light)));
            }
            else
                groupNode = new DefaultMutableTreeNode(new EntityNode(entityGroups.get(group).get(0)));
            model.insertNodeInto(groupNode, root, root.getChildCount());
        }

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void openEntityOptionsPanel(Entity entity) {
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, entity, controller);
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
