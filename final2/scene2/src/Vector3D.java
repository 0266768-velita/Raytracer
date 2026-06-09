public class Vector3D {

    public final double x;
    public final double y;
    public final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D negate() {
        return new Vector3D(-x, -y, -z);
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(x - other.x, y - other.y, z - other.z);
    }

    // scalar multiply
    public Vector3D multiply(double scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }

    // component-wise multiply
    public Vector3D multiply(Vector3D other) {
        return new Vector3D(x * other.x, y * other.y, z * other.z);
    }

    // returns zero vector if scalar is near zero
    public Vector3D divide(double scalar) {
        if (Math.abs(scalar) < 1e-9) return new Vector3D(0, 0, 0);
        return new Vector3D(x / scalar, y / scalar, z / scalar);
    }

    public double dot(Vector3D other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    // returns zero vector if magnitude is near zero
    public Vector3D normalize() {
        double magnitude = length();
        if (magnitude < 1e-9) return new Vector3D(0, 0, 0);
        return divide(magnitude);
    }

    // reflection vector: R = I - 2(I·N)N
    public Vector3D reflect(Vector3D normal) {
        return subtract(normal.multiply(2.0 * dot(normal))).normalize();
    }

    // refraction via Snell's law — falls back to reflection on total internal reflection
    public Vector3D refract(Vector3D normal, double eta) {
        double cosi = Math.max(-1.0, Math.min(1.0, dot(normal)));
        double k    = 1.0 - eta * eta * (1.0 - cosi * cosi);
        if (k < 0.0) return reflect(normal); // total internal reflection
        return multiply(eta)
                .subtract(normal.multiply(eta * cosi + Math.sqrt(k)))
                .normalize();
    }

    public double distance(Vector3D other) {
        return subtract(other).length();
    }

    public Vector3D midpoint(Vector3D other) {
        return add(other).multiply(0.5);
    }

    // clamp each component to [min, max]
    public Vector3D clamp(double min, double max) {
        return new Vector3D(
                Math.max(min, Math.min(max, x)),
                Math.max(min, Math.min(max, y)),
                Math.max(min, Math.min(max, z)));
    }

    // sum of all components
    public double sum() {
        return x + y + z;
    }

    public static final Vector3D ZERO = new Vector3D(0, 0, 0);

    @Override
    public String toString() {
        return String.format("Vector3D(%.3f, %.3f, %.3f)", x, y, z);
    }
}