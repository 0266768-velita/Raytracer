import java.awt.Color;
//class for  triangle in the scene
public class Triangle implements Object3D {

    private Vector3D v0;
    private Vector3D v1;
    private Vector3D v2;

    private Color color;

    public Triangle(
            Vector3D v0,
            Vector3D v1,
            Vector3D v2,
            Color color
    ) {

        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;

        this.color = color;
    }

    @Override
    public Intersection intersect(Ray ray) {

        double EPSILON = 0.0000001;

        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D h = ray.direction.cross(edge2);

        double a = edge1.dot(h);

        if (a > -EPSILON && a < EPSILON) {
            return null;
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

            Vector3D normal =
                    edge1.cross(edge2).normalize();

            return new Intersection(
                    t,
                    point,
                    normal,
                    color
            );
        }

        return null;
    }
}