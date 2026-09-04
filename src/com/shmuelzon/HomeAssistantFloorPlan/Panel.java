package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import java.io.File;
import java.io.IOException;
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
    private enum ActionType {BROWSE, OPEN, ADVANCED_OPTIONS, START, STOP, CLOSE}

    private static final String MAC_OS_DIRECTORY_DIALOG_PROPERTY = "apple.awt.fileDialogForDirectories";

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
    private JLabel lightMixingModeLabel;
    private JComboBox<Controller.LightMixingMode> lightMixingModeComboBox;
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

    private JLabel imageFormatLabel;
    private JComboBox<Controller.ImageFormat> imageFormatComboBox;
    private JButton outputDirectoryBrowseButton;
    private JButton outputDirectoryOpenButton;
    private JPanel outputDirectoryButtonsPanel;
    private FileContentManager outputDirectoryChooser;
    private JCheckBox useExistingRendersCheckbox;
    private JButton advancedOptionsButton;
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
        actions.put(ActionType.OPEN, new ResourceAction(preferences, Panel.class, ActionType.OPEN.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                openOutputDirectory();
            }
        });
        actions.put(ActionType.ADVANCED_OPTIONS, new ResourceAction(preferences, Panel.class, ActionType.ADVANCED_OPTIONS.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                new AdvancedOptionsPanel(preferences, controller).displayView(Panel.this);
            }
        });
        actions.put(ActionType.START, new ResourceAction(preferences, Panel.class, ActionType.START.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                if (!checkOutputDirectoryAccessible())
                    return;
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
        final String title = resource.getString("HomeAssistantFloorPlan.Panel.outputDirectory.title");
        final String selectedDirectory = OperatingSystem.isMacOSX()
            ? showNativeDirectoryDialog(title, outputDirectoryTextField.getText())
            : outputDirectoryChooser.showSaveDialog(this, title, ContentManager.ContentType.PHOTOS_DIRECTORY, outputDirectoryTextField.getText());
        if (selectedDirectory != null)
            outputDirectoryTextField.setText(selectedDirectory);
    }

    /* MacOS needs the native dialog box to allow access outside of the sandbox */
    private String showNativeDirectoryDialog(String title, String currentDirectory) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        FileDialog fileDialog = owner instanceof Frame ? new FileDialog((Frame)owner)
                              : owner instanceof Dialog ? new FileDialog((Dialog)owner)
                              : new FileDialog((Frame)null);
        fileDialog.setTitle(title);
        fileDialog.setMode(FileDialog.LOAD);
        if (currentDirectory != null && !currentDirectory.isEmpty())
            fileDialog.setDirectory(currentDirectory);

        String previousProperty = System.getProperty(MAC_OS_DIRECTORY_DIALOG_PROPERTY);
        System.setProperty(MAC_OS_DIRECTORY_DIALOG_PROPERTY, "true");
        try {
            fileDialog.setVisible(true);
        } finally {
            if (previousProperty != null)
                System.setProperty(MAC_OS_DIRECTORY_DIALOG_PROPERTY, previousProperty);
            else
                System.clearProperty(MAC_OS_DIRECTORY_DIALOG_PROPERTY);
        }

        if (fileDialog.getDirectory() == null || fileDialog.getFile() == null)
            return null;
        return new File(fileDialog.getDirectory(), fileDialog.getFile()).getAbsolutePath();
    }

    private boolean checkOutputDirectoryAccessible() {
        if (controller.isOutputDirectoryWritable())
            return true;

        JOptionPane.showMessageDialog(null, resource.getString("HomeAssistantFloorPlan.Panel.error.outputDirectoryNotAccessible.text"),
            resource.getString("HomeAssistantFloorPlan.Panel.error.title"), JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void openOutputDirectory() {
        if (!controller.isOutputDirectorySet() || !checkOutputDirectoryAccessible())
            return;

        File outputDirectory = new File(controller.getOutputDirectory());
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                throw new IOException(outputDirectory.toString());
            Desktop.getDesktop().open(outputDirectory);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                resource.getString("HomeAssistantFloorPlan.Panel.error.failedOpeningOutputDirectory.text") + " " + e,
                resource.getString("HomeAssistantFloorPlan.Panel.error.title"), JOptionPane.ERROR_MESSAGE);
        }
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

        advancedOptionsButton = new JButton(actionMap.get(ActionType.ADVANCED_OPTIONS));
        advancedOptionsButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.advancedOptionsButton.text"));
        advancedOptionsButton.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.advancedOptionsButton.tooltip"));
        advancedOptionsButton.setMargin(new Insets(0, 4, 0, 4));

        outputDirectoryLabel = new JLabel();
        outputDirectoryLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.outputDirectoryLabel.text"));
        outputDirectoryTextField = new JTextField(20);
        outputDirectoryTextField.setEditable(false);
        outputDirectoryTextField.setText(controller.getOutputDirectory());
        outputDirectoryTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                controller.setOutputDirectory(outputDirectoryTextField.getText());
                updateOutputDirectoryDependentButtons();
            }
        });
        outputDirectoryBrowseButton = new JButton(actionMap.get(ActionType.BROWSE));
        outputDirectoryBrowseButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.browseButton.text"));
        outputDirectoryOpenButton = new JButton(actionMap.get(ActionType.OPEN));
        outputDirectoryOpenButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.openButton.text"));
        outputDirectoryButtonsPanel = new JPanel(new GridLayout(1, 2, 3, 0));
        outputDirectoryButtonsPanel.add(outputDirectoryBrowseButton);
        outputDirectoryButtonsPanel.add(outputDirectoryOpenButton);
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
        updateOutputDirectoryDependentButtons();
        closeButton = new JButton(actionMap.get(ActionType.CLOSE));
        closeButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.closeButton.text"));
    }

    private void updateOutputDirectoryDependentButtons() {
        startButton.setEnabled(controller.isOutputDirectorySet());
        outputDirectoryOpenButton.setEnabled(controller.isOutputDirectorySet());
    }

    private void setComponentsEnabled(boolean enabled) {
        detectedLightsTree.setEnabled(enabled);
        otherEntitiesTree.setEnabled(enabled);
        widthSpinner.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);
        lightMixingModeComboBox.setEnabled(enabled);
        rendererComboBox.setEnabled(enabled);
        qualityComboBox.setEnabled(enabled);
        renderTimeSpinner.setEnabled(enabled);
        nightRenderCheckbox.setEnabled(enabled);
        nightRenderTimeSpinner.setEnabled(enabled);
        imageFormatComboBox.setEnabled(enabled);
        outputDirectoryTextField.setEnabled(enabled);
        outputDirectoryBrowseButton.setEnabled(enabled);
        useExistingRendersCheckbox.setEnabled(enabled);
        advancedOptionsButton.setEnabled(enabled);
        if (enabled) {
            startButton.setAction(getActionMap().get(ActionType.START));
            startButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.startButton.text"));
            updateOutputDirectoryDependentButtons();
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

        /* Light mixing mode + render time */
        add(lightMixingModeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(lightMixingModeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(renderTimeLabel, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(renderTimeSpinner, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Renderer + Night render*/
        add(rendererLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(rendererComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(nightRenderCheckbox, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(nightRenderTimeSpinner, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Image format + Quality */
        add(imageFormatLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(imageFormatComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityLabel, new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityComboBox, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Output directory */
        add(outputDirectoryLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryButtonsPanel, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Options */
        add(useExistingRendersCheckbox, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(advancedOptionsButton, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_END,
            GridBagConstraints.NONE, insets, 0, 0));
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
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, entity);
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
