public class Camera {

    private Vector3D position;
    private double fov;

    public Camera(Vector3D position) {
        this(position, 70.0);
    }

    public Camera(Vector3D position, double fov) {
        this.position = position;
        this.fov = fov;
    }

    public Vector3D getPosition() { return position; }
    public void setPosition(Vector3D position) { this.position = position; }
    public double getFov() { return fov; }
    public void setFov(double fov) { this.fov = fov; }

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

        Vector3D direction = new Vector3D(x, y, 1.0).normalize();
        return new Ray(position, direction);
    }
}