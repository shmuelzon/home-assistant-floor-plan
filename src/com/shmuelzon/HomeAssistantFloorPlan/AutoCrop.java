package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class AutoCrop {
    public static final Color CROP_COLOR = new Color(0, 255, 0);
    private static final int CROP_TOLERANCE = 10;

    private boolean isBackgroundColor(int color1, int background, int tolerance) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (background >> 16) & 0xFF;
        int g2 = (background >> 8) & 0xFF;
        int b2 = background & 0xFF;

        return Math.abs(r1 - r2) <= tolerance && Math.abs(g1 - g2) <= tolerance && Math.abs(b1 - b2) <= tolerance;
    }

    public Rectangle findCropArea(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int background = CROP_COLOR.getRGB();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isBackgroundColor(image.getRGB(x, y), background, CROP_TOLERANCE)) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX == -1) {
            // Image is all background
            return new Rectangle(0, 0, width, height);
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public BufferedImage crop(BufferedImage image, Rectangle cropArea) {
        if (cropArea == null || (cropArea.x == 0 && cropArea.y == 0 && cropArea.width == image.getWidth() && cropArea.height == image.getHeight())) {
            return image;
        }
        return image.getSubimage(cropArea.x, cropArea.y, cropArea.width, cropArea.height);
    }

    public BufferedImage makeTransparent(BufferedImage image, int tolerance) {
        BufferedImage dest = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int background = CROP_COLOR.getRGB();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if (!isBackgroundColor(pixel, background, tolerance)) {
                    dest.setRGB(x, y, pixel | 0xFF000000);
                }
            }
        }
        return dest;
    }
}
