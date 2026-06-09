public class Intersection {

    private double t;
    private Vector3D point;
    private Vector3D normal;
    private Material material;
    private double u;
    private double v;
    private Object3D object; // reference to the object that was hit

    public Intersection(double t, Vector3D point, Vector3D normal, Material material) {
        this(t, point, normal, material, 0.0, 0.0);
    }

    public Intersection(double t, Vector3D point, Vector3D normal, Material material, double u, double v) {
        this.t = t;
        this.point = point;
        this.normal = normal.normalize();
        this.material = material;
        this.u = u;
        this.v = v;
    }

    public double getT()           { return t; }
    public void   setT(double t)   { this.t = t; }

    public Vector3D getPoint()                { return point; }
    public void     setPoint(Vector3D point)  { this.point = point; }

    public Vector3D getNormal()               { return normal; }
    public void     setNormal(Vector3D n)     { this.normal = n.normalize(); }

    public Material getMaterial()             { return material; }
    public void     setMaterial(Material m)   { this.material = m; }

    public double getU()           { return u; }
    public void   setU(double u)   { this.u = u; }
    public double getV()           { return v; }
    public void   setV(double v)   { this.v = v; }

    // aliases kept for compatibility with the shader
    public double getTexU()        { return u; }
    public double getTexV()        { return v; }

    // the object that was hit — used to access Triangle textures
    public Object3D getObject()            { return object; }
    public void     setObject(Object3D o)  { this.object = o; }

    public java.awt.Color getColor() {
        if (material == null) return java.awt.Color.MAGENTA;
        return material.getColor();
    }
}