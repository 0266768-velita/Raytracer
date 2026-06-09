public class Camera {

    private Vector3D position;
    private double fov;
    private Vector3D forward, right, up;

    public Camera(Vector3D position) {
        this(position, 70.0);
    }

    public Camera(Vector3D position, double fov) {
        this.position = position;
        this.fov = fov;
        // default look direction is +Z
        buildAxes(new Vector3D(0, 0, 1));
    }

    public void setLookAt(Vector3D target) {
        double dx = target.x - position.x;
        double dy = target.y - position.y;
        double dz = target.z - position.z;
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        buildAxes(new Vector3D(dx/len, dy/len, dz/len));
    }

    private void buildAxes(Vector3D fwd) {
        this.forward = fwd;
        Vector3D worldUp = new Vector3D(0, 1, 0);
        // if forward is nearly parallel to worldUp, fall back to Z as up
        double dot = Math.abs(fwd.y);
        if (dot > 0.99) worldUp = new Vector3D(0, 0, 1);
        this.right = forward.cross(worldUp).normalize();
        this.up    = right.cross(forward).normalize();
    }

    public Vector3D getPosition()        { return position; }
    public void     setPosition(Vector3D p) { this.position = p; }
    public double   getFov()             { return fov; }
    public void     setFov(double f)     { this.fov = f; }

    // one ray per pixel, centered
    public Ray generateRay(int px, int py, int width, int height) {
        return generateRay(px, py, width, height, 0.5, 0.5);
    }

    // ray with sub-pixel offset for anti-aliasing (ox, oy in [0,1] within the pixel)
    public Ray generateRay(int px, int py, int width, int height, double ox, double oy) {
        double aspectRatio = (double) width / height;
        double scale = Math.tan(Math.toRadians(fov * 0.5));

        double x = (2.0 * (px + ox) / width  - 1.0) * aspectRatio * scale;
        double y =  1.0 - 2.0 * (py + oy) / height  * scale;

        Vector3D direction = new Vector3D(
                forward.x + x * right.x + y * up.x,
                forward.y + x * right.y + y * up.y,
                forward.z + x * right.z + y * up.z
        ).normalize();

        return new Ray(position, direction);
    }
}