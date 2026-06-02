public class camera {

    public Vector3D position;
//Camera position in 3D space
    public camera(Vector3D position) {
        this.position = position;
    }
//It generates a ray that passes through the center of the pixel
    public Ray generateRay(int px, int py, int width, int height) {

        double x = (2.0 * (px + 0.5) / width - 1.0);
        double y = (1.0 - 2.0 * (py + 0.5) / height);

        //
        double fov = Math.tan(Math.toRadians(70.0 / 2.0));

        Vector3D direction = new Vector3D(
                x * fov,
                y * fov,
                1.0
        ).normalize();

        return new Ray(position, direction);
    }
}