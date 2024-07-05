# Home Assistant Floor Plan Plugin for Sweet Home 3D

This project is a plugin for the [Sweet Home 3D](https://www.sweethome3d.com/)
interior design application.
It allows creating a 3D rendered floor plan panel for Home Assistant that
displays your home's current lighting state along with icons for toggling each
light, sensors and cameras.

<img src="doc/demo.gif" />

## Features

- **3D Rendered Floor Plan**: Integrates with Sweet Home 3D to generate images automatically to your floorplan (displays current lighting state, sensors, and other entities with interactive icons for toggling lights).
- **Rendering Modes**: Allow user to select the best rendering option to generate his images from his Sweet Home 3D project  
- **Complete Renders Mode**: Renders images for all possible light combinations and rooms.
- **Configurations**:
  - Group detected lights by room.
  - Adjust output resolution (Width / Height).
  - Adjust output quality (High / Low).
  - Progress bar indicates rendering status.
  - Create state icons for multiple entities from Home Assistant based on their names
  - Lights is detected alone or within groups under SH3D
  - Use existing exported renders to process quicker than the first time
  
## Rendering Modes

This plugin supports 3 modes for rendering the different lights

### CSS

In this (recommended) mode a base image is generated with all the lights turned
off. Then, for each light source, a new image is rendered with only it turned
on. The floor plan YAML then instructs the browser how to mix the different
lights sources from each render when multiple light sources interact with each
other.

This method offers good results with a low number of required images to render.

### Room Overlay

The floor plan is comprised of one base/background image without any of the
lights turned on. Then, for each light, it generates an overlay image where only
changed pixels are included and the rest of the image is transparent. This
allows for overlaying multiple images, with multiple lights turned on together
without the need for different renders for all possible combinations.

In order to get the best results for lights that do interact with each other,
i.e., the lights that appear in the same room, will be rendered with all
possible combinations. This approach significantly reduces the number of
rendered images, compared to all possible combinations of the entire floor.

### Complete Renders

This mode renders separate images for all possible light combinations on the
rendered floor. It requires generating many renders but offers the best quality.

## Usage

1. Download the plugin from the [releases](../../releases/latest) page and
   install it
2. Prepare your model to fit with the [criteria](#preparation) of this plugin
3. Start the plugin by clicking the "Tools"->"Home Assistant Floor Plan" menu
4. Modify the [configuration options](#configuration-options) accordingly
5. Click "Start"
6. Copy the generated `floorplan.yml` file and all images under `floorplan` folder to your HA path

## Configuration Options

<img src="doc/options.png" />

The configuration window displays a list of detected lights grouped according to
the room they're located in. Please verify the list matches your expectations.

* Width / Height - Configure the required output resolution of the rendered
  images
* Light mixing mode - See [Rendering Modes](#rendering-modes)
* Sensitivity - [1, 100] The degree by which two pixels need to be different
  from one another to be taken into account for the generated overlay image.
  Only relevant for "room overlay" light mixing mode
* Output directory - The location on your PC where the floor plan images and
  YAML will be saved

The progress bar at the bottom will indicate how many images need to be rendered
for the complete floor plan and will progress as they are ready.

## Preparation

* Set each light in SW3D with the entity name of Home Assistant, i.e.,
  `light.xxx`, `switch.xxx`
* Only light sources that are **visible** and have a **power > 0%** are considered
* If you have multiple light sources that are controlled by the same switch,
  e.g., spot lights, give them all the same name
* If you have lights withing groups, , give them all the same name
* If lights from two different rooms do interact with each other, e.g., there's
  a glass door separating the rooms, you can give both rooms the same name and
  they'll be treated as one
* To include state icons and labels for sensors, set the relevant furniture name
  to the entity name of Home Assistant, available entities for this plugin:
  - `binary_sensor.xxx`
  - `camera.xxx`
  - `climate.xxx`
  - `cover.xxx`
  - `fan.xxx`
  - `media_player.xxx`
  - `remote.xxx`
  - `sensor.xxx`
  - `switch.xxx`
  - `vacuum.xxx`

## Suggestions

For best results, it's suggested to:
* Set the 3D view's time to 8:00 AM and disable ceiling lights

When using the "Room overlay" light mixing mode, it's also suggested to:
* Use a dark background for the 3D view
  * It can later be converted to transparent using an image editor
* Close all the doors between individually lighted rooms

## Frequently Asked Questions

1. **Where should I copy the generated files and what should I copy?**  
  After the process is complete, copy the floorplan folder and floorplan.yaml to your Home Assistant path, specifically to: \config\www.

2. **How do I select the desired perspective for rendering?**  
  Before activating the plugin, set the SH3D project to the specific 3D point of view that you want to be rendered.

3. **How do I change the rendering settings?**  
  Prior to activating the plugin, go to “Create photo…” in the SH3D project and adjust the settings there. You do not need to render or save anything; simply make the changes and close the dialog.

4. **Can I work on the SH3D project while rendering?**  
  No, do not make any changes or interact with the 3D view while it’s rendering. These actions will be captured by the plugin as it scripts the renders one by one. Start the rendering process and then leave it to complete.

5. **What's the difference between `renders` and `floorplan` folders?**  
  The renders directory includes the rendered images as generated by SH3D. The floorplan directory includes the images you need to copy over to Home Assistant. With the CSS option, both directories are pretty much the same, except that the base image (with all the lights turned off) exists only in the floorplan directory. This is because in this mode, we let the browser to the light blending and this plugin doesn't need to do anything special.
  With the room overlay mode, the renders, again, includes all the images generated by SH3D while the floorplan directory will include the processed images with the transparent background.
  The renders directory was created (back when room overlay was the only option) in case one wanted to change any parameters (like the sensitivity) to generate a new floor plan without needing to wait for all the renders from SH3D, which don't change. It's basically a cache of rendered images.  

6. **What's the use of `Use existing renders` option?**  
  Technically this option can work with any mode. You can start with the CSS mode and generate the least number of needed renders but then decide to switch to room overlay. When doing so, you already have part of the required renders and there's no need to generate them again. If you started with room overlay and switched to CSS, you'll have all of the required images and the whole process will be done in a few seconds

## Possible Future Enhancements
- [ ] Allow selecting renderer (SunFlow/Yafaray)
- [x] Allow selecting quality (high/low)
- [ ] Allow selecting date/time of render
- [ ] Create multiple renders for multiple hours of the day and display in Home
      Assistant according to local time
- [ ] Allow stopping rendering thread
- [ ] Allow enabling/disabling/configuring state-icon
- [x] Support including sensors state-icons/labels for other items
- [ ] Support fans with animated gif/png with css3 image rotation
- [x] Make sure state-icons/labels do not overlap
- [x] Allow using existing rendered images and just re-create overlays and YAML
- [ ] After rendering is complete, show preview of overlay images
- [ ] Allow overriding state-icons/labels positions, and save persistently
- [ ] Allow defining, per entity, if it should be an icon or label, and save
      that persistently