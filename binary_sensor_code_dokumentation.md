# Binary Sensors im Home Assistant Floor Plan Plugin
## VOLLSTÄNDIGE CODE-BASIERTE DOKUMENTATION

> **Quelle**: Analyse des Java-Quellcodes von https://github.com/shmuelzon/home-assistant-floor-plan
> **Version**: master branch (Stand März 2026)
> **Analysierte Dateien**: Entity.java (795 Zeilen), EntityOptionsPanel.java (705 Zeilen)

---

## 1. WIE BINARY SENSORS ERKANNT WERDEN

### Code-Logik (Entity.java, Zeilen 644-645)
```java
isLight = firstPiece instanceof HomeLight;
isDoorOrWindow = firstPiece instanceof HomeDoorOrWindow;
```

**Bedeutung für Binary Sensors**:
- Binary Sensors sind **KEINE** SweetHome3D-Lights → `isLight = false`
- Binary Sensors sind **KEINE** SweetHome3D-Doors/Windows → `isDoorOrWindow = false`
- Binary Sensors sind **normale** `HomePieceOfFurniture`-Objekte mit dem Entity-Namen

**Erkennung**:
```
Sweet Home 3D:  Möbelname = "binary_sensor.bewegung_flur"
                         ↓
Plugin:         Erkennt Entity-Typ anhand Präfix
                         ↓
Behandlung:     Weder Light noch Door/Window
                         ↓
UI-Optionen:    Standard-Entity-Optionen + displayFurnitureCondition
```

---

## 2. STANDARDWERTE FÜR BINARY SENSORS

### Display Type (Entity.java, Zeilen 650-652)
```java
private DisplayType defaultDisplayType() {
    return name.startsWith("sensor.") ? DisplayType.LABEL : DisplayType.ICON;
}
```

**Für Binary Sensors**:
- `binary_sensor.xxx` → **ICON** (nicht LABEL!)
- Nur `sensor.xxx` bekommt LABEL als Standard

### Tap Action (Entity.java, Zeilen 658-681)
```java
private Action defaultAction() {
    String[] actionableEntityPrefixes = {
        "alarm_control_panel.", "button.", "climate.", 
        "cover.", "fan.", "humidifier.", "lawn_mower.",
        "light.", "lock.", "media_player.", "switch.",
        "vacuum.", "valve.", "water_header.",
    };
    
    for (String prefix : actionableEntityPrefixes) {
        if (name.startsWith(prefix))
            return Action.TOGGLE;
    }
    return Action.MORE_INFO;
}
```

**Für Binary Sensors**:
- `binary_sensor.` ist **NICHT** in der Liste → Standard-Action = **MORE_INFO**
- Klick öffnet Entity-Details-Popup in Home Assistant

---

## 3. VERFÜGBARE OPTIONEN IM PLUGIN-UI

### 3.1 ALLGEMEINE OPTIONEN (für ALLE Entities)

Code-Basis: EntityOptionsPanel.java, Zeilen 468-594

#### Display Type
- **Optionen**: BADGE, ICON, LABEL
- **Standard**: ICON
- **Code-Mapping** (Zeilen 520-524):
  ```java
  put(DisplayType.BADGE, "state-badge");
  put(DisplayType.ICON, "state-icon");
  put(DisplayType.LABEL, "state-label");
  ```

#### Icon Override
- **Nur sichtbar wenn**: DisplayType = ICON
- **Code** (Zeilen 675-676):
  ```java
  iconOverrideLabel.setVisible(displayTypeComboBox.getSelectedItem() == Entity.DisplayType.ICON);
  iconOverrideTextField.setVisible(displayTypeComboBox.getSelectedItem() == Entity.DisplayType.ICON);
  ```
- **Format**: `mdi:icon-name` (z.B. `mdi:motion-sensor`)

#### Attribute
- **Nur sichtbar wenn**: DisplayType = LABEL
- **Code** (Zeilen 677-678):
  ```java
  attributeLabel.setVisible(displayTypeComboBox.getSelectedItem() == Entity.DisplayType.LABEL);
  attributeTextField.setVisible(displayTypeComboBox.getSelectedItem() == Entity.DisplayType.LABEL);
  ```
- **Beispiele**: `last_changed`, `battery_level`, beliebiges Entity-Attribut

