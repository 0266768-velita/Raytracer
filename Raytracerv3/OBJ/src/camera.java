public class camera {

    public Vector3D position;
//position in space
    public camera(Vector3D position) {
        this.position = position;
    }
//rays of light in the scene
    public Ray generateRay(double x, double y) {

        double fov = Math.tan(Math.toRadians(70));

        Vector3D direction = new Vector3D(
                x * fov,
                y * fov,
                -1
        ).normalize();

        return new Ray(position, direction);
    }
}