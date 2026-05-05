public class camera {

    public Vector3D position;

    public camera(Vector3D position) {
        this.position = position;
    }

    // Generate a ray for a pixel(x, y)
    public Ray generateRay(double x, double y) {
        Vector3D direction = new Vector3D(x, y, -1).normalize();
        return new Ray(position, direction);
    }
}