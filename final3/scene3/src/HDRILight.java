import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

// loads an HDRI (EXR or tonemapped JPG) and uses it for scene background and ambient lighting
public class HDRILight {

    private BufferedImage image;
    private int width, height;
    private float[] pixels; // linear RGB, values can exceed 1.0
    private boolean isHDR = false;
    private double exposure = 0.8; // exposure multiplier

    public HDRILight(String path) {
        // try the main path first, then several fallbacks
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
                File f = new File(candidate);
                if (!f.exists()) continue;
                image = ImageIO.read(f);
                if (image != null) {
                    width  = image.getWidth();
                    height = image.getHeight();
                    isHDR  = candidate.toLowerCase().endsWith(".exr");
                    System.out.println("HDRI loaded: " + width + "x" + height + " -> " + candidate);
                    return;
                }
            } catch (Exception e) { /* try next candidate */ }
        }
        System.out.println("HDRI not found — using procedural blue sky.");
    }

    public void setExposure(double exposure) { this.exposure = exposure; }

    // sample the HDRI for a given ray direction using equirectangular projection
    public Color sample(double dx, double dy, double dz) {
        if (image == null) return proceduralSky(dx, dy, dz);

        // normalize direction
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-10) return Color.BLACK;
        dx /= len; dy /= len; dz /= len;

        // equirectangular: phi = azimuth, theta = elevation
        double phi   = Math.atan2(dz, dx);                        // [-pi, pi]
        double theta = Math.asin(Math.max(-1, Math.min(1, dy)));  // [-pi/2, pi/2]

        double u = (phi / (2 * Math.PI) + 0.5);                   // [0, 1]
        double v = (theta / Math.PI + 0.5);                        // [0, 1]

        int px = (int)(u * (width  - 1)) % width;
        int py = (int)((1.0 - v) * (height - 1)) % height;
        px = Math.max(0, Math.min(width  - 1, px));
        py = Math.max(0, Math.min(height - 1, py));

        Color raw = new Color(image.getRGB(px, py));
        return applyExposure(raw);
    }

    // average ambient color — samples a grid of directions across the upper hemisphere
    public Color getAmbientColor() {
        if (image == null) return new Color(180, 210, 240);

        double r = 0, g = 0, b = 0;
        int samples = 64;
        for (int i = 0; i < samples; i++) {
            double phi   = (i / (double) samples) * 2 * Math.PI;
            double theta = Math.PI * 0.3; // sample mostly the upper sky
            double dx = Math.cos(theta) * Math.cos(phi);
            double dy = Math.sin(theta);
            double dz = Math.cos(theta) * Math.sin(phi);
            Color c = sample(dx, dy, dz);
            r += c.getRed()   / 255.0;
            g += c.getGreen() / 255.0;
            b += c.getBlue()  / 255.0;
        }
        r /= samples; g /= samples; b /= samples;
        // scale down so it reads as ambient, not full illumination
        return ColorTools.toColor(r * 0.25, g * 0.25, b * 0.25);
    }

    // fallback sky when no HDRI is loaded — blue at zenith, white at horizon
    private Color proceduralSky(double dx, double dy, double dz) {
        double t      = Math.max(0, dy);
        Color zenith  = new Color(100, 160, 255);
        Color horizon = new Color(220, 235, 255);
        return ColorTools.lerp(horizon, zenith, t * t);
    }

    private Color applyExposure(Color c) {
        return ColorTools.toColor(
                Math.min(1, c.getRed()   / 255.0 * exposure),
                Math.min(1, c.getGreen() / 255.0 * exposure),
                Math.min(1, c.getBlue()  / 255.0 * exposure)
        );
    }
}