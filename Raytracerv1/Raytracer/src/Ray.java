public class Ray {

    public final Vector3D origin;     // principal point
    public final Vector3D direction;  // direction ray

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize(); //transform in normalize direction
    }

    // Gets a point along the ray
    public Vector3D getPoint(double t) {
        return origin.add(direction.multiply(t));
    }

    @Override
    public String toString() {
        return "Ray(origin=" + origin + ", direction=" + direction + ")";
    }
}