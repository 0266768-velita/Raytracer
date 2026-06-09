//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.awt.Color;

public class Intersection {
    public double t;
    public Vector3D point;
    public Vector3D normal;
    public Color color;
//bounce of lights in scene (the color in pixels )
    public Intersection(double t, Vector3D point, Vector3D normal, Color color) {
        this.t = t;
        this.point = point;
        this.normal = normal;
        this.color = color;
    }
}
