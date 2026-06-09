import java.awt.Color;


public class Directionallight extends Ligth {

    private Vector3D direction;

    // Create a directional light
    public Directionallight(Vector3D direction, Color color, double intensity) {
        super(color, intensity);
        this.direction = direction.normalize();
    }

    // Calculate the diffuse lighting factor
    @Override
    public double getNDotL(Intersection intersection) {

        Vector3D toLight = direction.negate().normalize();

        return Math.max(
                intersection.getNormal().dot(toLight),
                0.0
        );
    }

    // Return the direction from the surface to the light
    @Override
    public Vector3D getLightDirection(Intersection intersection) {
        return direction.negate().normalize();
    }

    // Directional lights are considered infinitely far away
    @Override
    public double getDistance(Intersection intersection) {
        return Double.MAX_VALUE;
    }

    public Vector3D getDirection() {
        return direction;
    }

    public void setDirection(Vector3D direction) {
        this.direction = direction.normalize();
    }
}