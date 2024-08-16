package com.shmuelzon.HomeAssistantFloorPlan;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class SimpleDocumentListener implements DocumentListener {

    abstract void executeUpdate(DocumentEvent event);

    @Override
    public void insertUpdate(DocumentEvent e) {
        executeUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        executeUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        executeUpdate(e);
    }
}
