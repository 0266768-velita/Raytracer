/**
 * Stores information about a ray-object intersection.
 */
public class Intersection {

    private double t;

    private Vector3D point;
    private Vector3D normal;

    private Material material;

    private double u;
    private double v;

    // Object hit by the ray
    private Object3D object;

    public Intersection(double t,
                        Vector3D point,
                        Vector3D normal,
                        Material material) {

        this(t, point, normal, material, 0.0, 0.0);
    }

    public Intersection(double t,
                        Vector3D point,
                        Vector3D normal,
                        Material material,
                        double u,
                        double v) {

        this.t = t;
        this.point = point;
        this.normal = normal.normalize();

        this.material = material;

        this.u = u;
        this.v = v;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public Vector3D getPoint() {
        return point;
    }

    public void setPoint(Vector3D point) {
        this.point = point;
    }

    public Vector3D getNormal() {
        return normal;
    }

    public void setNormal(Vector3D normal) {
        this.normal = normal.normalize();
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public double getU() {
        return u;
    }

    public void setU(double u) {
        this.u = u;
    }

    public double getV() {
        return v;
    }

    public void setV(double v) {
        this.v = v;
    }

    // Texture coordinates alias
    public double getTexU() {
        return u;
    }

    public double getTexV() {
        return v;
    }

    // Return the object hit by the ray
    public Object3D getObject() {
        return object;
    }

    public void setObject(Object3D object) {
        this.object = object;
    }

    // Return the material color
    public java.awt.Color getColor() {

        if (material == null) {
            return java.awt.Color.MAGENTA;
        }

        return material.getColor();
    }
}