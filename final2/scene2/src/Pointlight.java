import java.awt.Color;

// point light source — attenuates with distance
public class Pointlight extends Ligth {

    private Vector3D position;

    public Pointlight(Vector3D position, Color color, double intensity) {
        super(color, intensity);
        this.position = position;
    }

    // Lambert diffuse factor with quadratic distance attenuation
    @Override
    public double getNDotL(Intersection intersection) {
        Vector3D toLight = position.subtract(intersection.getPoint());
        double distance  = toLight.length();
        toLight          = toLight.normalize();

        double nDotL       = Math.max(intersection.getNormal().dot(toLight), 0.0);
        double attenuation = 1.0 / (1.0 + 0.05 * distance * distance);

        return nDotL * attenuation;
    }

    // normalized vector from the surface toward the light
    @Override
    public Vector3D getLightDirection(Intersection intersection) {
        return position.subtract(intersection.getPoint()).normalize();
    }

    // actual distance from the hit point to the light
    @Override
    public double getDistance(Intersection intersection) {
        return position.subtract(intersection.getPoint()).length();
    }

    public Vector3D getPosition()               { return position; }
    public void     setPosition(Vector3D position) { this.position = position; }
}