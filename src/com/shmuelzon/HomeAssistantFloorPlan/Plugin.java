package com.shmuelzon.HomeAssistantFloorPlan;

import com.eteks.sweethome3d.plugin.PluginAction;


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

        @Override
        public void execute() {
            controller = new Controller(getHome());
            panel = new Panel(getUserPreferences(), getPluginClassLoader(), controller);
            panel.displayView(getHomeController().getView());
        }
    }
 }
