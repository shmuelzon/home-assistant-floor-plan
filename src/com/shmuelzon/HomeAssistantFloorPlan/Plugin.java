package com.shmuelzon.HomeAssistantFloorPlan;

import com.eteks.sweethome3d.plugin.PluginAction;
import com.eteks.sweethome3d.viewcontroller.View;
import com.eteks.sweethome3d.model.HomeFurnitureGroup;
import com.eteks.sweethome3d.model.HomePieceOfFurniture;
import com.eteks.sweethome3d.model.Selectable;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.model.Camera;

import java.util.ArrayList;
import java.awt.Dialog;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


public class Plugin extends com.eteks.sweethome3d.plugin.Plugin {
    @Override
    public PluginAction[] getActions() {
        return new PluginAction [] { new HomeAssistantFloorPlanAction() };
    }

    public class HomeAssistantFloorPlanAction extends PluginAction {
        private Controller controller;
        private Panel panel;

        public HomeAssistantFloorPlanAction() {
            super("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", "HomeAssistantFloorPlan.Plugin", getPluginClassLoader(), true);
        }

        private HomePieceOfFurniture findFurnitureByName(String name, List<HomePieceOfFurniture> furnitureList) {
            for (HomePieceOfFurniture piece : furnitureList) {
                if (piece instanceof HomeFurnitureGroup) {
                    HomePieceOfFurniture foundInGroup = findFurnitureByName(name, ((HomeFurnitureGroup) piece).getFurniture());
                    if (foundInGroup != null) {
                        return foundInGroup;
                    }
                } else {
                    // Check for null before comparing and use equalsIgnoreCase for robustness
                    if (piece.getName() != null && name.equalsIgnoreCase(piece.getName())) {
                        return piece;
                    }
                }
            }
            return null;
        }

        @Override
        public void execute() {
            final ResourceBundle resourceBundle = ResourceBundle.getBundle("com.shmuelzon.HomeAssistantFloorPlan.ApplicationPlugin", Locale.getDefault(), getPluginClassLoader());
            final String storedCameraName = "HomeAssistantOutput"; // The name of your stored point of view
            final View view = getHomeController().getView();

            // Create and show a "Please wait" dialog immediately on the EDT. The 'view' object itself is a Component.
            final JDialog waitDialog = new JDialog(SwingUtilities.getWindowAncestor((java.awt.Component)view), "Loading...", Dialog.ModalityType.APPLICATION_MODAL); // Modal dialog
            waitDialog.setUndecorated(true); // Remove window decorations
            waitDialog.getContentPane().add(new JLabel("  Please wait, setting up view...  ")); // Add some padding
            waitDialog.pack();
            waitDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor((java.awt.Component)view));
            
            // Use a SwingWorker to perform the camera change and delay in the background
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // Use a temporary settings object to read config before controller is created
                    Settings settings = new Settings(getHome());
                    // These keys must match what Controller.java uses
                    final String POV_SETTING_KEY = "pointOfView";
                    final String FURNITURE_SETTING_KEY = "furnitureToCenter";
                    
                    // This text value must match what Panel.java uses
                    final String currentViewOption = resourceBundle.getString("HomeAssistantFloorPlan.Panel.pointOfView.currentView.text");
                    
                    // Check Sweet Home 3D's preference for "aerial view centered on selection"
                    UserPreferences userPreferences = getUserPreferences();
                    boolean isAerialViewCenteredOnSelection = userPreferences.isAerialViewCenteredOnSelectionEnabled();

                    if (isAerialViewCenteredOnSelection) {
                        // If SH3D preference is ON, use the furniture name from plugin settings
                        String furnitureNameToCenter = settings.get(FURNITURE_SETTING_KEY, "");
                        if (furnitureNameToCenter != null && !furnitureNameToCenter.isEmpty()) {
                            HomePieceOfFurniture pieceToSelect = findFurnitureByName(furnitureNameToCenter, getHome().getFurniture());
                            if (pieceToSelect != null) {
                                final List<Selectable> selection = new ArrayList<>();
                                selection.add(pieceToSelect);
                                SwingUtilities.invokeAndWait(() -> {
                                    getHome().setSelectedItems(selection);
                                });
                                Thread.sleep(500); // Wait for view to pan
                            } else {
                                System.out.println("Plugin Info: Furniture '" + furnitureNameToCenter + "' not found for centering.");
                            }
                        }
                    }

                    // Now, apply the point of view selected in the plugin's settings
                    String pointOfViewName = settings.get(POV_SETTING_KEY, currentViewOption);
                    if (!pointOfViewName.equals(currentViewOption)) {
                        Camera storedCamera = getHome().getStoredCameras().stream()
                                .filter(cam -> pointOfViewName.equals(cam.getName()))
                                .findFirst()
                                .orElse(null);
                        if (storedCamera != null) {
                            final Camera cameraToSet = storedCamera;
                            SwingUtilities.invokeAndWait(() -> {
                                getHomeController().getHomeController3D().goToCamera(cameraToSet);
                            });
                            Thread.sleep(500); // Wait for view to settle
                        } else {
                            System.err.println("Plugin Warning: Stored point of view '" + pointOfViewName + "' not found.");
                        }
                    }
                    return null;
                }

                @Override
                protected void done() {
                    // This part runs back on the EDT after doInBackground completes
                    waitDialog.dispose();

                    try {
                        get(); // Check for any exceptions that occurred in doInBackground()
                        controller = new Controller(getHome(), resourceBundle);
                        panel = new Panel(getUserPreferences(), getPluginClassLoader(), controller, HomeAssistantFloorPlanAction.this);
                        panel.displayView(view);
                    } catch (Exception e) {
                        Throwable cause = e;
                        if (e instanceof java.util.concurrent.ExecutionException) {
                            cause = e.getCause();
                        }                        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor((java.awt.Component)view),
                                "Error setting up view: " + cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        cause.printStackTrace();
                    }
                }
            }.execute(); // Start the SwingWorker
            waitDialog.setVisible(true);
        }
    }
 }
