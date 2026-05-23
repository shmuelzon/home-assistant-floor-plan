package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.BorderLayout;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
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
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;

import com.eteks.sweethome3d.model.Camera;
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
    private JTextField sh3dSearchField;
    private JLabel otherEntitiesLabel;
    private JTree otherEntitiesTree;
    private JTextField haSearchField;
    private JLabel cameraLabel;
    private JComboBox<com.eteks.sweethome3d.model.Camera> cameraComboBox;
    private JLabel resolutionLabel;
    private JTextField resolutionTextField;
    private JLabel lightMixingModeLabel;
    private JComboBox<Controller.LightMixingMode> lightMixingModeComboBox;
    private JLabel sensitivityLabel;
    private JSpinner sensitivitySpinner;
    private JLabel rendererLabel;
    private JComboBox<Controller.Renderer> rendererComboBox;
    private JLabel qualityLabel;
    private JComboBox<Controller.Quality> qualityComboBox;
    private JLabel baseFolderLabel;
    private JTextField baseFolderTextField;
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
    private FileContentManager outputDirectoryChooser;
    private JLabel haUrlLabel;
    private JComboBox<String> haUrlProtocolComboBox;
    private JTextField haUrlTextField;
    private JLabel haEntityCountLabel;
    private JLabel haLastFetchedLabel;
    private JButton haLoginButton;
    private JLabel haTokenStatusLabel;
    private JButton fetchEntitiesButton;
    private JButton showEntitiesButton;
    private JCheckBox useExistingRendersCheckbox;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton closeButton;
    private JButton checkEntitiesButton;
    private JCheckBox sh3dAccordionCheckbox;
    private JCheckBox haAccordionCheckbox;

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
                Camera selectedCamera = (Camera) cameraComboBox.getSelectedItem();
                if (selectedCamera == null || (selectedCamera.getName() == null || selectedCamera.getName().isEmpty())) {
                    int ok = JOptionPane.showConfirmDialog(Panel.this,
                        "<html>No named camera perspective is selected.<br>" +
                        "Using an unnamed camera may produce unexpected results.<br><br>" +
                        "Continue anyway?</html>",
                        resource.getString("HomeAssistantFloorPlan.Plugin.NAME"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (ok != JOptionPane.YES_OPTION) return;
                }
                renderExecutor = Executors.newSingleThreadExecutor();
                final JDialog progressWindow = showRenderProgressWindow();
                renderExecutor.execute(new Runnable() {
                    public void run() {
                        setComponentsEnabled(false);
                        try {
                            controller.render();
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    progressWindow.dispose();
                                    setComponentsEnabled(true);
                                    renderExecutor = null;
                                    int choice = JOptionPane.showConfirmDialog(Panel.this,
                                        resource.getString("HomeAssistantFloorPlan.Panel.info.finishedRendering.text")
                                            + "\n" + resource.getString("HomeAssistantFloorPlan.Panel.info.openFolder.text"),
                                        resource.getString("HomeAssistantFloorPlan.Plugin.NAME"),
                                        JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                                    if (choice == JOptionPane.YES_OPTION) {
                                        try {
                                            java.awt.Desktop.getDesktop().open(new java.io.File(controller.getEffectiveOutputDirectory()));
                                        } catch (Exception ex) {
                                            JOptionPane.showMessageDialog(Panel.this,
                                                "Could not open folder: " + ex.getMessage(),
                                                resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                                                JOptionPane.ERROR_MESSAGE);
                                        }
                                    }
                                }
                            });
                        } catch (InterruptedException e) {
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    progressWindow.dispose();
                                    setComponentsEnabled(true);
                                    renderExecutor = null;
                                }
                            });
                        } catch (Exception e) {
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    progressWindow.dispose();
                                    setComponentsEnabled(true);
                                    renderExecutor = null;
                                    JOptionPane.showMessageDialog(Panel.this,
                                        resource.getString("HomeAssistantFloorPlan.Panel.error.failedRendering.text") + " " + e,
                                        resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
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
        };
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            private void handleEvent(java.awt.event.MouseEvent event) {
                TreePath path = tree.getPathForLocation(event.getX(), event.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
                if (!node.isLeaf()) return;
                if (!(node.getUserObject() instanceof EntityNode)) return;
                EntityNode entityNode = (EntityNode)node.getUserObject();

                if (event.isPopupTrigger()) {
                    tree.setSelectionPath(path);
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem copyItem = new JMenuItem("Copy to clipboard");
                    copyItem.addActionListener(e -> copyToClipboard(entityNode.entity.getName()));
                    menu.add(copyItem);
                    menu.addSeparator();
                    JMenuItem renameItem = new JMenuItem("Rename...");
                    renameItem.addActionListener(e -> controller.openFurnitureProperties(entityNode.entity));
                    JMenuItem optionsItem = new JMenuItem("Options...");
                    optionsItem.addActionListener(e -> openEntityOptionsPanel(entityNode.entity));
                    menu.add(renameItem);

                    java.util.List<String> haIds = controller.getCachedHaEntityIds();
                    if (!haIds.isEmpty() && !haIds.contains(entityNode.entity.getName())) {
                        java.util.List<String> suggestions = controller.findSimilarEntities(entityNode.entity.getName(), 3);
                        if (!suggestions.isEmpty()) {
                            JMenu suggestMenu = new JMenu("Did you mean?");
                            for (String suggestion : suggestions) {
                                JMenuItem item = new JMenuItem(suggestion);
                                item.addActionListener(e -> confirmAndRename(tree, path, entityNode.entity, suggestion));
                                suggestMenu.add(item);
                            }
                            menu.add(suggestMenu);
                        }
                    }

                    menu.add(optionsItem);
                    menu.show(tree, event.getX(), event.getY());
                } else if (event.getClickCount() == 2 && tree.isEnabled()) {
                    openEntityOptionsPanel(entityNode.entity);
                }
            }
            public void mousePressed(java.awt.event.MouseEvent event) { handleEvent(event); }
            public void mouseReleased(java.awt.event.MouseEvent event) { handleEvent(event); }
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
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            {
                // Use empty icon instead of null to avoid NullPointerException in newer Java versions
                javax.swing.Icon emptyIcon = new javax.swing.Icon() {
                    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {}
                    public int getIconWidth() { return 0; }
                    public int getIconHeight() { return 0; }
                };
                setLeafIcon(emptyIcon);
                setOpenIcon(emptyIcon);
                setClosedIcon(emptyIcon);
            }
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, hasFocus);
                if (leaf && value instanceof DefaultMutableTreeNode) {
                    Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObj instanceof EntityNode) {
                        java.util.List<String> haIds = controller.getCachedHaEntityIds();
                        if (!haIds.isEmpty()) {
                            String entityName = ((EntityNode) userObj).entity.getName();
                            setForeground(haIds.contains(entityName)
                                ? new java.awt.Color(0, 140, 0)
                                : new java.awt.Color(200, 100, 0));
                        }
                    }
                }
                return this;
            }
        });
        tree.setBorder(LineBorder.createGrayLineBorder());
        tree.setVisibleRowCount(20);

        return tree;
    }

    private void createComponents() {
        final ActionMap actionMap = getActionMap();

        detectedLightsLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTreeLabel.text"));
        detectedLightsTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.detectedLightsTree.root.text"));
        {
            List<Entity> allEntities = new ArrayList<>(controller.getLightEntities());
            allEntities.addAll(controller.getOtherEntities());
            buildEntitiesGroupsTree(detectedLightsTree, allEntities.stream()
                .collect(Collectors.groupingBy(e -> e.getName().split("\\.")[0])));
        }

        sh3dSearchField = createSearchField("Enter search term", () -> refreshTrees());

        otherEntitiesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTreeLabel.text"));
        otherEntitiesTree = createTree(resource.getString("HomeAssistantFloorPlan.Panel.otherEntitiesTree.root.text"));
        buildEntitiesGroupsTree(otherEntitiesTree, new java.util.HashMap<>());
        otherEntitiesTree.addMouseListener(new java.awt.event.MouseAdapter() {
            private void handleEvent(java.awt.event.MouseEvent event) {
                if (!event.isPopupTrigger()) return;
                TreePath path = otherEntitiesTree.getPathForLocation(event.getX(), event.getY());
                if (path == null) return;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (!node.isLeaf()) return;
                Object userObj = node.getUserObject();
                String entityId = (userObj instanceof HaEntityNode)
                    ? ((HaEntityNode) userObj).entityId : userObj.toString();
                otherEntitiesTree.setSelectionPath(path);
                JPopupMenu menu = new JPopupMenu();
                JMenuItem copyItem = new JMenuItem("Copy to clipboard");
                copyItem.addActionListener(e -> copyToClipboard(entityId));
                menu.add(copyItem);
                menu.show(otherEntitiesTree, event.getX(), event.getY());
            }
            public void mousePressed(java.awt.event.MouseEvent event) { handleEvent(event); }
            public void mouseReleased(java.awt.event.MouseEvent event) { handleEvent(event); }
        });
        otherEntitiesTree.setCellRenderer(new DefaultTreeCellRenderer() {
            {
                // Use empty icon instead of null to avoid NullPointerException in newer Java versions
                javax.swing.Icon emptyIcon = new javax.swing.Icon() {
                    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {}
                    public int getIconWidth() { return 0; }
                    public int getIconHeight() { return 0; }
                };
                setLeafIcon(emptyIcon);
                setOpenIcon(emptyIcon);
                setClosedIcon(emptyIcon);
            }
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, hasFocus);
                if (leaf && value instanceof DefaultMutableTreeNode) {
                    Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObj instanceof HaEntityNode && !sel) {
                        setForeground(((HaEntityNode) userObj).matched
                            ? new java.awt.Color(0, 140, 0)
                            : new java.awt.Color(200, 100, 0));
                    }
                }
                return this;
            }
        });

        haSearchField = createSearchField("Enter search term", () -> checkEntities());

        PropertyChangeListener updateTreeOnProperyChanged = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                refreshTrees();
            }
        };
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, updateTreeOnProperyChanged);
        for (Entity light : controller.getLightEntities()) {
            light.addPropertyChangeListener(Entity.Property.ALWAYS_ON, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.IS_RGB, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.DISPLAY_FURNITURE_CONDITION, updateTreeOnProperyChanged);
        }

        // Populate right tree immediately if cached entities exist
        if (!controller.getCachedHaEntityIds().isEmpty())
            checkEntities();

        cameraLabel = new JLabel("Camera:");
        cameraComboBox = new JComboBox<>(controller.getAvailableCameras().toArray(new Camera[0]));
        cameraComboBox.setSelectedItem(controller.getSelectedCamera());
        cameraComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                super.getListCellRendererComponent(jList, o, i, b, b1);
                if (o instanceof Camera) {
                    Camera cam = (Camera) o;
                    String name = cam.getName();
                    setText(name != null && !name.isEmpty() ? name : "(unnamed camera)");
                }
                return this;
            }
        });
        cameraComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Camera selected = (Camera) cameraComboBox.getSelectedItem();
                if (selected != null)
                    controller.setSelectedCamera(selected);
            }
        });

        resolutionLabel = new JLabel("Resolution:");
        resolutionTextField = new JTextField(controller.getRenderWidth() + " x " + controller.getRenderHeight(), 12);
        resolutionTextField.addActionListener(e -> applyResolution());
        resolutionTextField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { applyResolution(); }
        });

        lightMixingModeLabel = new JLabel();
        lightMixingModeLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.lightMixingModeLabel.text"));
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
        java.text.SimpleDateFormat localDtFmt = (java.text.SimpleDateFormat)
            java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, Locale.getDefault());
        timeEditor.getFormat().applyPattern(localDtFmt.toPattern());
        timeEditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        renderTimeSpinner.setEditor(timeEditor);
        final DateFormatter timeFormatter = (DateFormatter)timeEditor.getTextField().getFormatter();
        timeFormatter.setAllowsInvalid(false);
        timeFormatter.setOverwriteMode(true);
        renderTimeModel.setValue(new Date(renderingTimes.get(0)));
        renderTimeSpinner.addChangeListener(renderTimeChangeListener);
        nightRenderCheckbox = new JCheckBox();
        nightRenderCheckbox.setText(resource.getString("HomeAssistantFloorPlan.Panel.nightRender.text"));
        nightRenderCheckbox.setToolTipText("<html>Add a second render pass at a different time-of-day.<br>Use this to generate a night-time floor plan for HA dark mode.</html>");
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

        baseFolderLabel = new JLabel();
        baseFolderLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.baseFolderLabel.text"));
        baseFolderTextField = new JTextField(20);
        baseFolderTextField.setText(controller.getBaseFolder());
        baseFolderTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                controller.setBaseFolder(baseFolderTextField.getText());
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
        outputDirectoryOpenButton = new JButton("Open");
        outputDirectoryOpenButton.setToolTipText("Open output directory in file manager");
        outputDirectoryOpenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String dir = controller.getEffectiveOutputDirectory();
                if (dir.isEmpty()) return;
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(dir));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(Panel.this,
                        "Could not open directory: " + ex.getMessage(),
                        resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        outputDirectoryChooser = new FileContentManager(preferences);

        haUrlLabel = new JLabel();
        haUrlLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.haUrlLabel.text"));
        haUrlProtocolComboBox = new JComboBox<>(new String[]{"https://", "http://"});
        haUrlProtocolComboBox.setPrototypeDisplayValue("https://");
        String savedUrl = controller.getHaUrl();
        String savedProtocol = savedUrl.startsWith("http://") ? "http://" : "https://";
        String savedHost = savedUrl.replaceFirst("^https?://", "");
        haUrlProtocolComboBox.setSelectedItem(savedProtocol);
        haUrlTextField = new JTextField(20);
        haUrlTextField.setText(savedHost);
        SimpleDocumentListener haUrlListener = new SimpleDocumentListener() {
            @Override
            public void executeUpdate(DocumentEvent e) {
                controller.setHaUrl((String)haUrlProtocolComboBox.getSelectedItem() + haUrlTextField.getText());
            }
        };
        haUrlTextField.getDocument().addDocumentListener(haUrlListener);
        haUrlProtocolComboBox.addActionListener(e ->
            controller.setHaUrl((String)haUrlProtocolComboBox.getSelectedItem() + haUrlTextField.getText()));

        haLoginButton = new JButton();
        haLoginButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.text"));
        haLoginButton.setToolTipText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.tooltip"));
        haLoginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                haLoginButton.setEnabled(false);
                haLoginButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.waiting.text"));
                try {
                    controller.startOAuthFlow(new Controller.OAuthCallback() {
                        public void onSuccess(String accessToken) {
                            EventQueue.invokeLater(() -> {
                                haLoginButton.setEnabled(true);
                                haLoginButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.text"));
                                updateTokenStatusLabel();
                                triggerFetchEntities(true);
                            });
                        }
                        public void onError(String message) {
                            EventQueue.invokeLater(() -> {
                                haLoginButton.setEnabled(true);
                                haLoginButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.text"));
                                JOptionPane.showMessageDialog(Panel.this,
                                    resource.getString("HomeAssistantFloorPlan.Panel.error.oauthFailed.text") + "\n" + message,
                                    resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    });
                } catch (Exception ex2) {
                    haLoginButton.setEnabled(true);
                    haLoginButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.haLoginButton.text"));
                    if ("HTTPS_TOKEN_REQUIRED".equals(ex2.getMessage())) {
                        JOptionPane.showMessageDialog(Panel.this,
                            "<html>Your HA uses HTTPS — OAuth with localhost redirect is not supported.<br><br>" +
                            "The HA security page has been opened in your browser.<br>" +
                            "Scroll down to <b>Long-Lived Access Tokens</b>, create one,<br>" +
                            "and paste it into the <b>HA API Token</b> field.</html>",
                            "Login to Home Assistant",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(Panel.this,
                            resource.getString("HomeAssistantFloorPlan.Panel.error.oauthFailed.text") + "\n" + ex2.getMessage(),
                            resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        haTokenStatusLabel = new JLabel();
        updateTokenStatusLabel();

        fetchEntitiesButton = new JButton();
        fetchEntitiesButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.fetchEntitiesButton.text"));
        fetchEntitiesButton.addActionListener(e -> triggerFetchEntities(true));

        showEntitiesButton = new JButton("Select entities");
        java.util.List<String> cached = controller.getCachedHaEntityIds();
        showEntitiesButton.setEnabled(!cached.isEmpty());
        showEntitiesButton.addActionListener(e -> showEntitiesList(controller.getCachedHaEntityIds()));

        haEntityCountLabel = new JLabel(cached.isEmpty() ? "" : cached.size() + " entities");
        haEntityCountLabel.setForeground(java.awt.Color.GRAY);
        haLastFetchedLabel = new JLabel();
        haLastFetchedLabel.setForeground(java.awt.Color.GRAY);
        updateLastFetchedLabel();

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
        checkEntitiesButton = new JButton("Check entities");
        checkEntitiesButton.addActionListener(e -> checkEntities());

        sh3dAccordionCheckbox = new JCheckBox("Accordion Mode", true);
        haAccordionCheckbox = new JCheckBox("Accordion Mode", true);

        detectedLightsTree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent e) {
                if (!sh3dAccordionCheckbox.isSelected()) return;
                collapseOtherDomains(detectedLightsTree, e.getPath());
            }
            public void treeCollapsed(TreeExpansionEvent e) {}
        });
        otherEntitiesTree.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeExpanded(TreeExpansionEvent e) {
                if (!haAccordionCheckbox.isSelected()) return;
                collapseOtherDomains(otherEntitiesTree, e.getPath());
            }
            public void treeCollapsed(TreeExpansionEvent e) {}
        });

        sh3dAccordionCheckbox.addActionListener(e -> {
            if (sh3dAccordionCheckbox.isSelected()) expandFirstDomain(detectedLightsTree);
        });
        haAccordionCheckbox.addActionListener(e -> {
            if (haAccordionCheckbox.isSelected()) expandFirstDomain(otherEntitiesTree);
        });
    }

    private void setComponentsEnabled(boolean enabled) {
        detectedLightsTree.setEnabled(enabled);
        otherEntitiesTree.setEnabled(enabled);
        resolutionTextField.setEnabled(enabled);
        lightMixingModeComboBox.setEnabled(enabled);
        sensitivitySpinner.setEnabled(enabled);
        rendererComboBox.setEnabled(enabled);
        qualityComboBox.setEnabled(enabled);
        renderTimeSpinner.setEnabled(enabled);
        nightRenderCheckbox.setEnabled(enabled);
        nightRenderTimeSpinner.setEnabled(enabled);
        imageFormatComboBox.setEnabled(enabled);
        baseFolderTextField.setEnabled(enabled);
        outputDirectoryTextField.setEnabled(enabled);
        outputDirectoryBrowseButton.setEnabled(enabled);
        useExistingRendersCheckbox.setEnabled(enabled);
        cameraComboBox.setEnabled(enabled);
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
            0, currentGridYIndex, 2, 1, 1, 1, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));
        JScrollPane otherEntitiesScrollPane = new JScrollPane(otherEntitiesTree);
        otherEntitiesScrollPane.setPreferredSize(new Dimension(275, 350));
        add(otherEntitiesScrollPane, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 1, 1, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));
        currentGridYIndex++;

        /* Search fields under both trees */
        add(wrapWithClearButton(sh3dSearchField), new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 1, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(wrapWithClearButton(haSearchField), new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 1, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Accordion mode checkboxes */
        add(sh3dAccordionCheckbox, new GridBagConstraints(
            0, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, insets, 0, 0));
        add(haAccordionCheckbox, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, insets, 0, 0));
        currentGridYIndex++;

        /* Camera selector */
        add(cameraLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(cameraComboBox, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Resolution */
        add(resolutionLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(resolutionTextField, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Light mixing mode + render time */
        add(labelWithHelp(lightMixingModeLabel,
            "<html><b>How multiple light states are combined:</b><br><br>" +
            "<b>CSS</b> &mdash; Each light is rendered separately. The browser uses CSS<br>" +
            "&nbsp;&nbsp;mix-blend-mode to combine them live. Fewest renders: O(N lights).<br>" +
            "&nbsp;&nbsp;Best performance, good for many independent lights.<br><br>" +
            "<b>Room overlay</b> &mdash; All light combinations within each room are<br>" +
            "&nbsp;&nbsp;pre-rendered as PNG overlays. Better quality per room.<br><br>" +
            "<b>Complete renders</b> &mdash; Every on/off combination of all lights is<br>" +
            "&nbsp;&nbsp;rendered. Best quality but exponential render count &mdash;<br>" +
            "&nbsp;&nbsp;only practical with very few lights.</html>"),
            new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(lightMixingModeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(labelWithHelp(renderTimeLabel,
            "<html><b>Simulated date &amp; time for sun position / daylight color.</b><br><br>" +
            "<b>This is NOT the render duration!</b> It controls the sun angle<br>" +
            "and natural light color used during ray tracing.<br><br>" +
            "Examples:<br>" +
            "&nbsp;&bull; Noon in summer &rarr; bright overhead daylight<br>" +
            "&nbsp;&bull; Late afternoon &rarr; warm golden shadows<br>" +
            "&nbsp;&bull; Night-time render adds a second timestamp for dark mode.</html>"),
            new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(renderTimeSpinner, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Renderer + Night render*/
        add(labelWithHelp(rendererLabel,
            "<html><b>Ray-tracing engine used to render each image:</b><br><br>" +
            "<b>YafaRay</b> &mdash; Modern renderer, generally faster and higher quality.<br>" +
            "&nbsp;&nbsp;Recommended for most use cases.<br><br>" +
            "<b>SunFlow</b> &mdash; The original SweetHome3D renderer. Slower,<br>" +
            "&nbsp;&nbsp;but may produce a different look.<br><br>" +
            "Render duration per image varies by machine, resolution and scene.<br>" +
            "Rough estimates (YafaRay, 1024&times;576):<br>" +
            "&nbsp;&bull; Low quality: ~30 sec &ndash; 3 min<br>" +
            "&nbsp;&bull; High quality: ~5 &ndash; 30 min</html>"),
            new GridBagConstraints(
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
        add(labelWithHelp(imageFormatLabel,
            "<html><b>Output image format for floor plan images:</b><br><br>" +
            "<b>PNG</b> &mdash; Lossless, supports transparency.<br>" +
            "&nbsp;&nbsp;Required for Room overlay mode. Larger file size.<br><br>" +
            "<b>JPEG</b> &mdash; Lossy compression, smaller files.<br>" +
            "&nbsp;&nbsp;Cannot be used with Room overlay (no transparency).</html>"),
            new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(imageFormatComboBox, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(labelWithHelp(qualityLabel,
            "<html><b>Ray sampling quality per rendered image:</b><br><br>" +
            "<b>High</b> &mdash; More ray samples. Smoother shadows, less noise.<br>" +
            "&nbsp;&nbsp;Significantly slower. Use for final output.<br><br>" +
            "<b>Low</b> &mdash; Fewer samples. Faster but grainy.<br>" +
            "&nbsp;&nbsp;Good for test/preview runs.<br><br>" +
            "Typical times per image (varies by machine &amp; scene):<br>" +
            "&nbsp;&bull; Low:&nbsp;&nbsp;~30 sec &ndash; 3 min<br>" +
            "&nbsp;&bull; High: ~5 &ndash; 30 min</html>"),
            new GridBagConstraints(
            2, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityComboBox, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Sensitivity */
        add(labelWithHelp(sensitivityLabel,
            "<html><b>Pixel difference threshold for light overlay images (0&ndash;100):</b><br><br>" +
            "When computing a light overlay, pixels where the lit and unlit renders<br>" +
            "differ by more than this value are included in the overlay layer.<br><br>" +
            "&nbsp;&bull; <b>Lower</b> value &rarr; more sensitive, picks up subtle light changes<br>" +
            "&nbsp;&bull; <b>Higher</b> value &rarr; only strong brightness differences included<br><br>" +
            "Default: 10. Increase if you see unwanted ambient bleed in overlays.</html>"),
            new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivitySpinner, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Base folder */
        add(baseFolderLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(baseFolderTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Output directory */
        JPanel outputDirLabelPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        outputDirLabelPanel.add(outputDirectoryLabel);
        outputDirLabelPanel.add(createHelpButton("<html>Local folder where rendered images and the floor plan YAML will be saved.<br><br>" +
            "If a camera name is set, a subfolder with that name is created automatically.<br>" +
            "The output structure is:<br>" +
            "<tt>&nbsp;&nbsp;&lt;output dir&gt;/&lt;camera&gt;/renders/</tt><br>" +
            "<tt>&nbsp;&nbsp;&lt;output dir&gt;/&lt;camera&gt;/floorplan/</tt></html>"));
        add(outputDirLabelPanel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(outputDirectoryTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        JPanel outputDirButtonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        outputDirButtonPanel.add(outputDirectoryBrowseButton);
        outputDirButtonPanel.add(outputDirectoryOpenButton);
        add(outputDirButtonPanel, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, insets, 0, 0));
        currentGridYIndex++;

        /* Base folder */
        add(labelWithHelp(baseFolderLabel,
            "<html><b>Home Assistant www folder path prefix for generated YAML image URLs.</b><br><br>" +
            "Set this to the path (relative to HA <tt>www/</tt>) where you copy the output files.<br><br>" +
            "Example: if you copy to <tt>config/www/floorplan/</tt>, set this to <tt>floorplan</tt><br>" +
            "→ YAML image paths will be <tt>/local/floorplan/&lt;camera&gt;/floorplan/...</tt><br><br>" +
            "Leave empty to use paths starting directly at <tt>/local/</tt>.</html>"),
            new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(baseFolderTextField, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Home Assistant connection */
        JPanel haUrlPanel = new JPanel(new BorderLayout(0, 0));
        haUrlPanel.add(haUrlProtocolComboBox, BorderLayout.WEST);
        haUrlPanel.add(haUrlTextField, BorderLayout.CENTER);
        JPanel haUrlLabelPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        haUrlLabelPanel.add(haUrlLabel);
        haUrlLabelPanel.add(createHelpButton("<html>The URL of your Home Assistant instance.<br><br>" +
            "Examples:<br>" +
            "<tt>&nbsp;&nbsp;http://homeassistant.local:8123</tt><br>" +
            "<tt>&nbsp;&nbsp;https://myhome.duckdns.org</tt><br><br>" +
            "If you use <b>HTTPS</b>: OAuth login is not supported.<br>" +
            "Create a Long-Lived Access Token in your HA profile<br>" +
            "and paste it into the HA API Token field.</html>"));
        add(haUrlLabelPanel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(haUrlPanel, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 1, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(haLoginButton, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(haTokenStatusLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(fetchEntitiesButton, new GridBagConstraints(
            1, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(showEntitiesButton, new GridBagConstraints(
            3, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(haEntityCountLabel, new GridBagConstraints(
            1, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, insets, 0, 0));
        add(haLastFetchedLabel, new GridBagConstraints(
            2, currentGridYIndex, 2, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, insets, 0, 0));
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
                null, new Object [] {startButton, checkEntitiesButton, closeButton}, startButton);
        final JDialog dialog =
        optionPane.createDialog(SwingUtilities.getRootPane((Component)parentView), resource.getString("HomeAssistantFloorPlan.Plugin.NAME"));
        dialog.applyComponentOrientation(parentView != null ?
            ((JComponent)parentView).getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        dialog.setModal(false);
        dialog.setResizable(true);
        dialog.pack();
        dialog.setSize(700, 800);

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

        boolean accordion = (tree == detectedLightsTree)
            ? (sh3dAccordionCheckbox != null && sh3dAccordionCheckbox.isSelected())
            : (haAccordionCheckbox != null && haAccordionCheckbox.isSelected());
        if (accordion) {
            expandFirstDomain(tree);
        } else {
            for (int i = 0; i < tree.getRowCount(); i++)
                tree.expandRow(i);
        }
    }

    private void confirmAndRename(JTree tree, TreePath path, Entity entity, String suggestion) {
        JTextField field = new JTextField(suggestion, 40);
        JButton applyButton = new JButton("Apply");
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.add(new JLabel("New name:"), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        panel.add(applyButton, BorderLayout.EAST);

        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{});
        JDialog dialog = optionPane.createDialog(this, "Rename: " + entity.getName());
        applyButton.addActionListener(e -> {
            String newName = field.getText().trim();
            if (!newName.isEmpty() && !newName.equals(entity.getName())) {
                controller.renameEntity(entity, newName);
                refreshTrees();
            }
            dialog.dispose();
        });
        field.addActionListener(e -> applyButton.doClick());
        dialog.setVisible(true);
    }

    private void renameEntity(JTree tree, Entity entity) {
        String newName = (String)JOptionPane.showInputDialog(this,
            "New entity name:", "Rename entity",
            JOptionPane.PLAIN_MESSAGE, null, null, entity.getName());
        if (newName == null || newName.trim().isEmpty() || newName.equals(entity.getName()))
            return;
        controller.renameEntity(entity, newName.trim());
        refreshTrees();
    }

    private void refreshTrees() {
        String filter = sh3dSearchField != null ? sh3dSearchField.getText().toLowerCase().trim() : "";
        List<Entity> allEntities = new ArrayList<>(controller.getLightEntities());
        allEntities.addAll(controller.getOtherEntities());
        buildEntitiesGroupsTree(detectedLightsTree, allEntities.stream()
            .filter(e -> filter.isEmpty() || e.getName().toLowerCase().contains(filter))
            .collect(Collectors.groupingBy(e -> e.getName().split("\\.")[0])));
        checkEntities();
    }

    private static class HaEntityNode {
        final String entityId;
        final boolean matched;
        HaEntityNode(String entityId, boolean matched) { this.entityId = entityId; this.matched = matched; }
        @Override public String toString() {
            int dot = entityId.indexOf('.');
            return dot >= 0 ? entityId.substring(dot + 1) : entityId;
        }
    }

    private void checkEntities() {
        List<String> haIds = controller.getHaSelectedEntityIds();
        java.util.Set<String> sh3dIds = new java.util.HashSet<>();
        for (Entity e : controller.getLightEntities()) sh3dIds.add(e.getName());
        for (Entity e : controller.getOtherEntities()) sh3dIds.add(e.getName());
        String filter = haSearchField != null ? haSearchField.getText().toLowerCase().trim() : "";

        // Right tree: SH3D entities first (green=found, orange=not found), then remaining HA entities
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("HA Entities");
        java.util.Map<String, DefaultMutableTreeNode> domains = new java.util.LinkedHashMap<>();

        // Pass 1: SH3D entities sorted by domain
        java.util.List<String> sh3dSorted = new java.util.ArrayList<>(sh3dIds);
        java.util.Collections.sort(sh3dSorted);
        for (String entityId : sh3dSorted) {
            if (!filter.isEmpty() && !entityId.toLowerCase().contains(filter)) continue;
            int dot = entityId.indexOf('.');
            String domain = dot >= 0 ? entityId.substring(0, dot) : entityId;
            domains.computeIfAbsent(domain, d -> { DefaultMutableTreeNode n = new DefaultMutableTreeNode(d); root.add(n); return n; });
            DefaultMutableTreeNode domainNode = domains.get(domain);
            boolean matched = haIds.contains(entityId);
            domainNode.add(new DefaultMutableTreeNode(new HaEntityNode(entityId, matched)));
            domainNode.setUserObject(domain + " (" + domainNode.getChildCount() + ")");
        }
        // Pass 2: remaining HA entities not in SH3D
        for (String entityId : haIds) {
            if (sh3dIds.contains(entityId)) continue;
            if (!filter.isEmpty() && !entityId.toLowerCase().contains(filter)) continue;
            int dot = entityId.indexOf('.');
            String domain = dot >= 0 ? entityId.substring(0, dot) : entityId;
            domains.computeIfAbsent(domain, d -> { DefaultMutableTreeNode n = new DefaultMutableTreeNode(d); root.add(n); return n; });
            DefaultMutableTreeNode domainNode = domains.get(domain);
            domainNode.add(new DefaultMutableTreeNode(new HaEntityNode(entityId, false)));
            domainNode.setUserObject(domain + " (" + domainNode.getChildCount() + ")");
        }
           
        DefaultTreeModel model = (DefaultTreeModel) otherEntitiesTree.getModel();
        model.setRoot(root);
        model.reload();
        if (haAccordionCheckbox != null && haAccordionCheckbox.isSelected()) {
            expandFirstDomain(otherEntitiesTree);
        } else {
            for (int i = 0; i < otherEntitiesTree.getRowCount(); i++)
                otherEntitiesTree.expandRow(i);
        }
        ((DefaultTreeModel) detectedLightsTree.getModel()).reload();
        if (sh3dAccordionCheckbox != null && sh3dAccordionCheckbox.isSelected()) {
            expandFirstDomain(detectedLightsTree);
        } else {
            for (int i = 0; i < detectedLightsTree.getRowCount(); i++)
                detectedLightsTree.expandRow(i);
        }
    }

    private static final java.util.Set<String> SUPPORTED_DOMAINS = new java.util.HashSet<>(
        java.util.Arrays.asList("binary_sensor", "light", "switch", "sensor", "cover", "person"));

    private DefaultMutableTreeNode buildEntityTree(java.util.List<String> entities, String filter, java.util.Set<String> enabledDomains) {
        String f = filter.toLowerCase();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Entities");
        Map<String, DefaultMutableTreeNode> domains = new java.util.TreeMap<>();
        for (String entityId : entities) {
            int dot = entityId.indexOf('.');
            String domain = dot >= 0 ? entityId.substring(0, dot) : entityId;
            if (!enabledDomains.isEmpty() && !enabledDomains.contains(domain)) continue;
            if (!f.isEmpty() && !entityId.toLowerCase().contains(f)) continue;
            String name = dot >= 0 ? entityId.substring(dot + 1) : "";
            domains.computeIfAbsent(domain, d -> {
                DefaultMutableTreeNode n = new DefaultMutableTreeNode(d);
                root.add(n);
                return n;
            });
            domains.get(domain).add(new DefaultMutableTreeNode(name));
            DefaultMutableTreeNode domainNode = domains.get(domain);
            domainNode.setUserObject(domain + " (" + domainNode.getChildCount() + ")");
        }
        return root;
    }

    private java.util.Set<String> enabledDomains(java.util.Map<String, javax.swing.JCheckBox> checkboxes) {
        java.util.Set<String> s = new java.util.HashSet<>();
        checkboxes.forEach((d, cb) -> { if (cb.isSelected()) s.add(d); });
        return s;
    }

    private int countLeaves(DefaultMutableTreeNode node) {
        if (node.isLeaf()) return 1;
        int count = 0;
        for (int i = 0; i < node.getChildCount(); i++)
            count += countLeaves((DefaultMutableTreeNode) node.getChildAt(i));
        return count;
    }

    private java.util.Set<String> selectedDomains(java.util.Map<String, javax.swing.JCheckBoxMenuItem> items) {
        java.util.Set<String> s = new java.util.HashSet<>();
        items.forEach((d, item) -> { if (item.isSelected()) s.add(d); });
        return s;
    }

    private void showEntitiesList(java.util.List<String> entities) {
        java.util.Set<String> allDomains = new java.util.TreeSet<>();
        for (String e : entities) { int d = e.indexOf('.'); if (d > 0) allDomains.add(e.substring(0, d)); }

        java.util.Map<String, javax.swing.JCheckBoxMenuItem> domainItems = new java.util.LinkedHashMap<>();
        JPopupMenu domainPopup = new JPopupMenu();
        for (String domain : allDomains) {
            javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(domain, SUPPORTED_DOMAINS.contains(domain));
            domainItems.put(domain, item);
            domainPopup.add(item);
        }

        JButton filterButton = new JButton("Filter domains \u25bc");
        filterButton.addActionListener(e ->
            domainPopup.show(filterButton, 0, filterButton.getHeight()));

        JTextField searchField = new JTextField(20);

        JLabel countLabel = new JLabel();
        countLabel.setForeground(java.awt.Color.GRAY);

        // Selected list — rebuilt whenever entities change
        DefaultListModel<String> selectedModel = new DefaultListModel<>();
        javax.swing.border.TitledBorder selectedBorder = javax.swing.BorderFactory.createTitledBorder("Selected (0)");
        JList<String> selectedList = new JList<>(selectedModel);
        selectedList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!isSelected) setForeground(new java.awt.Color(0, 140, 0));
                return this;
            }
        });
        JScrollPane selectedScroll = new JScrollPane(selectedList);
        selectedScroll.setBorder(selectedBorder);
        selectedScroll.setPreferredSize(new java.awt.Dimension(500, 100));

        Runnable[] refreshRef = new Runnable[1];
        DefaultMutableTreeNode root = buildEntityTree(entities, "", selectedDomains(domainItems));
        DefaultTreeModel model = new DefaultTreeModel(root);
        JTree tree = new JTree(model);
        tree.setRootVisible(false);

        Runnable rebuildSelected = () -> {
            java.util.Set<String> sh3dIds = new java.util.HashSet<>();
            for (Entity e : controller.getLightEntities()) sh3dIds.add(e.getName());
            for (Entity e : controller.getOtherEntities()) sh3dIds.add(e.getName());
            selectedModel.clear();
            java.util.List<String> sorted = new java.util.ArrayList<>(sh3dIds);
            java.util.Collections.sort(sorted);
            for (String id : sorted) {
                if (entities.contains(id)) selectedModel.addElement(id);
            }
            selectedBorder.setTitle("Selected (" + selectedModel.size() + ")");
            selectedScroll.repaint();
        };
        rebuildSelected.run();

        refreshRef[0] = () -> {
            DefaultMutableTreeNode newRoot = buildEntityTree(entities, searchField.getText(), selectedDomains(domainItems));
            model.setRoot(newRoot);
            int shown = countLeaves(newRoot);
            countLabel.setText(shown + " of " + entities.size());
        };
        refreshRef[0].run();
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override public void executeUpdate(DocumentEvent e) { refreshRef[0].run(); }
        });
        domainItems.values().forEach(item -> item.addActionListener(e -> refreshRef[0].run()));

        // Listen for entity changes while dialog is open
        PropertyChangeListener entityChangeListener = ev -> EventQueue.invokeLater(() -> {
            rebuildSelected.run();
            refreshRef[0].run();
        });
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, entityChangeListener);

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new java.awt.Dimension(500, 310));

        JPanel topRow = new JPanel(new BorderLayout(4, 0));
        topRow.add(filterButton, BorderLayout.WEST);
        topRow.add(searchField, BorderLayout.CENTER);
        topRow.add(countLabel, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(selectedScroll, BorderLayout.NORTH);
        top.add(topRow, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 4));
        content.add(top, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, content,
            "Select entities",
            JOptionPane.PLAIN_MESSAGE);

        // Clean up listener when dialog closes
        controller.removePropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, entityChangeListener);
    }

    private JTextField createSearchField(String placeholder, Runnable onChange) {
        JTextField field = new JTextField();
        // Draw placeholder text manually
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override public void executeUpdate(DocumentEvent e) { onChange.run(); }
        });
        // Paint placeholder manually for L&Fs that ignore putClientProperty
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { field.repaint(); }
            public void focusLost(java.awt.event.FocusEvent e) { field.repaint(); }
        });
        field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override protected void paintSafely(java.awt.Graphics g) {
                super.paintSafely(g);
                if (field.getText().isEmpty() && !field.hasFocus()) {
                    g.setColor(java.awt.Color.LIGHT_GRAY);
                    java.awt.Insets ins = field.getInsets();
                    g.drawString(placeholder, ins.left + 2, field.getHeight() - ins.bottom - 4);
                }
            }
        });
        return field;
    }

    private JPanel wrapWithClearButton(JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        JButton clearBtn = new JButton("\u2715");
        clearBtn.setMargin(new java.awt.Insets(0, 4, 0, 4));
        clearBtn.setFocusable(false);
        clearBtn.addActionListener(e -> field.setText(""));
        panel.add(field, BorderLayout.CENTER);
        panel.add(clearBtn, BorderLayout.EAST);
        return panel;
    }

    private void copyToClipboard(String text) {
        java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(text);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }

    private void collapseOtherDomains(JTree tree, TreePath expandedPath) {
        if (expandedPath.getPathCount() != 2) return; // only act on domain-level nodes
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            TreePath childPath = new TreePath(child.getPath());
            if (!childPath.equals(expandedPath))
                tree.collapsePath(childPath);
        }
    }

    private void expandFirstDomain(JTree tree) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root.getChildCount() > 0) {
            // collapse all first
            for (int i = 0; i < root.getChildCount(); i++)
                tree.collapsePath(new TreePath(((DefaultMutableTreeNode) root.getChildAt(i)).getPath()));
            // then expand first
            DefaultMutableTreeNode first = (DefaultMutableTreeNode) root.getChildAt(0);
            tree.expandPath(new TreePath(first.getPath()));
        }
    }

    private JPanel labelWithHelp(JLabel label, String message) {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));
        p.add(label);
        p.add(createHelpButton(message));
        return p;
    }

    private JButton createHelpButton(String message) {
        JButton btn = new JButton("?");
        btn.setMargin(new java.awt.Insets(0, 4, 0, 4));
        btn.setFont(btn.getFont().deriveFont(java.awt.Font.BOLD, 10f));
        btn.setFocusPainted(false);
        btn.addActionListener(e ->
            JOptionPane.showMessageDialog(Panel.this, message, "Help", JOptionPane.INFORMATION_MESSAGE));
        return btn;
    }

    private void applyResolution() {
        Matcher m = Pattern.compile("(\\d+)\\s*[xX\u00d7]\\s*(\\d+)").matcher(resolutionTextField.getText().trim());
        if (m.find()) {
            int w = Integer.parseInt(m.group(1));
            int h = Integer.parseInt(m.group(2));
            if (w >= 10 && h >= 10 && w <= 10000 && h <= 10000) {
                controller.setRenderWidth(w);
                controller.setRenderHeight(h);
                resolutionTextField.setText(w + " x " + h);
                return;
            }
        }
        resolutionTextField.setText(controller.getRenderWidth() + " x " + controller.getRenderHeight());
    }

    private void updateLastFetchedLabel() {
        long t = controller.getHaEntityCacheTime();
        if (t == 0) {
            haLastFetchedLabel.setText("");
        } else {
            java.text.DateFormat fmt = java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.MEDIUM, java.text.DateFormat.MEDIUM, Locale.getDefault());
            haLastFetchedLabel.setText("Last fetch: " + fmt.format(new java.util.Date(t)));
        }
    }

    private void updateTokenStatusLabel() {
        String token = controller.getHaApiToken();
        if (token != null && !token.isEmpty()) {
            haTokenStatusLabel.setText("<html><font color='green'>&#9679; Connected</font></html>");
        } else {
            haTokenStatusLabel.setText("<html><font color='gray'>&#9679; Not logged in</font></html>");
        }
    }

    private void triggerFetchEntities(boolean showListAfter) {
        fetchEntitiesButton.setEnabled(false);
        fetchEntitiesButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.fetchEntitiesButton.loading.text"));
        new Thread(() -> {
            try {
                java.util.List<String> entities = controller.fetchEntitiesFromHomeAssistant();
                EventQueue.invokeLater(() -> {
                    fetchEntitiesButton.setEnabled(true);
                    fetchEntitiesButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.fetchEntitiesButton.text"));
                    showEntitiesButton.setEnabled(true);
                    haEntityCountLabel.setText(entities.size() + " entities");
                    updateLastFetchedLabel();
                    checkEntities();
                    if (showListAfter)
                        showEntitiesList(entities);
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    fetchEntitiesButton.setEnabled(true);
                    fetchEntitiesButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.fetchEntitiesButton.text"));
                    JOptionPane.showMessageDialog(Panel.this,
                        resource.getString("HomeAssistantFloorPlan.Panel.error.fetchFailed.text") + "\n" + ex.getMessage(),
                        resource.getString("HomeAssistantFloorPlan.Panel.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private java.awt.image.BufferedImage scaleImage(java.awt.image.BufferedImage src, int maxW, int maxH) {
        if (maxW <= 0 || maxH <= 0) return src;
        double scale = Math.min((double)maxW / src.getWidth(), (double)maxH / src.getHeight());
        int w = Math.max(1, (int)(src.getWidth() * scale));
        int h = Math.max(1, (int)(src.getHeight() * scale));
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, src.getType());
        out.createGraphics().drawImage(src.getScaledInstance(w, h, java.awt.Image.SCALE_FAST), 0, 0, null);
        return out;
    }

    private void openEntityOptionsPanel(Entity entity) {
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, entity);
        entityOptionsPanel.displayView(this);
    }

    private JDialog showRenderProgressWindow() {
        JDialog win = new JDialog(SwingUtilities.getWindowAncestor(this),
            resource.getString("HomeAssistantFloorPlan.Plugin.NAME") + " — Rendering",
            java.awt.Dialog.ModalityType.MODELESS);
        win.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel previewLabel = new JLabel("Waiting for first render...", JLabel.CENTER);
        previewLabel.setPreferredSize(new java.awt.Dimension(480, 270));
        previewLabel.setBorder(LineBorder.createGrayLineBorder());

        JProgressBar winProgress = new JProgressBar() {
            @Override
            public String getString() {
                return String.format("%d / %d", getValue(), getMaximum());
            }
        };
        winProgress.setStringPainted(true);
        winProgress.setMinimum(0);
        winProgress.setMaximum(controller.getNumberOfTotalRenders());
        controller.addPropertyChangeListener(Controller.Property.COMPLETED_RENDERS, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                EventQueue.invokeLater(() -> winProgress.setValue(((Number)ev.getNewValue()).intValue()));
            }
        });
        controller.addPropertyChangeListener(Controller.Property.RENDER_PREVIEW, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
                java.awt.image.BufferedImage img = (java.awt.image.BufferedImage) ev.getNewValue();
                if (img != null) {
                    java.awt.image.BufferedImage scaled = scaleImage(img,
                        previewLabel.getWidth() > 0 ? previewLabel.getWidth() : 480,
                        previewLabel.getHeight() > 0 ? previewLabel.getHeight() : 270);
                    EventQueue.invokeLater(() -> previewLabel.setIcon(new javax.swing.ImageIcon(scaled)));
                }
            }
        });

        JButton stopButton = new JButton(resource.getString("HomeAssistantFloorPlan.Panel.stopButton.text"));
        stopButton.addActionListener(e -> stop());

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(winProgress, BorderLayout.CENTER);
        south.add(stopButton, BorderLayout.EAST);

        win.getContentPane().setLayout(new BorderLayout(0, 4));
        win.getContentPane().add(previewLabel, BorderLayout.CENTER);
        win.getContentPane().add(south, BorderLayout.SOUTH);
        win.pack();
        win.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        win.setVisible(true);
        return win;
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
