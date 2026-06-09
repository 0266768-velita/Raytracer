import java.awt.Color;



 //t:      distance along the ray to the hit point
 //point:  world-space position of the hit
 // normal: surface normal at the hit point
 //        For triangles (flat shading)  face normal (same for whole triangle)
 //      For spheres - point normal (varies per pixel)
 // color:  base color of the intersected object

public class Intersection {
    public double t;
    public Vector3D point;
    public Vector3D normal;
    public Color color;

    public Intersection(double t, Vector3D point, Vector3D normal, Color color) {
        this.t = t;
        this.point = point;
        this.normal = normal;
        this.color = color;
    }
}