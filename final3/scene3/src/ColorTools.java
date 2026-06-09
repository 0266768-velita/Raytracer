import java.awt.Color;

// color utility methods for ray tracing — all channels handled as doubles in [0,1] to avoid overflow
public class ColorTools {

    // add two colors, clamped to [0,1]
    public static Color add(Color a, Color b) {
        double r  = a.getRed()  / 255.0 + b.getRed()  / 255.0;
        double g  = a.getGreen()/ 255.0 + b.getGreen()/ 255.0;
        double bl = a.getBlue() / 255.0 + b.getBlue() / 255.0;
        return toColor(r, g, bl);
    }

    // multiply a color by a scalar
    public static Color scale(Color c, double s) {
        return toColor(
                c.getRed()  / 255.0 * s,
                c.getGreen()/ 255.0 * s,
                c.getBlue() / 255.0 * s
        );
    }

    // component-wise multiply — useful for tinting
    public static Color multiply(Color a, Color b) {
        return toColor(
                (a.getRed()  / 255.0) * (b.getRed()  / 255.0),
                (a.getGreen()/ 255.0) * (b.getGreen()/ 255.0),
                (a.getBlue() / 255.0) * (b.getBlue() / 255.0)
        );
    }

    // linear interpolation between two colors
    public static Color lerp(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return toColor(
                a.getRed()  / 255.0 + (b.getRed()  / 255.0 - a.getRed()  / 255.0) * t,
                a.getGreen()/ 255.0 + (b.getGreen()/ 255.0 - a.getGreen()/ 255.0) * t,
                a.getBlue() / 255.0 + (b.getBlue() / 255.0 - a.getBlue() / 255.0) * t
        );
    }

    // gamma correction — linear to sRGB
    public static Color gammaCorrect(Color c, double gamma) {
        double inv = 1.0 / gamma;
        return toColor(
                Math.pow(c.getRed()  / 255.0, inv),
                Math.pow(c.getGreen()/ 255.0, inv),
                Math.pow(c.getBlue() / 255.0, inv)
        );
    }

    // Reinhard tone mapping — maps HDR [0, inf) to [0, 1]
    public static Color toneMapReinhard(Color c) {
        double r = c.getRed()  / 255.0;
        double g = c.getGreen()/ 255.0;
        double b = c.getBlue() / 255.0;
        return toColor(r / (1 + r), g / (1 + g), b / (1 + b));
    }

    // convert doubles in [0,1] to a Color, clamped
    public static Color toColor(double r, double g, double b) {
        return new Color(
                (float) Math.max(0, Math.min(1, r)),
                (float) Math.max(0, Math.min(1, g)),
                (float) Math.max(0, Math.min(1, b))
        );
    }
}