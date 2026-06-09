import java.awt.Color;

// sphere primitive
public class Sphere implements Object3D {

    private Vector3D center;
    private double   radius;
    private Material material;

    public Sphere(Vector3D center, double radius, Material material) {
        this.center   = center;
        this.radius   = radius;
        this.material = material;
    }

    // compatibility constructor for code that passes a Color directly
    public Sphere(Vector3D center, double radius, Color color) {
        this(center, radius, Material.diffuse(color));
    }

    // analytic ray-sphere intersection using the quadratic formula
    @Override
    public Intersection intersect(Ray ray) {
        Vector3D oc = ray.getOrigin().subtract(center);

        double a = ray.getDirection().dot(ray.getDirection());
        double b = 2.0 * oc.dot(ray.getDirection());
        double c = oc.dot(oc) - radius * radius;

        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) return null;

        double sqrtD = Math.sqrt(discriminant);
        double t     = (-b - sqrtD) / (2.0 * a);

        // try the far intersection if the near one is behind the ray
        if (t < 1e-6) {
            t = (-b + sqrtD) / (2.0 * a);
            if (t < 1e-6) return null;
        }

        Vector3D point  = ray.getPoint(t);
        Vector3D normal = point.subtract(center).normalize();

        return new Intersection(t, point, normal, material);
    }

    @Override public Material getMaterial()               { return material; }
    @Override public void setMaterial(Material material)  { this.material = material; }

    public Vector3D getCenter()           { return center; }
    public void     setCenter(Vector3D c) { this.center = c; }

    public double getRadius()             { return radius; }
    public void   setRadius(double r)     { this.radius = r; }
}