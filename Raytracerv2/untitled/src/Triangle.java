import java.awt.Color;

public class Triangle implements Object3D {
//3 vertices of triangle
    private Vector3D v0;
    private Vector3D v1;
    private Vector3D v2;
    private Color color;
//save vertices and color
    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;
    }
 //   exception to double 0
    @Override
    public Intersection intersect(Ray ray) {
        double EPSILON = 0.000001;

        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);
//check the doth ligth in the edges
        Vector3D h = ray.direction.cross(edge2);
        double a = edge1.dot(h);

        if (Math.abs(a) < EPSILON) {
            return null;
        }
//first check if the point is out or inside of triangle
        double f = 1.0 / a;
        Vector3D s = ray.origin.subtract(v0);

        double u = f * s.dot(h);
        if (u < 0.0 || u > 1.0) {
            return null;
        }
//confirm the point in the triangle
        Vector3D q = s.cross(edge1);
        double v = f * ray.direction.dot(q);

        if (v < 0.0 || u + v > 1.0) {
            return null;
        }

        double t = f * edge2.dot(q);
//measure of distance in the triangle and point of light  for the ray (get a ray distance)
// and normalize for a good numbers in the proyect no with measure rarely or very big or very small
        if (t > EPSILON) {
            Vector3D point = ray.getPoint(t);
            Vector3D normal = edge1.cross(edge2).normalize();

            return new Intersection(t, point, normal, color);
        }

        return null;
    }
}