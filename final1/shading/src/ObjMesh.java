import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


public class ObjMesh {

    private List<Triangle> triangles;

    // Create the mesh using a material
    public ObjMesh(
            ObjLoader loader,
            Material material
    ) {

        triangles = new ArrayList<>();

        List<Vector3D> vertices =
                loader.getVertices();

        List<Face> faces =
                loader.getFaces();

        for (Face face : faces) {

            int[] vertexIndices =
                    face.getVertexIndices();

            // Skip invalid faces
            if (vertexIndices.length < 3) {
                continue;
            }

            // Convert polygons into triangles
            for (int i = 1;
                 i < vertexIndices.length - 1;
                 i++) {

                Vector3D v0 =
                        vertices.get(
                                vertexIndices[0]
                        );

                Vector3D v1 =
                        vertices.get(
                                vertexIndices[i]
                        );

                Vector3D v2 =
                        vertices.get(
                                vertexIndices[i + 1]
                        );

                triangles.add(
                        new Triangle(
                                v0,
                                v1,
                                v2,
                                material
                        )
                );
            }
        }
    }

    // Constructor that receives a color
    public ObjMesh(
            ObjLoader loader,
            Color color
    ) {

        this(
                loader,
                Material.diffuse(color)
        );
    }

    public Triangle[] getTriangles() {

        return triangles.toArray(
                new Triangle[0]
        );
    }

    public List<Triangle> getTriangleList() {
        return triangles;
    }

    public int getTriangleCount() {
        return triangles.size();
    }
}