//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class Ray {
    public final Vector3D origin;
    public final Vector3D direction;

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Vector3D getPoint(double t) {
        return this.origin.add(this.direction.multiply(t));
    }

    public String toString() {
        String var10000 = String.valueOf(this.origin);
        return "Ray(origin=" + var10000 + ", direction=" + String.valueOf(this.direction) + ")";
    }
}
