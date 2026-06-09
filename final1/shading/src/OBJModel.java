import java.util.ArrayList;
import java.util.List;

/**
 * Stores the data loaded from an OBJ file.
 */
public class OBJModel {

    private List<Vector3D> vertices;

    private List<Vector3D> normals;

    private List<Vector2D> uvs;

    private List<Face> faces;

    public OBJModel() {

        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        uvs = new ArrayList<>();
        faces = new ArrayList<>();
    }

    public List<Vector3D> getVertices() {
        return vertices;
    }

    public void setVertices(List<Vector3D> vertices) {
        this.vertices = vertices;
    }

    public List<Vector3D> getNormals() {
        return normals;
    }

    public void setNormals(List<Vector3D> normals) {
        this.normals = normals;
    }

    public List<Vector2D> getUvs() {
        return uvs;
    }

    public void setUvs(List<Vector2D> uvs) {
        this.uvs = uvs;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public void setFaces(List<Face> faces) {
        this.faces = faces;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getNormalCount() {
        return normals.size();
    }

    public int getUVCount() {
        return uvs.size();
    }

    public int getFaceCount() {
        return faces.size();
    }

    // Print model information
    public void printSummary() {

        System.out.println("========== OBJ MODEL ==========");
        System.out.println("Vertices : " + vertices.size());
        System.out.println("Normals  : " + normals.size());
        System.out.println("UVs      : " + uvs.size());
        System.out.println("Faces    : " + faces.size());
        System.out.println("===============================");
    }
}