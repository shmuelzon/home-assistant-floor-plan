# Home Assistant Floor Plan Plugin for Sweet Home 3D

This project is a plugin for the [Sweet Home 3D](https://www.sweethome3d.com/)
interior design application.
It allows creating a 3D rendered floor plan panel for Home Assistant that
displays your home's current lighting state along with icons for toggling each
light, sensors and cameras.

<img src="doc/demo.gif" />

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
  `light.xxx`
* Only light sources that are visible and have a power > 0% are considered
* If you have multiple light sources that are controlled by the same switch,
  e.g., spot lights, give them all the same name
* If lights from two different rooms do interact with each other, e.g., there's
  a glass door separating the rooms, you can give both rooms the same name and
  they'll be treated as one
* To include state icons and labels for sensors, set the relevant furniture name
  to the entity name of Home Assistant, i.e., `sensor.xxx`, `binary_sensor.xxx`
  or `camera.xxx`

## Suggestions

For best results, it's suggested to:
* Set the 3D view's time to 8:00 AM and disable ceiling lights

When using the "Room overlay" light mixing mode, it's also suggested to:
* Use a dark background for the 3D view
  * It can later be converted to transparent using an image editor
* Close all the doors between individually lighted rooms

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
