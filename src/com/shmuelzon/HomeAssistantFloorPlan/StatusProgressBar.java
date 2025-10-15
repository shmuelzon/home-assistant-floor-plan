package com.shmuelzon.HomeAssistantFloorPlan;

import javax.swing.JProgressBar;

public class StatusProgressBar extends javax.swing.JProgressBar {
    private String statusText = "";

    @Override
    public String getString() {
        return String.format("%s (%d/%d)", statusText, getValue(), getMaximum());
    }

    public void setStatusText(String text) {
        this.statusText = text;
    }
}