#### Display Condition
- **Optionen**: ALWAYS, NEVER, AVAILABLE, WHEN_ON, WHEN_OFF
- **Standard**: ALWAYS
- **YAML-Generierung** (Zeilen 557-573):
  ```java
  if (displayCondition == DisplayCondition.ALWAYS)
      return yaml;  // kein conditional wrapper
  
  String state_condition = "state_not: unavailable";
  if (displayCondition != DisplayCondition.AVAILABLE)
      state_condition = "state: " + (displayCondition == DisplayCondition.WHEN_ON ? "on" : "off");
  
  return String.format(
      "  - type: conditional\n" +
      "    conditions:\n" +
      "      - condition: state\n" +
      "        entity: %s\n" +
      "        %s\n" +
      "    elements:\n" +
      "%s",
      name, state_condition, yaml.replaceAll(".*\\R", "    $0")
  );
  ```

**Generiertes YAML**:
- `ALWAYS`: Kein conditional
- `NEVER`: Leerer String (Entity wird nicht generiert)
- `AVAILABLE`: `state_not: unavailable`
- `WHEN_ON`: `state: on`
- `WHEN_OFF`: `state: off`

#### Tap / Double Tap / Hold Actions
- **Optionen**: MORE_INFO, NAVIGATE, NONE, TOGGLE
- **Standard Tap**: MORE_INFO
- **Standard Double Tap**: NONE
- **Standard Hold**: MORE_INFO
- **Navigate Value**: Nur sichtbar wenn Action = NAVIGATE
- **Code** (Zeile 679-681):
  ```java
  tapActionValueTextField.setVisible(tapActionComboBox.getSelectedItem() == Entity.Action.NAVIGATE);
  ```

**YAML-Ausgabe** (Zeilen 502-517):
```java
private String actionYaml(Action action, String value) {
    final Map<Action, String> actionToYamlString = new HashMap<Action, String>() {{
        put(Action.MORE_INFO, "more-info");
        put(Action.NAVIGATE, "navigate");
        put(Action.NONE, "none");
        put(Action.TOGGLE, "toggle");
    }};
    
    String yaml = actionToYamlString.get(action);
    
    if (action == Action.NAVIGATE)
        yaml += String.format("\n" +
            "      navigation_path: %s", value);
    
    return yaml;
}
```

#### Position
- **Links (left)**: 0-100% (Double)
- **Oben (top)**: 0-100% (Double)
- **Standard**: Automatisch berechnet aus 3D-Position
- **User-Defined**: Wird gespeichert und überschreibt Auto-Berechnung

#### Opacity
- **Bereich**: 0-100 (Integer)
- **Standard**: 100
- **Einheit**: Prozent

#### Scale
- **Bereich**: 0-100 (Integer)
- **Standard**: 100
- **Einheit**: Prozent

#### Background Color
- **Format**: CSS-Color-String
- **Standard**: `rgba(255, 255, 255, 0.3)` (weißer Hintergrund, 30% Transparenz)

---

### 3.2 BINARY-SENSOR-SPEZIFISCHE OPTIONEN

Code-Basis: EntityOptionsPanel.java, Zeilen 640-652

#### Display Furniture Condition

**NUR für Non-Lights und Non-Doors/Windows verfügbar** (Zeilen 596-601):
```java
if (entity.getIsLight())
    layoutLightSpecificComponents(labelAlignment, insets, currentGridYIndex);
else if (entity.getIsDoorOrWindow())
    layoutDoorOrWindowSpecificComponents(labelAlignment, insets, currentGridYIndex);
else
    layoutNonLightSpecificComponents(labelAlignment, insets, currentGridYIndex);
```

**Optionen**:
- `ALWAYS`: Möbel immer sichtbar
- `STATE_EQUALS`: Möbel sichtbar wenn Entity-State = Value
- `STATE_NOT_EQUALS`: Möbel sichtbar wenn Entity-State ≠ Value

**Condition Value**: Textfeld für den Vergleichswert

**Verwendungszweck**:
- Möbelstück basierend auf Binary Sensor State ein/ausblenden
- Beispiel: Stuhl nur anzeigen wenn `binary_sensor.stuhl_besetzt == "on"`

---

### 3.3 OPTIONEN DIE BINARY SENSORS **NICHT** HABEN

#### ❌ Always On (nur für Lights)
Code: EntityOptionsPanel.java, Zeilen 604-613
```java
private void layoutLightSpecificComponents(...) {
    /* Always on */
    add(alwaysOnLabel, ...);
    add(alwaysOnCheckbox, ...);
    /* Is RGB */
    add(isRgbLabel, ...);
    add(isRgbCheckbox, ...);
}
```

#### ❌ Is RGB/Dimmable (nur für Lights)
Code: Gleiche Methode, Zeilen 615-623

#### ❌ Open Door/Window Condition (nur für Doors/Windows)
Code: EntityOptionsPanel.java, Zeilen 626-638
```java
private void layoutDoorOrWindowSpecificComponents(...) {
    add(openFurnitureConditionLabel, ...);
    add(openFurnitureConditionComboBox, ...);
    add(openFurnitureConditionValueTextField, ...);
}
```

