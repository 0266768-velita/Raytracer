import java.awt.Color;

// Triangle with FLAT SHADING: uses face normal (same normal for every pixel in the triangle)
public class Triangle implements Object3D {

    private Vector3D v0;
    private Vector3D v1;
    private Vector3D v2;
    private Color color;

    // Pre-computed face normal (flat shading: one normal per triangle, not per vertex)
    private final Vector3D faceNormal;

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;

        // Compute once at construction time — flat shading uses this for every intersection
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
        this.faceNormal = edge1.cross(edge2).normalize();
    }

    // Returns the pre-computed flat (face) normal
    public Vector3D getFaceNormal() {
        return faceNormal;
    }

    public Vector3D[] getVertices() {
        return new Vector3D[]{v0, v1, v2};
    }

    @Override
    public Intersection intersect(Ray ray) {
        final double EPSILON = 0.0000001;

        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D h = ray.direction.cross(edge2);
        double a = edge1.dot(h);

        if (a > -EPSILON && a < EPSILON) {
            return null; // Ray is parallel to triangle
        }

        double f = 1.0 / a;
        Vector3D s = ray.origin.subtract(v0);
        double u = f * s.dot(h);

        if (u < 0.0 || u > 1.0) {
            return null;
        }

        Vector3D q = s.cross(edge1);
        double v = f * ray.direction.dot(q);

        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        double t = f * edge2.dot(q);

        if (t > EPSILON) {
            Vector3D point = ray.getPoint(t);

            // FLAT SHADING: use faceNormal for ALL pixels of this triangle
            // If the ray hits the back face, flip the normal so it always faces the camera
            Vector3D normal = faceNormal;
            if (normal.dot(ray.direction) > 0) {
                normal = normal.negate();
            }

            return new Intersection(t, point, normal, color);
        }

        return null;
    }
}