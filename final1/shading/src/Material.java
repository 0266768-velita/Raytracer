import java.awt.Color;

/**
 * Material used by objects in the scene.
 */
public class Material {

    private Color color;

    private double ambient;
    private double diffuse;
    private double specular;

    private double shininess;

    private double reflection;

    private double refraction;

    private double refractiveIndex;

    // Create a material with all properties
    public Material(Color color,
                    double ambient,
                    double diffuse,
                    double specular,
                    double shininess,
                    double reflection,
                    double refraction,
                    double refractiveIndex) {

        this.color = color;

        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;

        this.shininess = shininess;

        this.reflection = reflection;

        this.refraction = refraction;

        this.refractiveIndex = refractiveIndex;
    }

    // Create a basic diffuse material
    public Material(Color color) {

        this(
                color,
                0.10,
                0.90,
                0.20,
                32.0,
                0.0,
                0.0,
                1.0
        );
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getAmbient() {
        return ambient;
    }

    public void setAmbient(double ambient) {
        this.ambient = ambient;
    }

    public double getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(double diffuse) {
        this.diffuse = diffuse;
    }

    public double getSpecular() {
        return specular;
    }

    public void setSpecular(double specular) {
        this.specular = specular;
    }

    public double getShininess() {
        return shininess;
    }

    public void setShininess(double shininess) {
        this.shininess = shininess;
    }

    public double getReflection() {
        return reflection;
    }

    public void setReflection(double reflection) {
        this.reflection = reflection;
    }

    public double getRefraction() {
        return refraction;
    }

    public void setRefraction(double refraction) {
        this.refraction = refraction;
    }

    public double getRefractiveIndex() {
        return refractiveIndex;
    }

    public void setRefractiveIndex(double refractiveIndex) {
        this.refractiveIndex = refractiveIndex;
    }

    // Create a diffuse material
    public static Material diffuse(Color color) {
        return new Material(color);
    }

    // Create a mirror material
    public static Material mirror(Color color) {

        return new Material(
                color,
                0.05,
                0.30,
                1.00,
                256.0,
                1.00,
                0.00,
                1.00
        );
    }

    // Create a glass material
    public static Material glass(Color color) {

        return new Material(
                color,
                0.05,
                0.20,
                1.00,
                256.0,
                0.10,
                0.90,
                1.50
        );
    }

    // Create a metallic material
    public static Material metal(Color color) {

        return new Material(
                color,
                0.05,
                0.60,
                1.00,
                128.0,
                0.50,
                0.00,
                1.00
        );
    }
}