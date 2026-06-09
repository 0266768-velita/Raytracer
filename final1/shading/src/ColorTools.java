import java.awt.Color;


public class ColorTools {

    // Add two colors together
    public static Color add(Color a, Color b) {

        double r = a.getRed() / 255.0 + b.getRed() / 255.0;
        double g = a.getGreen() / 255.0 + b.getGreen() / 255.0;
        double bl = a.getBlue() / 255.0 + b.getBlue() / 255.0;

        return toColor(r, g, bl);
    }

    // Multiply a color by a number
    public static Color scale(Color c, double s) {

        return toColor(
                c.getRed() / 255.0 * s,
                c.getGreen() / 255.0 * s,
                c.getBlue() / 255.0 * s
        );
    }

    // Multiply two colors component by component
    public static Color multiply(Color a, Color b) {

        return toColor(
                (a.getRed() / 255.0) * (b.getRed() / 255.0),
                (a.getGreen() / 255.0) * (b.getGreen() / 255.0),
                (a.getBlue() / 255.0) * (b.getBlue() / 255.0)
        );
    }

    // Blend between two colors
    public static Color lerp(Color a, Color b, double t) {

        t = Math.max(0, Math.min(1, t));

        return toColor(
                a.getRed() / 255.0 +
                        (b.getRed() / 255.0 - a.getRed() / 255.0) * t,

                a.getGreen() / 255.0 +
                        (b.getGreen() / 255.0 - a.getGreen() / 255.0) * t,

                a.getBlue() / 255.0 +
                        (b.getBlue() / 255.0 - a.getBlue() / 255.0) * t
        );
    }

    // Apply gamma correction
    public static Color gammaCorrect(Color c, double gamma) {

        double inverseGamma = 1.0 / gamma;

        return toColor(
                Math.pow(c.getRed() / 255.0, inverseGamma),
                Math.pow(c.getGreen() / 255.0, inverseGamma),
                Math.pow(c.getBlue() / 255.0, inverseGamma)
        );
    }

    // Compress high values using Reinhard tone mapping
    public static Color toneMapReinhard(Color c) {

        double r = c.getRed() / 255.0;
        double g = c.getGreen() / 255.0;
        double b = c.getBlue() / 255.0;

        return toColor(
                r / (1 + r),
                g / (1 + g),
                b / (1 + b)
        );
    }

    // Convert values to a valid Color object
    public static Color toColor(double r, double g, double b) {

        return new Color(
                (float) Math.max(0, Math.min(1, r)),
                (float) Math.max(0, Math.min(1, g)),
                (float) Math.max(0, Math.min(1, b))
        );
    }
}