import java.awt.Color;

// material for scene objects — supports ambient, diffuse, Blinn-Phong specular, reflection, refraction and IOR
public class Material {

    private Color color;

    private double ambient;
    private double diffuse;
    private double specular;
    private double shininess;
    private double reflection;
    private double refraction;
    private double refractiveIndex;

    public Material(Color color,
                    double ambient,
                    double diffuse,
                    double specular,
                    double shininess,
                    double reflection,
                    double refraction,
                    double refractiveIndex) {
        this.color           = color;
        this.ambient         = ambient;
        this.diffuse         = diffuse;
        this.specular        = specular;
        this.shininess       = shininess;
        this.reflection      = reflection;
        this.refraction      = refraction;
        this.refractiveIndex = refractiveIndex;
    }

    // simple diffuse material with sensible defaults
    public Material(Color color) {
        this(
                color,
                0.10,   // ambient
                0.90,   // diffuse
                0.20,   // specular
                32.0,   // shininess
                0.0,    // reflection
                0.0,    // refraction
                1.0     // IOR
        );
    }

    public Color  getColor()                          { return color; }
    public void   setColor(Color color)               { this.color = color; }

    public double getAmbient()                        { return ambient; }
    public void   setAmbient(double ambient)          { this.ambient = ambient; }

    public double getDiffuse()                        { return diffuse; }
    public void   setDiffuse(double diffuse)          { this.diffuse = diffuse; }

    public double getSpecular()                       { return specular; }
    public void   setSpecular(double specular)        { this.specular = specular; }

    public double getShininess()                      { return shininess; }
    public void   setShininess(double shininess)      { this.shininess = shininess; }

    public double getReflection()                     { return reflection; }
    public void   setReflection(double reflection)    { this.reflection = reflection; }

    public double getRefraction()                     { return refraction; }
    public void   setRefraction(double refraction)    { this.refraction = refraction; }

    public double getRefractiveIndex()                            { return refractiveIndex; }
    public void   setRefractiveIndex(double refractiveIndex)      { this.refractiveIndex = refractiveIndex; }

    // factory helpers
    public static Material diffuse(Color color) { return new Material(color); }

    public static Material mirror(Color color) {
        return new Material(color, 0.05, 0.30, 1.00, 256.0, 1.00, 0.00, 1.00);
    }

    public static Material glass(Color color) {
        return new Material(color, 0.05, 0.20, 1.00, 256.0, 0.10, 0.90, 1.50);
    }

    public static Material metal(Color color) {
        return new Material(color, 0.05, 0.60, 1.00, 128.0, 0.50, 0.00, 1.00);
    }
}