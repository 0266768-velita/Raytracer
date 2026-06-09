
public class Face {

    private int[] vertexIndices;
    private int[] normalIndices;
    private int[] uvIndices;

    private String materialName;
    private String objectName; // Name of the object from the OBJ file

    public Face(int[] vertexIndices, int[] normalIndices, int[] uvIndices) {
        this(vertexIndices, normalIndices, uvIndices, null);
    }

    public Face(int[] vertexIndices,
                int[] normalIndices,
                int[] uvIndices,
                String materialName) {

        this.vertexIndices = vertexIndices;
        this.normalIndices = normalIndices;
        this.uvIndices = uvIndices;
        this.materialName = materialName;
    }

    public int[] getVertexIndices() {
        return vertexIndices;
    }

    public int[] getNormalIndices() {
        return normalIndices;
    }

    public int[] getUvIndices() {
        return uvIndices;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setVertexIndices(int[] vertexIndices) {
        this.vertexIndices = vertexIndices;
    }

    public void setNormalIndices(int[] normalIndices) {
        this.normalIndices = normalIndices;
    }

    public void setUvIndices(int[] uvIndices) {
        this.uvIndices = uvIndices;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    // Check if the face has normal data
    public boolean hasNormals() {
        return normalIndices != null;
    }

    // Check if the face has texture coordinates
    public boolean hasUVs() {
        return uvIndices != null;
    }

    // Check if the face uses a material
    public boolean hasMaterial() {
        return materialName != null;
    }

    // Return the number of vertices in the face
    public int getVertexCount() {

        if (vertexIndices == null) {
            return 0;
        }

        return vertexIndices.length;
    }
}