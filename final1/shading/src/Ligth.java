import java.awt.Color;

/**
 * Base class for all lights in the scene.
 */
public abstract class Ligth {

    protected Color color;

    protected double intensity;

    public Ligth(Color color, double intensity) {
        this.color = color;
        this.intensity = intensity;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    // Calculate the Lambert diffuse factor
    public abstract double getNDotL(Intersection intersection);

    // Return the direction from the surface to the light
    public abstract Vector3D getLightDirection(
            Intersection intersection
    );

    // Return the distance from the point to the light
    public abstract double getDistance(
            Intersection intersection
    );
}