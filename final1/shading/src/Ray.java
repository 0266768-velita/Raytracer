/**
 * Represents a ray in 3D space.
 */
public class Ray {

    // Starting point of the ray
    private final Vector3D origin;

    // Direction of the ray
    private final Vector3D direction;

    // Create a ray
    public Ray(
            Vector3D origin,
            Vector3D direction
    ) {

        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Vector3D getOrigin() {
        return origin;
    }

    public Vector3D getDirection() {
        return direction;
    }

    // Get a point along the ray
    public Vector3D getPoint(double t) {

        return origin.add(
                direction.multiply(t)
        );
    }

    // Create a small offset to avoid self-intersections
    public Ray offset(
            Vector3D normal,
            double epsilon
    ) {

        return new Ray(
                origin.add(
                        normal.multiply(epsilon)
                ),
                direction
        );
    }

    @Override
    public String toString() {

        return "Ray(origin="
                + origin
                + ", direction="
                + direction
                + ")";
    }
}