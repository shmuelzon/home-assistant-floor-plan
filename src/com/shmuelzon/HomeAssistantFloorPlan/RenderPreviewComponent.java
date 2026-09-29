package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.eteks.sweethome3d.swing.ScaledImageComponent;
import com.eteks.sweethome3d.swing.SwingTools;

/* Displays the image currently being rendered, bucket by bucket, like Sweet Home 3D's photo panel */
@SuppressWarnings("serial")
public class RenderPreviewComponent extends ScaledImageComponent {
    private static final int FRAME_THICKNESS = Math.round(6 * SwingTools.getResolutionScale());
    private static final int SHADOW_OFFSET = Math.round(4 * SwingTools.getResolutionScale());

    private final Border imageBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    private int placeholderWidth = 16;
    private int placeholderHeight = 9;

    public RenderPreviewComponent() {
        super(null, true);
        setBorder(BorderFactory.createEmptyBorder(FRAME_THICKNESS, FRAME_THICKNESS, FRAME_THICKNESS + SHADOW_OFFSET, FRAME_THICKNESS + SHADOW_OFFSET));
        setOpaque(false);
    }

    /* Size (or just aspect ratio) of the frame displayed until the first image is available */
    public void setPlaceholderSize(int width, int height) {
        placeholderWidth = width;
        placeholderHeight = height;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        BufferedImage image = getImage();
        Rectangle imageBounds = image != null
            ? getScaledBounds(image.getWidth(), image.getHeight())
            : getScaledBounds(placeholderWidth, placeholderHeight);

        /* Only paint a frame around the (scaled) image so the rest of the panel remains visible */
        int x = imageBounds.x - FRAME_THICKNESS;
        int y = imageBounds.y - FRAME_THICKNESS;
        int frameWidth = imageBounds.width + 2 * FRAME_THICKNESS;
        int frameHeight = imageBounds.height + 2 * FRAME_THICKNESS;

        g.setColor(new Color(0, 0, 0, 64));
        g.fillRect(x + SHADOW_OFFSET, y + SHADOW_OFFSET, frameWidth, frameHeight);
        Color background = UIManager.getColor("Panel.background");
        g.setColor(background != null ? background : getBackground());
        g.fillRect(x, y, frameWidth, frameHeight);
        imageBorder.paintBorder(this, g, imageBounds.x - 2, imageBounds.y - 2, imageBounds.width + 4, imageBounds.height + 4);

        if (image != null) {
            paintImage(g, null);
        } else {
            /* Scene is still being prepared by the renderer, match the renderer's initial black image */
            g.setColor(Color.BLACK);
            g.fillRect(imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height);
        }
    }

    private Rectangle getScaledBounds(int width, int height) {
        Insets insets = getInsets();
        int availableWidth = getWidth() - insets.left - insets.right;
        int availableHeight = getHeight() - insets.top - insets.bottom;
        float scale = Math.min((float)availableWidth / width, (float)availableHeight / height);
        int scaledWidth = Math.round(width * scale);
        int scaledHeight = Math.round(height * scale);
        return new Rectangle(insets.left + (availableWidth - scaledWidth) / 2,
            insets.top + (availableHeight - scaledHeight) / 2, scaledWidth, scaledHeight);
    }

    /* Called by the photo renderer (on the event dispatch thread) whenever a part of the image changes */
    @Override
    public boolean imageUpdate(Image image, int infoFlags, int x, int y, int width, int height) {
        if (image != getImage() && image instanceof BufferedImage)
            setImage((BufferedImage)image);
        else
            repaint();
        return true;
    }
}
