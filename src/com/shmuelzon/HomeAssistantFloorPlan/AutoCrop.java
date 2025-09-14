package com.shmuelzon.HomeAssistantFloorPlan;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class AutoCrop {
    public static final Color CROP_COLOR = new Color(0, 255, 0);

    private boolean isBackgroundColor(int color1, int background, int tolerance) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (background >> 16) & 0xFF;
        int g2 = (background >> 8) & 0xFF;
        int b2 = background & 0xFF;

        return Math.abs(r1 - r2) <= tolerance && Math.abs(g1 - g2) <= tolerance && Math.abs(b1 - b2) <= tolerance;
    }

    public Rectangle findCropArea(BufferedImage image, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        int background = CROP_COLOR.getRGB();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!isBackgroundColor(image.getRGB(x, y), background, tolerance)) {
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

        // Add padding to ensure we don't crop too aggressively
        int padding = Math.min(width, height) / 50; // 2% padding
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(width - 1, maxX + padding);
        maxY = Math.min(height - 1, maxY + padding);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    public BufferedImage crop(BufferedImage image, Rectangle cropArea, boolean maintainAspectRatio, int targetWidth, int targetHeight) {
        if (cropArea == null || (cropArea.x == 0 && cropArea.y == 0 && cropArea.width == image.getWidth() && cropArea.height == image.getHeight())) {
            if (maintainAspectRatio && (image.getWidth() != targetWidth || image.getHeight() != targetHeight)) {
                return resizeImage(image, targetWidth, targetHeight);
            }
            return image;
        }
        
        BufferedImage croppedImage = image.getSubimage(cropArea.x, cropArea.y, cropArea.width, cropArea.height);
        
        if (maintainAspectRatio) {
            return resizeImage(croppedImage, targetWidth, targetHeight);
        }
        
        return croppedImage;
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        java.awt.Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }
}
