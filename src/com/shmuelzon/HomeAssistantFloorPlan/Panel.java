package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.Cursor;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Instant;
import java.awt.image.BufferedImage;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import javax.swing.ActionMap;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.DefaultListModel;
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
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.DateFormatter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import com.eteks.sweethome3d.model.Content;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.PieceOfFurniture; // Use this interface
import com.eteks.sweethome3d.model.CatalogPieceOfFurniture; // For accessing category
import com.eteks.sweethome3d.model.FurnitureCategory;     // For category name
import com.eteks.sweethome3d.model.Room;
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
    private enum ActionType {ADD_RENDER_TIME, REMOVE_RENDER_TIME, BROWSE, START, STOP, CLOSE, PREVIEW}
    private Plugin.HomeAssistantFloorPlanAction pluginAction;
    
    final TimeZone timeZone = TimeZone.getDefault(); // Interpret user input in local time zone
    private static Panel currentPanel;
    private UserPreferences preferences;
    private Controller controller;
    private ResourceBundle resource;
    private ExecutorService renderExecutor;
    private JLabel detectedLightsLabel;
    private JTree detectedLightsTree;
    private JLabel otherEntitiesLabel;
    private JTree otherEntitiesTree;
    private JLabel pointOfViewLabel;
    private JComboBox<String> pointOfViewComboBox;
    private JLabel furnitureToCenterLabel;
    private JComboBox<String> furnitureToCenterComboBox;
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
    private JLabel renderDateLabel;
    private JLabel renderTimeLabel;
    private JLabel addRemoveTimesLabel;
    private JLabel outputDirectoryLabel;
    private JTextField outputDirectoryTextField;
    private SpinnerDateModel dateModel;
    private JSpinner renderDateSpinner;
    private SpinnerDateModel timeModel;
    private JSpinner renderTimeSpinner;
    private JButton renderTimeAddButton;
    private JButton renderTimeRemoveButton;
    private DefaultListModel<Long> renderTimesListModel;
    private JLabel addedTimesLabel;
    private JList<Long> renderTimesList;
    private JLabel imageFormatLabel;
    private JComboBox<Controller.ImageFormat> imageFormatComboBox;
    private JButton outputDirectoryBrowseButton;
    private FileContentManager outputDirectoryChooser;
    private JCheckBox useExistingRendersCheckbox;
    private JProgressBar progressBar;
    private JButton startButton;
    private JButton closeButton;
    private JButton previewButton;
    private boolean isProgrammaticTimeChange = false;

    private class EntityNode {
        public Entity entity;
        public List<String> attributes;
        private Controller nodeController; // Store controller for room lookup

        public EntityNode(Entity entity, Controller controller) {
            this.entity = entity;
            this.nodeController = controller;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String entityIdPart = entity.getName(); // This is now the entity_id part
            String attributePart = entity.getAttribute();

            if (entityIdPart != null) {
                int dotIndex = entityIdPart.indexOf('.');
                if (dotIndex != -1 && dotIndex < entityIdPart.length() - 1) {
                    sb.append(entityIdPart.substring(dotIndex + 1));
                } else {
                    sb.append(entityIdPart);
                }
            }

            List<String> details = new ArrayList<>();
            String simplifiedType = null;
            String sh3dId = entity.getId();
            // If there's an attribute, add it to the details.
            if (attributePart != null && !attributePart.isEmpty()) {
                details.add("attr: " + attributePart);
            }
            List<? extends com.eteks.sweethome3d.model.HomePieceOfFurniture> pieces = entity.getPiecesOfFurniture();

            if (pieces != null && !pieces.isEmpty()) {
                com.eteks.sweethome3d.model.HomePieceOfFurniture firstPiece = pieces.get(0);

                Content contentModel = firstPiece.getModel();
                if (contentModel instanceof PieceOfFurniture) {
                    PieceOfFurniture model = (PieceOfFurniture) contentModel;

                    if (entity.getIsLight()) {
                        // For lights, type is implicitly "Light", so simplifiedType remains null
                    } else if (model.isDoorOrWindow()) { // Check if it's a door or window type
                        // Since getInformation() is for tooltips, and getName() is HA entity ID,
                        // we infer by checking if the HA entity ID (model.getName()) contains "door" or "window".
                        String modelName = model.getName(); // This is the HA entity ID
                        if (modelName != null && !modelName.trim().isEmpty()) {
                            String lowerModelName = modelName.toLowerCase();
                            if (lowerModelName.contains("door")) {
                                simplifiedType = "Door";
                            } else if (lowerModelName.contains("window")) {
                                simplifiedType = "Window";
                            } else {
                                simplifiedType = "Door/Window"; // Fallback if name isn't specific
                            }
                        } else {
                            simplifiedType = "Door/Window"; // Fallback if no name
                        }
                    } else {
                        // For other non-light, non-door/window furniture
                        String modelInfoName = null; // From model.getInformation()
                        String categoryName = null;

                        // Prioritize model.getInformation() for the descriptive name
                        if (model.getInformation() != null && !model.getInformation().trim().isEmpty()) {
                            modelInfoName = model.getInformation().trim();
                        }

                        // Get category name if it's a CatalogPieceOfFurniture
                        // Only try category if modelInfoName wasn't specific enough
                        if (modelInfoName == null || modelInfoName.isEmpty() || "Piece of furniture".equalsIgnoreCase(modelInfoName) || "Default".equalsIgnoreCase(modelInfoName)) {
                            if (model instanceof CatalogPieceOfFurniture) {
                                FurnitureCategory category = ((CatalogPieceOfFurniture) model).getCategory();
                                if (category != null && category.getName() != null && !category.getName().trim().isEmpty()) {
                                    categoryName = category.getName().trim();
                                }
                            }
                        }

                        // Decision logic: Prefer specific model name, then category, then fallback
                        if (modelInfoName != null && !modelInfoName.equalsIgnoreCase("Piece of furniture") && !modelInfoName.equalsIgnoreCase("Default") && !modelInfoName.isEmpty()) {
                            simplifiedType = modelInfoName;
                            if ("Armchair".equalsIgnoreCase(simplifiedType) || "Office chair".equalsIgnoreCase(simplifiedType)) {
                                simplifiedType = "Chair"; // Refine common names
                            }
                        } else if (categoryName != null && !categoryName.isEmpty() && !"Miscellaneous".equalsIgnoreCase(categoryName)) {
                            // Model name was generic/null/empty, use category name if it's not too generic itself
                            simplifiedType = categoryName;
                        } else if (model.getName() != null && !model.getName().trim().isEmpty() && !model.getName().equalsIgnoreCase("Piece of furniture") && !model.getName().equalsIgnoreCase("Default")) {
                            // Fallback to model.getName() if info and category were not useful, but avoid HA entity ID if it's the same as model.getName()
                            if (!entity.getName().equals(model.getName())) { // Avoid using the HA entity ID as the type
                                simplifiedType = model.getName().trim();
                            }
                        }
                    }
                }
            }

            // Fallback to ID parsing if simplifiedType is still null AND it's not a light
            if (!entity.getIsLight() && simplifiedType == null && sh3dId != null) {
                int hyphenIndex = sh3dId.indexOf('-');
                if (hyphenIndex > 0) {
                    String rawTypeFromId = sh3dId.substring(0, hyphenIndex);
                    if ("PieceOfFurniture".equalsIgnoreCase(rawTypeFromId)) {
                        simplifiedType = "Furniture"; // Generic fallback from ID
                    } else if (!rawTypeFromId.isEmpty()) {
                        simplifiedType = Character.toUpperCase(rawTypeFromId.charAt(0)) + rawTypeFromId.substring(1);
                    }
                }
            }

            // Add simplified type to details list if it's NOT a light entity and a type is determined
            if (!entity.getIsLight() && simplifiedType != null && !simplifiedType.isEmpty()) {
                details.add(simplifiedType);
            }

            if (this.nodeController != null) {
                Room room = this.nodeController.getRoomForEntity(this.entity);
                if (room != null && room.getName() != null && !room.getName().trim().isEmpty()) {
                    details.add(room.getName());
                }
            }

            if (!details.isEmpty()) {
                sb.append(" (").append(String.join(", ", details)).append(")");
            }

            List<String> currentAttributes = attributesList();
            if (!currentAttributes.isEmpty()) {
                sb.append(" ").append(currentAttributes.toString());
            }
            return sb.toString();
        }

        // --- MODIFIED: This method now correctly checks the new operator/value fields ---
        private List<String> attributesList() {
            attributes = new ArrayList<>();

            if (entity.getAlwaysOn())
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.alwaysOn.text"));
            if (entity.getIsRgb())
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.isRgb.text"));
            
            // Check if there's a meaningful, non-empty condition set for the furniture
            String furnitureValue = entity.getFurnitureDisplayValue();
            if (entity.getFurnitureDisplayOperator() != Entity.DisplayOperator.ALWAYS) {
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.displayByState.text"));
            }
            if (entity.getBlinking())
                attributes.add(resource.getString("HomeAssistantFloorPlan.Panel.attributes.blinking.text"));
            // Optionally, you could add indicators for label-specific styles here if desired
            // For example:
            // if (entity.getDisplayType() == Entity.DisplayType.LABEL && entity.getLabelColor() != null && !entity.getLabelColor().isEmpty()) {
            //     attributes.add("custom color");
            // }


            return attributes;
        }
    }

    private class ImagePanel extends JPanel {
        private BufferedImage image;

        public ImagePanel(BufferedImage image) {
            this.image = image;
            // The preferred size of this panel should be the size of the image.
            // This is crucial for JScrollPane to work correctly.
            this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    // The mouse coordinates (e.getPoint()) are now directly relative to the image.
                    List<Entity> entitiesAtPoint = controller.getEntitiesAtPoint(e.getPoint());
                    if (!entitiesAtPoint.isEmpty()) {
                        List<String> entityNames = entitiesAtPoint.stream()
                                                                .map(Entity::getName)
                                                                .collect(Collectors.toList());
                        String tooltipText = "<html>" + String.join("<br>", entityNames) + "</html>";
                        setToolTipText(tooltipText);
                        // Manually trigger the tooltip manager to show the tooltip immediately.
                        ToolTipManager.sharedInstance().mouseMoved(e);
                    } else {
                        setToolTipText(null);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            }
        }
    }
    public Panel(UserPreferences preferences, ClassLoader classLoader, Controller controller, Plugin.HomeAssistantFloorPlanAction pluginAction) {
        super(new GridBagLayout());
        this.preferences = preferences;
        this.controller = controller;
        this.pluginAction = pluginAction;

        resource = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault(), classLoader);
        createActions();
        createComponents();

        // Set tooltip initial delay to 0 for immediate display
        ToolTipManager.sharedInstance().setInitialDelay(0);
        layoutComponents();
    }

    private void createActions() {
        final ActionMap actions = getActionMap();
        actions.put(ActionType.ADD_RENDER_TIME, new ResourceAction(preferences, Panel.class, ActionType.ADD_RENDER_TIME.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                LocalTime time = ((Date)renderTimeSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalTime();
                LocalDate date = ((Date)renderDateSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalDate();
                Long newTimestamp = date.atTime(time).atZone(timeZone.toZoneId()).toInstant().toEpochMilli();

                List<Long> renderingTimes = controller.getRenderDateTimes();
                renderingTimes.add(newTimestamp);
                controller.setRenderDateTimes(new ArrayList<>(new TreeSet<Long>(renderingTimes)));
                updateRenderingTimesList(false);
            }
        });
        actions.put(ActionType.REMOVE_RENDER_TIME, new ResourceAction(preferences, Panel.class, ActionType.REMOVE_RENDER_TIME.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                int selectedIndex = renderTimesList.getSelectedIndex();
                if (selectedIndex == -1 || renderTimesListModel.getSize() <= 1)
                    return;

                List<Long> renderingTimes = controller.getRenderDateTimes();
                renderingTimes.remove(selectedIndex);
                controller.setRenderDateTimes(renderingTimes);
                updateRenderingTimesList(false);
            }
        });
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
                        // Disable UI components. It's generally safe to call this from the worker thread
                        // as setComponentsEnabled mostly sets properties. For strict EDT-only, use SwingUtilities.invokeLater.
                        SwingUtilities.invokeLater(() -> setComponentsEnabled(false));

                        String finalMessage = null;
                        String finalTitle = null;
                        int finalMessageType = JOptionPane.INFORMATION_MESSAGE;

                        try {
                            controller.render();
                            finalMessage = resource.getString("HomeAssistantFloorPlan.Panel.info.finishedRendering.text");
                            // Title can be null for information messages or set if desired
                        } catch (InterruptedException e) { // Log InterruptedException
                            System.err.println("Rendering process was interrupted:");
                            e.printStackTrace();
                            // Optionally set a message for the user about interruption
                            // finalMessage = "Rendering was interrupted.";
                            // finalTitle = "Interrupted";
                            // finalMessageType = JOptionPane.WARNING_MESSAGE;
                        } catch (Exception e) {
                            System.err.println("Exception during rendering process:");
                            e.printStackTrace();
                            finalMessage = resource.getString("HomeAssistantFloorPlan.Panel.error.failedRendering.text") + "\n" + e.getMessage();
                            finalTitle = resource.getString("HomeAssistantFloorPlan.Panel.error.title");
                            finalMessageType = JOptionPane.ERROR_MESSAGE;
                        } finally {
                            // This block ensures UI is re-enabled and messages are shown on the EDT
                            final String messageToShow = finalMessage;
                            final String titleToShow = finalTitle;
                            final int messageTypeToShow = finalMessageType;
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    setComponentsEnabled(true);
                                    renderExecutor = null; // Clear the executor
                                    if (messageToShow != null) {
                                        JOptionPane.showMessageDialog(Panel.this, messageToShow, titleToShow, messageTypeToShow);
                                    }
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
        actions.put(ActionType.PREVIEW, new ResourceAction(preferences, Panel.class, ActionType.PREVIEW.name(), true) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                previewButton.setEnabled(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                new SwingWorker<BufferedImage, Void>() {
                    @Override
                    protected BufferedImage doInBackground() throws Exception {
                        return controller.generatePreviewImage();
                    }

                    @Override
                    protected void done() {
                        try {
                            final BufferedImage previewImage = get(); // Get result from doInBackground
                            if (previewImage != null) {
                                // Create an instance of our custom ImagePanel
                                final ImagePanel imagePanel = new ImagePanel(previewImage);

                                // Put the panel in a scroll pane
                                JScrollPane scrollPane = new JScrollPane(imagePanel);

                                // Set a reasonable preferred size for the scroll pane itself
                                int prefWidth = Math.min(1024, previewImage.getWidth()) + 40; // Cap width at 1024px + padding
                                int prefHeight = Math.min(1024, previewImage.getHeight()) + 40; // Cap height at 1024px + padding
                                scrollPane.setPreferredSize(new Dimension(prefWidth, prefHeight));
                                
                                JDialog previewDialog = new JDialog(SwingUtilities.getWindowAncestor(Panel.this), resource.getString("HomeAssistantFloorPlan.Panel.previewButton.text"), Dialog.ModalityType.APPLICATION_MODAL);
                                previewDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                previewDialog.getContentPane().add(scrollPane);
                                previewDialog.pack();
                                previewDialog.setLocationRelativeTo(Panel.this); // Center relative to the main plugin panel
                                previewDialog.setVisible(true);
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            // Optionally inform user: JOptionPane.showMessageDialog(Panel.this, "Preview rendering was interrupted.", "Interrupted", JOptionPane.WARNING_MESSAGE);
                        } catch (ExecutionException ex) {
                            Throwable cause = ex.getCause();
                            String errorMessage = "An unexpected error occurred during preview.";
                            if (cause != null) {
                                errorMessage = "Error generating preview: " + cause.getMessage();
                                if (cause instanceof InterruptedException) Thread.currentThread().interrupt();
                            }
                            JOptionPane.showMessageDialog(Panel.this, errorMessage, "Preview Error", JOptionPane.ERROR_MESSAGE);
                            if (cause != null) cause.printStackTrace(); else ex.printStackTrace();
                        } finally {
                            previewButton.setEnabled(true);
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                }.execute();
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

    private void restartPlugin() {
        // Use invokeLater to ensure the current event dispatching is complete
        // before we dispose the window and start a new action.
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
            // Stop any running render before restarting
            stop();
            pluginAction.execute();
        });
    }

    private void handlePointOfViewChange() {
        String selection = (String) pointOfViewComboBox.getSelectedItem();
        if (selection == null) return;

        // The point of view selection always triggers a restart if changed.
        if (!selection.equals(controller.getPointOfViewName())) {
            controller.setPointOfViewName(selection);
            restartPlugin();
        }
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
                // Re-group other entities each time to reflect any changes
                Map<String, List<Entity>> currentOtherEntitiesGroupedByType = controller.getOtherEntities().stream()
                    .collect(Collectors.groupingBy(entity -> {
                        String name = entity.getName();
                        if (name != null && name.contains(".")) {
                            return name.split("\\.")[0];
                        }
                        return "unknown_domain"; // Fallback for names without a domain
                    }));
                buildEntitiesGroupsTree(otherEntitiesTree, currentOtherEntitiesGroupedByType);
            }
        };
        controller.addPropertyChangeListener(Controller.Property.NUMBER_OF_RENDERS, updateTreeOnProperyChanged);
        for (Entity light : controller.getLightEntities()) {
            light.addPropertyChangeListener(Entity.Property.ALWAYS_ON, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.IS_RGB, updateTreeOnProperyChanged);
            light.addPropertyChangeListener(Entity.Property.FURNITURE_DISPLAY_CONDITION, updateTreeOnProperyChanged);
        }

        // --- NEW: Add this loop to ensure the UI updates for non-light entities ---
        for (Entity other : controller.getOtherEntities()) {
            other.addPropertyChangeListener(Entity.Property.FURNITURE_DISPLAY_CONDITION, updateTreeOnProperyChanged);
        }
        // --- END NEW ---

        pointOfViewLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.pointOfViewLabel.text"));
        pointOfViewComboBox = new JComboBox<>();
        furnitureToCenterLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.furnitureToCenterLabel.text"));
        furnitureToCenterComboBox = new JComboBox<>();

        final String currentViewOption = resource.getString("HomeAssistantFloorPlan.Panel.pointOfView.currentView.text");

        // Check Sweet Home 3D's preference for "aerial view centered on selection"
        boolean isAerialViewCenteredOnSelection = preferences.isAerialViewCenteredOnSelectionEnabled();

        pointOfViewComboBox.addItem(currentViewOption);
        for (String cameraName : controller.getStoredCameraNames()) {
            pointOfViewComboBox.addItem(cameraName);
        }

        // Set selected item
        String savedPov = controller.getPointOfViewName();
        pointOfViewComboBox.setSelectedItem(savedPov);

        pointOfViewComboBox.addActionListener(e -> handlePointOfViewChange());
        // --- NEW: Apply consistent look and feel ---
        pointOfViewComboBox.setEditable(true);
        JTextField pointOfViewEditor = (JTextField) pointOfViewComboBox.getEditor().getEditorComponent();
        pointOfViewEditor.setEditable(false);
        pointOfViewEditor.setFocusable(false);
        makeClickableToOpenDropdown(pointOfViewComboBox);
        // --- END NEW ---

        // Populate the furniture to center on combo box
        final String noCenterOption = "<None>";
        furnitureToCenterComboBox.addItem(noCenterOption);
        TreeSet<String> furnitureNames = new TreeSet<>();
        for (HomePieceOfFurniture piece : controller.getHome().getFurniture()) {
            if (piece.getName() != null && !piece.getName().trim().isEmpty()) {
                furnitureNames.add(piece.getName());
            }
        }
        for (String name : furnitureNames) {
            furnitureToCenterComboBox.addItem(name);
        }

        String savedFurniture = controller.getFurnitureNameToCenter();
        if (savedFurniture == null || savedFurniture.trim().isEmpty()) {
            furnitureToCenterComboBox.setSelectedItem(noCenterOption);
        } else {
            furnitureToCenterComboBox.setSelectedItem(savedFurniture);
        }

        furnitureToCenterComboBox.addActionListener(e -> {
            String selection = (String) furnitureToCenterComboBox.getSelectedItem();
            String valueToSet = noCenterOption.equals(selection) ? "" : selection;

            if (!valueToSet.equals(controller.getFurnitureNameToCenter())) {
                controller.setFurnitureNameToCenter(valueToSet);
                restartPlugin();
            }
        });
        // --- NEW: Apply consistent look and feel ---
        furnitureToCenterComboBox.setEditable(true);
        JTextField furnitureToCenterEditor = (JTextField) furnitureToCenterComboBox.getEditor().getEditorComponent();
        furnitureToCenterEditor.setEditable(false);
        furnitureToCenterEditor.setFocusable(false);
        makeClickableToOpenDropdown(furnitureToCenterComboBox);
        // --- END NEW ---

        // Initial visibility of furniture to center on depends on SH3D preference
        furnitureToCenterLabel.setVisible(isAerialViewCenteredOnSelection);
        furnitureToCenterComboBox.setVisible(isAerialViewCenteredOnSelection);

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
        // --- NEW: Apply consistent look and feel ---
        lightMixingModeComboBox.setEditable(true);
        JTextField lightMixingModeEditor = (JTextField) lightMixingModeComboBox.getEditor().getEditorComponent();
        lightMixingModeEditor.setEditable(false);
        lightMixingModeEditor.setFocusable(false);
        if (controller.getLightMixingMode() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.lightMixingModeComboBox.%s.text", controller.getLightMixingMode().name()));
            lightMixingModeEditor.setText(initialDisplayText);
        }
        lightMixingModeComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.lightMixingModeComboBox.%s.text", ((Controller.LightMixingMode)o).name())));
                return rendererComponent;
            }
        });
        lightMixingModeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Controller.LightMixingMode selectedMode = (Controller.LightMixingMode)lightMixingModeComboBox.getSelectedItem();
                controller.setLightMixingMode(selectedMode);
                updateComboBoxEditorText(lightMixingModeComboBox, "HomeAssistantFloorPlan.Panel.lightMixingModeComboBox.%s.text", selectedMode);
            }
        });
        makeClickableToOpenDropdown(lightMixingModeComboBox);

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
        // --- NEW: Apply consistent look and feel ---
        rendererComboBox.setEditable(true);
        JTextField rendererEditor = (JTextField) rendererComboBox.getEditor().getEditorComponent();
        rendererEditor.setEditable(false);
        rendererEditor.setFocusable(false);
        if (controller.getRenderer() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.rendererComboBox.%s.text", controller.getRenderer().name()));
            rendererEditor.setText(initialDisplayText);
        }
        rendererComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.rendererComboBox.%s.text", ((Controller.Renderer)o).name())));
                return rendererComponent;
            }
        });
        rendererComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Controller.Renderer selectedRenderer = (Controller.Renderer)rendererComboBox.getSelectedItem();
                controller.setRenderer(selectedRenderer);
                updateComboBoxEditorText(rendererComboBox, "HomeAssistantFloorPlan.Panel.rendererComboBox.%s.text", selectedRenderer);
            }
        });
        makeClickableToOpenDropdown(rendererComboBox);

        qualityLabel = new JLabel();
        qualityLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.qualityLabel.text"));
        qualityComboBox = new JComboBox<Controller.Quality>(Controller.Quality.values());
        qualityComboBox.setSelectedItem(controller.getQuality());
        // --- NEW: Apply consistent look and feel ---
        qualityComboBox.setEditable(true);
        JTextField qualityEditor = (JTextField) qualityComboBox.getEditor().getEditorComponent();
        qualityEditor.setEditable(false);
        qualityEditor.setFocusable(false);
        if (controller.getQuality() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.qualityComboBox.%s.text", controller.getQuality().name()));
            qualityEditor.setText(initialDisplayText);
        }
        qualityComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.qualityComboBox.%s.text", ((Controller.Quality)o).name())));
                return rendererComponent;
            }
        });
        qualityComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Controller.Quality selectedQuality = (Controller.Quality)qualityComboBox.getSelectedItem();
                controller.setQuality(selectedQuality);
                updateComboBoxEditorText(qualityComboBox, "HomeAssistantFloorPlan.Panel.qualityComboBox.%s.text", selectedQuality);
            }
        });
        makeClickableToOpenDropdown(qualityComboBox);

        renderDateLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.renderDateLabel.text"));
        renderTimeLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.renderTimeLabel.text"));
        addRemoveTimesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.addRemoveTimesLabel.text"));
        addedTimesLabel = new JLabel(resource.getString("HomeAssistantFloorPlan.Panel.addedTimesLabel.text"));
        dateModel = new SpinnerDateModel();
        renderDateSpinner = new JSpinner(dateModel);
        final JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(renderDateSpinner);
        dateEditor.getFormat().setTimeZone(timeZone);
        dateEditor.getFormat().applyPattern("dd/MM/yyyy");
        renderDateSpinner.setEditor(dateEditor);
        final DateFormatter dateFormatter = (DateFormatter)dateEditor.getTextField().getFormatter();
        dateFormatter.setAllowsInvalid(false);
        dateFormatter.setOverwriteMode(true);
        renderDateSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                changeDatesForAllRenderingTimes();
            }
        });
        timeModel = new SpinnerDateModel();
        renderTimeSpinner = new JSpinner(timeModel); // Keep this as it's used by the spinner
        final JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(renderTimeSpinner);
        timeEditor.getFormat().setTimeZone(timeZone);
        timeEditor.getFormat().applyPattern("HH:mm");
        renderTimeSpinner.setEditor(timeEditor);
        final DateFormatter timeFormatter = (DateFormatter)timeEditor.getTextField().getFormatter();
        timeFormatter.setAllowsInvalid(false);
        timeFormatter.setOverwriteMode(true);        
        renderTimeAddButton = new JButton(actionMap.get(ActionType.ADD_RENDER_TIME));
        renderTimeAddButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.addTimeButton.text"));

        // This listener updates the selected time in the list when the spinner is changed.
        renderTimeSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                if (isProgrammaticTimeChange) {
                    return; // Do nothing if the change was triggered programmatically
                }

                int selectedIndex = renderTimesList.getSelectedIndex();
                if (selectedIndex == -1) {
                    // If no item is selected, do not modify the list.
                    // The user must click "Add" to add a new time.
                    return;
                }

                // An item is selected, so update its time component.
                LocalTime newTime = ((Date) renderTimeSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalTime();
                List<Long> currentRenderingTimes = controller.getRenderDateTimes();

                if (selectedIndex < currentRenderingTimes.size()) {
                    Long selectedTimestamp = currentRenderingTimes.get(selectedIndex);
                    LocalDate selectedDate = Instant.ofEpochMilli(selectedTimestamp).atZone(timeZone.toZoneId()).toLocalDate();
                    Long updatedTimestamp = selectedDate.atTime(newTime).atZone(timeZone.toZoneId()).toInstant().toEpochMilli();

                    // Prevent updating if it creates a duplicate time
                    boolean alreadyExists = false;
                    for (int i = 0; i < currentRenderingTimes.size(); i++) {
                        if (i != selectedIndex && currentRenderingTimes.get(i).equals(updatedTimestamp)) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        currentRenderingTimes.set(selectedIndex, updatedTimestamp);
                        controller.setRenderDateTimes(new ArrayList<>(new TreeSet<>(currentRenderingTimes)));
                        updateRenderingTimesList(false);
                        renderTimesList.setSelectedValue(updatedTimestamp, true); // Re-select the (possibly moved) item
                    }
                }
            }
        });
        renderTimeRemoveButton = new JButton(actionMap.get(ActionType.REMOVE_RENDER_TIME));
        renderTimeRemoveButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.removeTimeButton.text"));
        
        renderTimesListModel = new DefaultListModel<>();
        renderTimesList = new JList<>(renderTimesListModel);
        renderTimesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        renderTimesList.setVisibleRowCount(3);
        renderTimesList.setAutoscrolls(true);
        renderTimesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, timeEditor.getFormat().format(Date.from(Instant.ofEpochMilli((Long) value).atZone(timeZone.toZoneId()).toInstant())), index, isSelected, cellHasFocus);
            }
        });
        // This listener updates the spinner when a time is selected in the list.
        renderTimesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Long selectedValue = renderTimesList.getSelectedValue();
                if (selectedValue != null) {
                    isProgrammaticTimeChange = true;
                    timeModel.setValue(new Date(selectedValue));
                    isProgrammaticTimeChange = false;
                }
            }
        });
        updateRenderingTimesList(true);

        imageFormatLabel = new JLabel();
        imageFormatLabel.setText(resource.getString("HomeAssistantFloorPlan.Panel.imageFormatLabel.text"));
        imageFormatComboBox = new JComboBox<Controller.ImageFormat>(Controller.ImageFormat.values());
        imageFormatComboBox.setSelectedItem(controller.getImageFormat());
        // --- NEW: Apply consistent look and feel ---
        imageFormatComboBox.setEditable(true);
        JTextField imageFormatEditor = (JTextField) imageFormatComboBox.getEditor().getEditorComponent();
        imageFormatEditor.setEditable(false);
        imageFormatEditor.setFocusable(false);
        if (controller.getImageFormat() != null) {
            String initialDisplayText = resource.getString(String.format("HomeAssistantFloorPlan.Panel.imageFormatComboBox.%s.text", controller.getImageFormat().name()));
            imageFormatEditor.setText(initialDisplayText);
        }
        imageFormatComboBox.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> jList, Object o, int i, boolean b, boolean b1) {
                Component rendererComponent = super.getListCellRendererComponent(jList, o, i, b, b1);
                setText(resource.getString(String.format("HomeAssistantFloorPlan.Panel.imageFormatComboBox.%s.text", ((Controller.ImageFormat)o).name())));
                return rendererComponent;
            }
        });
        imageFormatComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Controller.ImageFormat selectedFormat = (Controller.ImageFormat)imageFormatComboBox.getSelectedItem();
                controller.setImageFormat(selectedFormat);
                updateComboBoxEditorText(imageFormatComboBox, "HomeAssistantFloorPlan.Panel.imageFormatComboBox.%s.text", selectedFormat);
            }
        });
        makeClickableToOpenDropdown(imageFormatComboBox);

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

        previewButton = new JButton(actionMap.get(ActionType.PREVIEW));
        previewButton.setText(resource.getString("HomeAssistantFloorPlan.Panel.previewButton.text"));
    }

    private void setComponentsEnabled(boolean enabled) {
        detectedLightsTree.setEnabled(enabled);
        otherEntitiesTree.setEnabled(enabled);
        widthSpinner.setEnabled(enabled);
        heightSpinner.setEnabled(enabled);
        lightMixingModeComboBox.setEnabled(enabled);
        sensitivitySpinner.setEnabled(enabled);
        rendererComboBox.setEnabled(enabled);
        qualityComboBox.setEnabled(enabled);
        renderDateSpinner.setEnabled(enabled);
        renderTimeSpinner.setEnabled(enabled);
        renderTimeAddButton.setEnabled(enabled);
        renderTimeRemoveButton.setEnabled(enabled);
        renderTimesList.setEnabled(enabled);;
        imageFormatComboBox.setEnabled(enabled);
        outputDirectoryTextField.setEnabled(enabled);
        outputDirectoryBrowseButton.setEnabled(enabled);
        useExistingRendersCheckbox.setEnabled(enabled);
        previewButton.setEnabled(enabled);
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
        JScrollPane otherEntitiesScrollPane = new JScrollPane(otherEntitiesTree);

        JSplitPane treesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                   detectedLightsScrollPane,
                                                   otherEntitiesScrollPane);
        treesSplitPane.setResizeWeight(0.33);
        treesSplitPane.setOneTouchExpandable(true);
        treesSplitPane.setContinuousLayout(true);
        treesSplitPane.setPreferredSize(new Dimension(800, 350));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                treesSplitPane.setDividerLocation(0.33);
            }
        });

        add(treesSplitPane, new GridBagConstraints(
            0, currentGridYIndex, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, insets, 0, 0));
        currentGridYIndex++;

        // --- Refactored to a 2-column layout for narrower, more stable width ---

        /* Point of View */
        add(pointOfViewLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        pointOfViewLabel.setHorizontalAlignment(labelAlignment);
        add(pointOfViewComboBox, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Furniture to Center */
        add(furnitureToCenterLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        furnitureToCenterLabel.setHorizontalAlignment(labelAlignment);
        add(furnitureToCenterComboBox, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Resolution */
        add(widthLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        widthLabel.setHorizontalAlignment(labelAlignment);
        JPanel resolutionPanel = new JPanel(new GridBagLayout());
        resolutionPanel.add(widthSpinner, new GridBagConstraints(
            0, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,standardGap), 0, 0));
        resolutionPanel.add(heightLabel, new GridBagConstraints(
            1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,standardGap), 0, 0));
        resolutionPanel.add(heightSpinner, new GridBagConstraints(
            2, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        add(resolutionPanel, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Light mixing mode */
        add(lightMixingModeLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        lightMixingModeLabel.setHorizontalAlignment(labelAlignment);
        add(lightMixingModeComboBox, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        // --- Refactored to add components directly to the main panel's GridBagLayout ---
        // --- This ensures that columns align vertically across these two rows. ---

        /* Renderer and Quality Row */
        add(rendererLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rendererLabel.setHorizontalAlignment(labelAlignment);
        add(rendererComboBox, new GridBagConstraints(1, currentGridYIndex, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(qualityLabel, new GridBagConstraints(2, currentGridYIndex, 1, 1, 0.0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
        add(qualityComboBox, new GridBagConstraints(3, currentGridYIndex, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Image format and Sensitivity Row */
        add(imageFormatLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        imageFormatLabel.setHorizontalAlignment(labelAlignment);
        add(imageFormatComboBox, new GridBagConstraints(1, currentGridYIndex, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        add(sensitivityLabel, new GridBagConstraints(2, currentGridYIndex, 1, 1, 0.0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insets, 0, 0));
        add(sensitivitySpinner, new GridBagConstraints(3, currentGridYIndex, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Render Date and Time */
        add(renderDateLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        renderDateLabel.setHorizontalAlignment(labelAlignment);
        JPanel dateTimePanel = new JPanel(new GridBagLayout());
        dateTimePanel.add(renderDateSpinner, new GridBagConstraints(
            0, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,standardGap), 0, 0));
        dateTimePanel.add(renderTimeLabel, new GridBagConstraints(
            1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,standardGap), 0, 0));
        dateTimePanel.add(renderTimeSpinner, new GridBagConstraints(
            2, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        add(dateTimePanel, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Added Times List and Buttons */
        add(addedTimesLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.NORTH,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        addedTimesLabel.setHorizontalAlignment(labelAlignment);
        JPanel timesListPanel = new JPanel(new java.awt.BorderLayout(standardGap, 0));
        timesListPanel.add(new JScrollPane(renderTimesList), java.awt.BorderLayout.CENTER);
        JPanel addRemoveButtonsPanel = new JPanel(new GridBagLayout());
        addRemoveButtonsPanel.add(renderTimeAddButton, new GridBagConstraints(
            0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,standardGap,0), 0, 0));
        addRemoveButtonsPanel.add(renderTimeRemoveButton, new GridBagConstraints(
            0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
        timesListPanel.add(addRemoveButtonsPanel, java.awt.BorderLayout.EAST);
        add(timesListPanel, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
            GridBagConstraints.BOTH, insets, 0, 0));
        currentGridYIndex++;

        /* Output directory */
        add(outputDirectoryLabel, new GridBagConstraints(
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        outputDirectoryLabel.setHorizontalAlignment(labelAlignment);
        JPanel outputDirectoryPanel = new JPanel(new java.awt.BorderLayout(standardGap, 0));
        outputDirectoryPanel.add(outputDirectoryTextField, java.awt.BorderLayout.CENTER);
        outputDirectoryPanel.add(outputDirectoryBrowseButton, java.awt.BorderLayout.EAST);
        add(outputDirectoryPanel, new GridBagConstraints(
            1, currentGridYIndex, 3, 1, 1.0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Options */
        add(useExistingRendersCheckbox, new GridBagConstraints(
            0, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        /* Progress bar */
        add(progressBar, new GridBagConstraints(
            0, currentGridYIndex, 4, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL, insets, 0, 0));
        currentGridYIndex++;

        add(previewButton, new GridBagConstraints( // Add preview button
            0, currentGridYIndex, 1, 1, 0, 0, GridBagConstraints.LINE_START, // Align left under progress bar
            GridBagConstraints.NONE, insets, 0, 0));
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
                null, new Object [] {startButton, closeButton}, startButton); // Preview button is in the panel layout
        final JDialog dialog =
        optionPane.createDialog(SwingUtilities.getRootPane((Component)parentView), resource.getString("HomeAssistantFloorPlan.Plugin.NAME"));
        dialog.applyComponentOrientation(parentView != null ?
            ((JComponent)parentView).getComponentOrientation() : ComponentOrientation.getOrientation(Locale.getDefault()));
        dialog.setModal(true);
        dialog.setResizable(true); // Explicitly set resizable
        dialog.pack();             // Pack the dialog after creation and before showing

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

        // entityGroups is Map<String (groupKey), List<Entity>>
        // For lights: groupKey is determined by LightMixingMode (room name, HA entity name, or "Home").
        // For other entities: groupKey is the entity domain (e.g., "sensor", "switch").
        for (String groupKey : new TreeSet<String>(entityGroups.keySet())) {
            List<Entity> entitiesInGroup = entityGroups.get(groupKey);
            if (entitiesInGroup == null || entitiesInGroup.isEmpty()) {
                continue;
            }

            DefaultMutableTreeNode groupDisplayNode;

            // Create a node for the groupKey (e.g., Room Name, HA Entity Name for CSS lights, or Domain for other entities).
            groupDisplayNode = new DefaultMutableTreeNode(groupKey);
            // Sort entities within the group using Entity.compareTo (which now includes SH3D ID)
            for (Entity entity : new TreeSet<>(entitiesInGroup)) { 
                groupDisplayNode.add(new DefaultMutableTreeNode(new EntityNode(entity, this.controller)));
            }
            model.insertNodeInto(groupDisplayNode, root, root.getChildCount());
        }
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void updateRenderingTimesList(boolean overrideSpinners) {
        List<Long> renderingTimes = controller.getRenderDateTimes();

        if (overrideSpinners) {
            dateModel.setValue(new Date(renderingTimes.get(0)));
            timeModel.setValue(new Date(renderingTimes.get(0)));
        }

        renderTimesListModel.clear();
        for (Long renderingTime : renderingTimes)
            renderTimesListModel.addElement(renderingTime);
    }

    private void changeDatesForAllRenderingTimes() {
        List<Long> currentRenderingTimes = controller.getRenderDateTimes();
        LocalDate newDate = ((Date)renderDateSpinner.getValue()).toInstant().atZone(timeZone.toZoneId()).toLocalDate();
        List<Long> newRenderingTimes = currentRenderingTimes.stream().map(timestamp -> {
            LocalTime time = Instant.ofEpochMilli(timestamp).atZone(timeZone.toZoneId()).toLocalTime(); // This line was duplicated, but now it's the only one.
            return newDate.atTime(time).atZone(timeZone.toZoneId()).toInstant().toEpochMilli(); // Use toEpochMilli for consistency
        }).collect(Collectors.toList());

        controller.setRenderDateTimes(newRenderingTimes);
        updateRenderingTimesList(false);
    }

    private void openEntityOptionsPanel(Entity entity) {
        // Pass the main 'controller' instance to the options panel constructor.
        // This gives the panel the reference it needs to get state suggestions and other controller interactions.
        EntityOptionsPanel entityOptionsPanel = new EntityOptionsPanel(preferences, entity, this.controller); // Pass 'this.controller'
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

    // --- NEW: Helper method to update editor text for non-editable JComboBoxes ---
    private <T extends Enum<T>> void updateComboBoxEditorText(JComboBox<T> comboBox, String resourceKeyPattern, T selectedValue) {
        // This helper is for JComboBoxes that are set to editable() but whose editor JTextField is set to non-editable.
        // It ensures the editor displays the localized string instead of the enum's toString().
        if (comboBox == null || !comboBox.isEditable()) {
            return;
        }
        Component editorComp = comboBox.getEditor().getEditorComponent();
        if (!(editorComp instanceof JTextField)) { // Ensure the editor component is a JTextField
            return;
        }
        JTextField editorTextField = (JTextField) editorComp;
        // Only apply if the JTextField itself is not editable (our "fake" setup)
        if (editorTextField.isEditable()) { 
            return;
        }

        String determinedTextValue; // Use a non-final temporary variable
        if (selectedValue != null) {
            if (resource != null) {
                try {
                    determinedTextValue = resource.getString(String.format(resourceKeyPattern, selectedValue.name()));
                } catch (java.util.MissingResourceException e) {
                    // Fallback if resource key is missing, log and use enum's default toString()
                    System.err.println("Warning: Missing resource for JComboBox editor text: " + String.format(resourceKeyPattern, selectedValue.name()) + ". Using default name.");
                    determinedTextValue = selectedValue.toString(); 
                }
            } else { // resource is null, but selectedValue is not
                determinedTextValue = selectedValue.toString(); // Fallback if resource bundle is somehow null
            }
        } else { // selectedValue is null
            determinedTextValue = ""; // Empty string for a null selectedValue
        }
        final String textToSet = determinedTextValue; // Assign to the final variable once

        // Directly set the text of the JTextField component of the editor.
        // Only update if the text is actually different to avoid potential event loops or unnecessary screen flicker.
        if (!editorTextField.getText().equals(textToSet)) {
            editorTextField.setText(textToSet);
        }
    }

    // --- NEW: Helper method to make the editor area of a non-typable editable combo box clickable ---
    private <T> void makeClickableToOpenDropdown(JComboBox<T> comboBox) {
        if (comboBox == null || !comboBox.isEditable()) return;
        Component editorComp = comboBox.getEditor().getEditorComponent();
        if (editorComp == null || (editorComp instanceof JTextField && ((JTextField) editorComp).isEditable())) return;

        editorComp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (comboBox.isEnabled()) {
                    SwingUtilities.invokeLater(() -> comboBox.setPopupVisible(!comboBox.isPopupVisible()));
                }
            }
        });
    }
};
