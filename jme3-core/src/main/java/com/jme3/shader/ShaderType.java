package com.jme3.shader;

/**
 * Type of shader. The shader will control the pipeline of it's type.
 */
public enum ShaderType {

    /**
     * Control fragment rasterization. (e.g color of pixel).
     */
    Fragment("frag"),
    /**
     * Control vertex processing. (e.g transform of model to clip space)
     */
    Vertex("vert"),
    /**
     * Control geometry assembly. (e.g compile a triangle list from input
     * data)
     */
    Geometry("geom"),
    /**
     * Controls tesselation factor (e.g how often a input patch should be
     * subdivided)
     */
    TessellationControl("tsctrl"),
    /**
     * Controls tesselation transform (e.g similar to the vertex shader, but
     * required to mix inputs manual)
     */
    TessellationEvaluation("tseval");

    private String extension;
    
    public String getExtension() {
        return extension;
    }
    
    private ShaderType(String extension) {
        this.extension = extension;
    }
}
