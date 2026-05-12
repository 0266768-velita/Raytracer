//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class Vector3D {
    public final double x;
    public final double y;
    public final double z;
//3 measures for vectors in all objects
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3D negate() {
        return new Vector3D(-this.x, -this.y, -this.z);
    }

    public Vector3D subtract(Vector3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    public Vector3D multiply(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3D multiply(Vector3D other) {
        return new Vector3D(this.x * other.x, this.y * other.y, this.z * other.z);
    }

    public Vector3D add(Vector3D other) {
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public double dot(Vector3D other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vector3D cross(Vector3D other) {
        return new Vector3D(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public Vector3D normalize() {
        double mag = this.length();
        return mag == (double)0.0F ? new Vector3D((double)0.0F, (double)0.0F, (double)0.0F) : this.multiply((double)1.0F / mag);
    }

    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", this.x, this.y, this.z);
    }
}
