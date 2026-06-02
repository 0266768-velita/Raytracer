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
//Calculate the nearest intersection between the ray and this sphere
    @Override
    public Intersection intersect(Ray ray) {
        //Vector from the origin of the ray to the center of the sphere
        //Represents the displacement
        Vector3D oc = ray.origin.subtract(center);

        double a = ray.direction.dot(ray.direction);
        double b = 2.0 * oc.dot(ray.direction);
        double c = oc.dot(oc) - radius * radius;

        double discriminant = b * b - 4.0 * a * c;
//The minor root is negative: the origin of the ray is inside the
//sphere
        if (discriminant < 0.0) {
            return null; // No intersection
        }

        double t = (-b - Math.sqrt(discriminant)) / (2.0 * a);

        if (t < 0.0) {
            // Try the other root (ray started inside the sphere)
            t = (-b + Math.sqrt(discriminant)) / (2.0 * a);
            if (t < 0.0) return null;
        }

        Vector3D point = ray.getPoint(t);

        // Normal smooth: unit vector from the center to the point of impact
        //Varying this value per pixel produces a continuous lighting gradient
        Vector3D normal = point.subtract(center).normalize();

        return new Intersection(t, point, normal, color);
    }
}