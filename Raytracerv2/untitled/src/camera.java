//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class camera {
    public Vector3D position;

    public camera(Vector3D position) {
        this.position = position;
    }
//conect with a ray direction
    public Ray generateRay(double x, double y) {
        Vector3D direction = (new Vector3D(x, y, (double)-1.0F)).normalize();
        return new Ray(this.position, direction);
    }
}