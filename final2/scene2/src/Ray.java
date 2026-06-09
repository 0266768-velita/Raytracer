// ray in 3D space defined by an origin and a normalized direction
public class Ray {

    private final Vector3D origin;
    private final Vector3D direction; // always normalized

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin    = origin;
        this.direction = direction.normalize();
    }

    public Vector3D getOrigin()    { return origin; }
    public Vector3D getDirection() { return direction; }

    // P(t) = O + tD
    public Vector3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }

    // returns a new ray nudged along the normal to avoid self-intersections
    public Ray offset(Vector3D normal, double epsilon) {
        return new Ray(origin.add(normal.multiply(epsilon)), direction);
    }

    @Override
    public String toString() {
        return "Ray(origin=" + origin + ", direction=" + direction + ")";
    }
}