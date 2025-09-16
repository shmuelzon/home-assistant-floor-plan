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

    public BufferedImage crop(BufferedImage image, Rectangle cropArea, boolean maintainAspectRatio, int targetWidth, int targetHeight) {
        if (cropArea == null || (cropArea.x == 0 && cropArea.y == 0 && cropArea.width == image.getWidth() && cropArea.height == image.getHeight())) {
            if (maintainAspectRatio && (image.getWidth() != targetWidth || image.getHeight() != targetHeight)) {
                return resizeImage(image, targetWidth, targetHeight);
            }
            return image;
        }
        
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        int x = Math.max(0, Math.min(cropArea.x, imageWidth - 1));
        int y = Math.max(0, Math.min(cropArea.y, imageHeight - 1));
        int width = Math.max(1, Math.min(cropArea.width, imageWidth - x));
        int height = Math.max(1, Math.min(cropArea.height, imageHeight - y));
        
        if (x + width > imageWidth || y + height > imageHeight || width <= 0 || height <= 0) {
            if (maintainAspectRatio && (image.getWidth() != targetWidth || image.getHeight() != targetHeight)) {
                return resizeImage(image, targetWidth, targetHeight);
            }
            return image;
        }
        
        BufferedImage croppedImage = image.getSubimage(x, y, width, height);
        
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
