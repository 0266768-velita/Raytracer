import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ObjLoader {

    private List<Vector3D> vertices = new ArrayList<>();
    private List<Vector3D> normals = new ArrayList<>();
    private List<Vector2D> textureCoordinates = new ArrayList<>();
    private List<Face> faces = new ArrayList<>();

    public List<Vector3D> getVertices() {
        return vertices;
    }

    public List<Vector3D> getNormals() {
        return normals;
    }

    public List<Vector2D> getTextureCoordinates() {
        return textureCoordinates;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public void load(String filePath) throws IOException {

        vertices.clear();
        normals.clear();
        textureCoordinates.clear();
        faces.clear();

        // Current material used by the faces
        String currentMaterial = null;

        BufferedReader reader =
                new BufferedReader(new FileReader(filePath));

        String line;

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");

            switch (parts[0]) {

                case "v":

                    vertices.add(
                            new Vector3D(
                                    Double.parseDouble(parts[1]),
                                    Double.parseDouble(parts[2]),
                                    Double.parseDouble(parts[3])
                            )
                    );

                    break;

                case "vn":

                    normals.add(
                            new Vector3D(
                                    Double.parseDouble(parts[1]),
                                    Double.parseDouble(parts[2]),
                                    Double.parseDouble(parts[3])
                            ).normalize()
                    );

                    break;

                case "vt":

                    textureCoordinates.add(
                            new Vector2D(
                                    Double.parseDouble(parts[1]),
                                    Double.parseDouble(parts[2])
                            )
                    );

                    break;

                case "usemtl":

                    // Update the active material
                    if (parts.length >= 2) {
                        currentMaterial = parts[1];
                    }

                    break;

                case "f":

                    parseFace(parts, currentMaterial);

                    break;
            }
        }

        reader.close();

        System.out.println("OBJ loaded successfully");
        System.out.println("Vertices  : " + vertices.size());
        System.out.println("Normals   : " + normals.size());
        System.out.println("UVs       : " + textureCoordinates.size());
        System.out.println("Faces     : " + faces.size());
    }

    // Read face data from an OBJ face line
    private void parseFace(String[] parts, String materialName) {

        int vertexCount = parts.length - 1;

        int[] vertexIndices = new int[vertexCount];
        int[] uvIndices = new int[vertexCount];
        int[] normalIndices = new int[vertexCount];

        for (int i = 1; i < parts.length; i++) {

            String[] data = parts[i].split("/");

            vertexIndices[i - 1] = Integer.parseInt(data[0]) - 1;

            uvIndices[i - 1] = -1;
            normalIndices[i - 1] = -1;

            if (data.length > 1 && !data[1].isEmpty()) {
                uvIndices[i - 1] =
                        Integer.parseInt(data[1]) - 1;
            }

            if (data.length > 2 && !data[2].isEmpty()) {
                normalIndices[i - 1] =
                        Integer.parseInt(data[2]) - 1;
            }
        }

        faces.add(
                new Face(
                        vertexIndices,
                        normalIndices,
                        uvIndices,
                        materialName
                )
        );
    }
}