---

## 4. YAML-AUSGABE FÜR BINARY SENSORS

### Standard-Ausgabe (Entity.java, Zeilen 535-555)

**Code**:
```java
String yaml = String.format(Locale.US,
    "  - type: %s\n" +
    "    entity: %s\n" +
    "    title: %s\n" +
    "%s" +  // iconOverride oder attribute
    "    style:\n" +
    "      top: %.2f%%\n" +
    "      left: %.2f%%\n" +
    "      border-radius: 50%%\n" +
    "      text-align: center\n" +
    "      background-color: %s\n" +
    "      opacity: %d%%\n" +
    "      transform: translate(-50%%, -50%%) scale(%d%%)\n" +
    "    tap_action:\n" +
    "      action: %s\n" +
    "    double_tap_action:\n" +
    "      action: %s\n" +
    "    hold_action:\n" +
    "      action: %s\n",
    displayTypeToYamlString.get(displayType), 
    name, 
    title, 
    additionalYaml, 
    position.y, 
    position.x, 
    backgroundColor, 
    opacity, 
    scale,
    actionYaml(tapAction, tapActionValue), 
    actionYaml(doubleTapAction, doubleTapActionValue), 
    actionYaml(holdAction, holdActionValue)
);
```

**Beispiel-Ausgabe** (Binary Sensor mit Defaults):
```yaml
- type: state-icon
  entity: binary_sensor.bewegung_flur
  title: Bewegungsmelder Flur
  style:
    top: 45.23%
    left: 32.67%
    border-radius: 50%
    text-align: center
    background-color: rgba(255, 255, 255, 0.3)
    opacity: 100%
    transform: translate(-50%, -50%) scale(100%)
  tap_action:
    action: more-info
  double_tap_action:
    action: none
  hold_action:
    action: more-info
```

**Mit Icon Override**:
```yaml
- type: state-icon
  entity: binary_sensor.bewegung_flur
  title: null
  icon: mdi:run
  style:
    # ... (gleich wie oben)
```

**Mit Display Condition = WHEN_ON**:
```yaml
- type: conditional
  conditions:
    - condition: state
      entity: binary_sensor.bewegung_flur
      state: on
  elements:
    - type: state-icon
      entity: binary_sensor.bewegung_flur
      title: null
      style:
        # ... (gleich wie oben, aber eingerückt)
```

**Mit Display Condition = AVAILABLE**:
```yaml
- type: conditional
  conditions:
    - condition: state
      entity: binary_sensor.bewegung_flur
      state_not: unavailable
  elements:
    - type: state-icon
      # ... (wie oben)
```

---

## 5. RENDERING-VERHALTEN

### Kein 3D-Rendering für Binary Sensors

Code: Entity.java, Zeilen 490-494
```java
if (isLight)
    setLightPower(true);

if (isDoorOrWindow)
    setDoorOrWindowState(true);
```

**Bedeutung**:
- Nur Lights ändern Power (→ Licht-Effekt im 3D-Render)
- Nur Doors/Windows ändern Geometrie (→ Offen/Geschlossen im Render)
- **Binary Sensors machen GAR NICHTS im 3D-Render**

**Binary Sensors sind reine UI-Overlays** - sie werden nur als Icons/Labels auf dem fertigen Floorplan angezeigt, nicht in den 3D-Renders selbst.

---

## 6. TITEL / TOOLTIP

Code: Entity.java, Zeilen 633
```java
title = firstPiece.getDescription();
```

**Sweet Home 3D**:
- Setze die **Beschreibung** des Möbelstücks in SH3D
- Diese wird als `title` im YAML verwendet
- Zeigt Tooltip beim Hover in Home Assistant

---

## 7. PERSISTIERUNG DER EINSTELLUNGEN

Code: Entity.java, Zeilen 41-61 (Setting-Namen)
```java
private static final String SETTING_NAME_DISPLAY_TYPE = "displayType";
private static final String SETTING_NAME_ICON_OVERRIDE = "iconOverride";
// ... etc
```

**Speicher-Schlüssel-Format**:
```
{entity_name}.{setting_name}
```

**Beispiele**:
- `binary_sensor.bewegung_flur.displayType` = `ICON`
- `binary_sensor.bewegung_flur.iconOverride` = `mdi:run`
- `binary_sensor.bewegung_flur.displayCondition` = `WHEN_ON`

