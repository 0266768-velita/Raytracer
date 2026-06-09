import java.awt.Color;

// Abstract base class for all types of scene light
//emission geometry in lights
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

    public double getIntensity() {
        return intensity;
    }
    //Calculate the diffuse factor N·L between the surface normal and the direction
     //towards the light source

    public abstract double getNDotL(Intersection intersection);
}