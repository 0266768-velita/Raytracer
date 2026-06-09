import java.awt.Color;

/*
 * Sphere object used in the scene.
 */
public class Sphere implements Object3D {

    private Vector3D center;
    private double radius;

    private Material material;

    // Create a sphere using a material
    public Sphere(
            Vector3D center,
            double radius,
            Material material
    ) {

        this.center = center;
        this.radius = radius;
        this.material = material;
    }

    // Simple constructor using only a color
    public Sphere(
            Vector3D center,
            double radius,
            Color color
    ) {

        this(
                center,
                radius,
                Material.diffuse(color)
        );
    }

    // Check if a ray hits the sphere
    @Override
    public Intersection intersect(Ray ray) {

        Vector3D oc =
                ray.getOrigin()
                        .subtract(center);

        double a =
                ray.getDirection()
                        .dot(ray.getDirection());

        double b =
                2.0 *
                        oc.dot(
                                ray.getDirection()
                        );

        double c =
                oc.dot(oc)
                        - radius * radius;

        double discriminant =
                b * b
                        - 4.0 * a * c;

        // No intersection
        if (discriminant < 0.0) {
            return null;
        }

        double sqrtD =
                Math.sqrt(discriminant);

        // Closest intersection
        double t =
                (-b - sqrtD)
                        / (2.0 * a);

        // Try the second solution
        if (t < 1e-6) {

            t =
                    (-b + sqrtD)
                            / (2.0 * a);

            if (t < 1e-6) {
                return null;
            }
        }

        // Hit position
        Vector3D point =
                ray.getPoint(t);

        // Surface normal
        Vector3D normal =
                point
                        .subtract(center)
                        .normalize();

        return new Intersection(
                t,
                point,
                normal,
                material
        );
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public void setMaterial(
            Material material
    ) {
        this.material = material;
    }

    public Vector3D getCenter() {
        return center;
    }

    public void setCenter(
            Vector3D center
    ) {
        this.center = center;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(
            double radius
    ) {
        this.radius = radius;
    }
}