**Geänderte Werte werden rot markiert** (EntityOptionsPanel.java, Zeilen 654-672):
```java
private void markModified() {
    Color modifiedColor = new Color(200, 0, 0);
    
    displayTypeLabel.setForeground(entity.isDisplayTypeModified() ? modifiedColor : Color.BLACK);
    iconOverrideLabel.setForeground(entity.isIconOverrideModified() ? modifiedColor : Color.BLACK);
    // ...
}
```

---

## 8. ZUSAMMENFASSUNG: BINARY SENSOR vs LIGHT vs DOOR/WINDOW

| Feature | Light | Door/Window | Binary Sensor |
|---------|-------|-------------|---------------|
| **3D-Rendering** | ✅ Licht-Effekt | ✅ Offen/Geschlossen | ❌ Kein Rendering |
| **Default Display Type** | ICON | ICON | ICON |
| **Default Tap Action** | TOGGLE | TOGGLE | MORE_INFO |
| **Always On Option** | ✅ | ❌ | ❌ |
| **Is RGB Option** | ✅ | ❌ | ❌ |
| **Open Condition** | ❌ | ✅ | ❌ |
| **Display Furniture Condition** | ❌ | ❌ | ✅ |
| **Icon Override** | ✅ | ✅ | ✅ |
| **Display Condition** | ✅ | ✅ | ✅ |
| **Position Override** | ✅ | ✅ | ✅ |
| **Opacity/Scale** | ✅ | ✅ | ✅ |

---

## 9. PRAKTISCHES BEISPIEL: BEWEGUNGSMELDER

### In Sweet Home 3D
```
Möbelname: binary_sensor.bewegung_kueche
Beschreibung: Bewegungsmelder Küche
```

### Im Plugin konfiguriert
- Display Type: ICON
- Icon Override: `mdi:motion-sensor`
- Display Condition: WHEN_ON
- Tap Action: MORE_INFO
- Opacity: 80
- Background Color: `rgba(255, 0, 0, 0.5)` (rot)

### Generiertes YAML
```yaml
- type: conditional
  conditions:
    - condition: state
      entity: binary_sensor.bewegung_kueche
      state: on
  elements:
    - type: state-icon
      entity: binary_sensor.bewegung_kueche
      title: Bewegungsmelder Küche
      icon: mdi:motion-sensor
      style:
        top: 68.45%
        left: 34.12%
        border-radius: 50%
        text-align: center
        background-color: rgba(255, 0, 0, 0.5)
        opacity: 80%
        transform: translate(-50%, -50%) scale(100%)
      tap_action:
        action: more-info
      double_tap_action:
        action: none
      hold_action:
        action: more-info
```

### In Home Assistant
- **Wenn `binary_sensor.bewegung_kueche` = "on"**: Rotes Motion-Icon erscheint
- **Wenn "off" oder "unavailable"**: Icon verschwindet
- **Beim Klick**: More-Info-Dialog öffnet sich

---

## 10. WICHTIGE ERKENNTNISSE

### ✅ WAS FUNKTIONIERT
1. Binary Sensors werden automatisch erkannt via Namens-Präfix
2. Standard-Icon kann überschrieben werden
3. Display Conditions funktionieren einwandfrei (on/off/available)
4. Position kann manuell korrigiert werden
5. Alle visuellen Anpassungen (opacity, scale, color) verfügbar

### ❌ WAS NICHT FUNKTIONIERT / NICHT VORHANDEN
1. **KEIN automatisches Icon-Switching basierend auf State**
   - Ein Icon für beide States (on/off)
   - Keine device_class-Integration
2. **KEIN 3D-Rendering**
   - Binary Sensors erscheinen nicht in den 3D-Renders
   - Nur als Overlays auf dem fertigen Bild
3. **KEINE Light-Features**
   - Kein "Always On"
   - Kein RGB/Dimming
4. **Standard-Action ist MORE_INFO, nicht TOGGLE**
   - Muss manuell auf TOGGLE geändert werden wenn gewünscht

---

## 11. BEKANNTE PROBLEME (aus Issues)

### Icon-Positionierung (Issue #82)
- Automatische Position kann bei verschiedenen Render-Auflösungen falsch sein
- **Workaround**: Position manuell im Plugin korrigieren

### Fehlende State-Icons
- **Problem**: Plugin generiert nur EIN Icon, kein State-basiertes Switching
- **Workaround**: Nutze Display Condition + mehrere Entities mit verschiedenen Icons

---

## QUELLEN

- **Entity.java** (795 Zeilen): Hauptlogik für Entity-Handling
- **EntityOptionsPanel.java** (705 Zeilen): UI-Optionen und Layout
- **Repository**: https://github.com/shmuelzon/home-assistant-floor-plan
- **Commit**: master branch (Stand März 2026)
