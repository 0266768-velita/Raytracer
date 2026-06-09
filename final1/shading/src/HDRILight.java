import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * Loads an HDRI image and uses it for the sky background
 * and ambient lighting.
 */
public class HDRILight {

    private BufferedImage image;
    private int width;
    private int height;

    private float[] pixels;

    private boolean isHDR = false;

    private double exposure = 0.8;

    public HDRILight(String path) {

        // Try different file locations
        String[] candidates = {
                path,
                path.replace(".exr", ".jpg"),
                path.replace(".exr", ".png"),
                "models/textures/sky.jpg",
                "models/textures/sky.png",
                "models/textures/sky.exr",
                "models/sky.jpg",
                "models/sky.exr"
        };

        for (String candidate : candidates) {

            try {

                File file = new File(candidate);

                if (!file.exists()) {
                    continue;
                }

                image = ImageIO.read(file);

                if (image != null) {

                    width = image.getWidth();
                    height = image.getHeight();

                    isHDR = candidate.toLowerCase().endsWith(".exr");

                    System.out.println(
                            "HDRI loaded: "
                                    + width + "x" + height
                                    + " -> "
                                    + candidate
                    );

                    return;
                }

            } catch (Exception e) {
                // Try the next file
            }
        }

        System.out.println(
                "HDRI not found, using procedural sky."
        );
    }

    public void setExposure(double exposure) {
        this.exposure = exposure;
    }

    // Get a color from the HDRI using a ray direction
    public Color sample(double dx, double dy, double dz) {

        if (image == null) {
            return proceduralSky(dx, dy, dz);
        }

        // Normalize the direction
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length < 1e-10) {
            return Color.BLACK;
        }

        dx /= length;
        dy /= length;
        dz /= length;

        // Convert direction to UV coordinates
        double phi = Math.atan2(dz, dx);

        double theta = Math.asin(
                Math.max(-1, Math.min(1, dy))
        );

        double u = (phi / (2 * Math.PI) + 0.5);
        double v = (theta / Math.PI + 0.5);

        int px = (int) (u * (width - 1)) % width;

        int py = (int) ((1.0 - v) * (height - 1)) % height;

        px = Math.max(0, Math.min(width - 1, px));
        py = Math.max(0, Math.min(height - 1, py));

        Color color = new Color(image.getRGB(px, py));

        return applyExposure(color);
    }

    // Calculate an average sky color for ambient light
    public Color getAmbientColor() {

        if (image == null) {
            return new Color(180, 210, 240);
        }

        double r = 0;
        double g = 0;
        double b = 0;

        int samples = 64;

        for (int i = 0; i < samples; i++) {

            double phi =
                    (i / (double) samples) * 2 * Math.PI;

            double theta = Math.PI * 0.3;

            double dx = Math.cos(theta) * Math.cos(phi);
            double dy = Math.sin(theta);
            double dz = Math.cos(theta) * Math.sin(phi);

            Color color = sample(dx, dy, dz);

            r += color.getRed() / 255.0;
            g += color.getGreen() / 255.0;
            b += color.getBlue() / 255.0;
        }

        r /= samples;
        g /= samples;
        b /= samples;

        // Make the ambient light softer
        return ColorTools.toColor(
                r * 0.25,
                g * 0.25,
                b * 0.25
        );
    }

    // Generate a simple sky if no HDRI is available
    private Color proceduralSky(double dx, double dy, double dz) {

        double t = Math.max(0, dy);

        Color zenith = new Color(100, 160, 255);
        Color horizon = new Color(220, 235, 255);

        return ColorTools.lerp(
                horizon,
                zenith,
                t * t
        );
    }

    // Apply exposure to the sampled color
    private Color applyExposure(Color color) {

        return ColorTools.toColor(
                Math.min(1,
                        color.getRed() / 255.0 * exposure),

                Math.min(1,
                        color.getGreen() / 255.0 * exposure),

                Math.min(1,
                        color.getBlue() / 255.0 * exposure)
        );
    }
}