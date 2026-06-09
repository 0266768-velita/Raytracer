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

    public Vector3D getPosition() {
        return position;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public double getFov() {
        return fov;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

    // Generate one ray for the center of the pixel
    public Ray generateRay(int px, int py, int width, int height) {
        return generateRay(px, py, width, height, 0.5, 0.5);
    }

    // Generate a ray with a custom offset inside the pixel
    // Used for anti-aliasing
    public Ray generateRay(int px, int py, int width, int height,
                           double ox, double oy) {

        double aspectRatio = (double) width / height;

        // Convert FOV to screen scale
        double scale = Math.tan(Math.toRadians(fov * 0.5));

        // Convert pixel position to camera space
        double x = (2.0 * (px + ox) / width - 1.0)
                * aspectRatio * scale;

        double y = 1.0 - 2.0 * (py + oy) / height * scale;

        // Create the ray direction and normalize it
        Vector3D direction = new Vector3D(x, y, 1.0).normalize();

        return new Ray(position, direction);
    }
}