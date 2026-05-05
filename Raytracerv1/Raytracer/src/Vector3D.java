public class Vector3D {
    public final double x, y, z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    //invert directions
    public Vector3D negate() {
        return new Vector3D(-x, -y, -z);
    }
    // rest for find center
    public Vector3D subtract(Vector3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }
    // multiply to move the ray
    public Vector3D multiply(double scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }

    // multiply vectors to add colors
    public Vector3D multiply(Vector3D other) {
        return new Vector3D(this.x * other.x, this.y * other.y, this.z * other.z);
    }
    //addition of vectors to find their resultant
    public Vector3D add(Vector3D other) {
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }
    //shades and lights
    public double dot(Vector3D other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }
    //cross product for vectors
    public Vector3D cross(Vector3D other) {
        return new Vector3D(
                this.y * other.z - this.z * other.y,
                this.z * other.x - this.x * other.z,
                this.x * other.y - this.y * other.x
        );
    }
    //vector large root process
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    // Normalize-- Makes the vector measure exactly 1
    public Vector3D normalize() {
        double mag = length();
        if (mag == 0) return new Vector3D(0, 0, 0); // Evitar división por cero
        return this.multiply(1.0 / mag);
    }
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}