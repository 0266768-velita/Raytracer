import java.awt.Color;

public class Sphere implements Object3D {

    public Vector3D center;
    public double radius;
    public Color color;

    public Sphere(Vector3D center, double radius, Color color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D oc = ray.origin.subtract(center);

        double a = ray.direction.dot(ray.direction);
        double b = 2.0 * oc.dot(ray.direction);
        double c = oc.dot(oc) - radius * radius;

        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) return null;

        double t = (-b - Math.sqrt(discriminant)) / (2.0 * a);

        if (t < 0) return null;

        Vector3D point = ray.getPoint(t);
        Vector3D normal = point.subtract(center).normalize();

        return new Intersection(t, point, normal, color);
    }
}