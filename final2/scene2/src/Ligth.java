import java.awt.Color;

// base class for all scene lights
public abstract class Ligth {

    protected Color color;
    protected double intensity;

    public Ligth(Color color, double intensity) {
        this.color = color;
        this.intensity = intensity;
    }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }

    // Lambert diffuse term N·L
    public abstract double getNDotL(Intersection intersection);

    // normalized vector from the surface toward the light — used for Lambert, Blinn-Phong, shadows and reflections
    public abstract Vector3D getLightDirection(Intersection intersection);

    // distance from the hit point to the light — directional lights return infinity, point lights return the real distance
    public abstract double getDistance(Intersection intersection);
}