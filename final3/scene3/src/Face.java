public class Face {

    private int[]  vertexIndices;
    private int[]  normalIndices;
    private int[]  uvIndices;
    private String materialName;
    private String objectName;    // object name from the "o" line in the OBJ file

    public Face(int[] vertexIndices, int[] normalIndices, int[] uvIndices) {
        this(vertexIndices, normalIndices, uvIndices, null);
    }

    public Face(int[] vertexIndices, int[] normalIndices, int[] uvIndices, String materialName) {
        this.vertexIndices = vertexIndices;
        this.normalIndices = normalIndices;
        this.uvIndices     = uvIndices;
        this.materialName  = materialName;
    }

    public int[]  getVertexIndices()  { return vertexIndices; }
    public int[]  getNormalIndices()  { return normalIndices; }
    public int[]  getUvIndices()      { return uvIndices;     }
    public String getMaterialName()   { return materialName;  }
    public String getObjectName()     { return objectName;    }

    public void setVertexIndices(int[] v) { this.vertexIndices = v; }
    public void setNormalIndices(int[] n) { this.normalIndices = n; }
    public void setUvIndices(int[] u)     { this.uvIndices = u;     }
    public void setMaterialName(String m) { this.materialName = m;  }
    public void setObjectName(String o)   { this.objectName = o;    }

    public boolean hasNormals()  { return normalIndices != null; }
    public boolean hasUVs()      { return uvIndices != null;     }
    public boolean hasMaterial() { return materialName != null;  }

    public int getVertexCount() {
        return vertexIndices != null ? vertexIndices.length : 0;
    }
}