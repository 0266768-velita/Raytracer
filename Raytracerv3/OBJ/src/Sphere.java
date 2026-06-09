//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.awt.Color;
//create a sphere with sizes
public class Sphere implements Object3D {
    public Vector3D center;
    public double radius;
    public Color color;
    //measure for sphere basic things
    public Sphere(Vector3D center, double radius, Color color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    public Intersection intersect(Ray ray) {
        Vector3D oc = ray.origin.subtract(this.center);
        double a = ray.direction.dot(ray.direction);
        double b = (double)2.0F * oc.dot(ray.direction);
        double c = oc.dot(oc) - this.radius * this.radius;
        double discriminant = b * b - (double)4.0F * a * c;
        if (discriminant < (double)0.0F) {
            return null;
        } else {
            double t = (-b - Math.sqrt(discriminant)) / ((double)2.0F * a);
            if (t < (double)0.0F) {
                return null;
            } else {
                Vector3D point = ray.getPoint(t);
                Vector3D normal = point.subtract(this.center).normalize();
                return new Intersection(t, point, normal, this.color);
            }
        }
    }
}
