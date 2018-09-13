package com.jme3.shader;

import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;

/**
 * Shader source describes a shader object in OpenGL. Each shader source
 * is assigned a certain pipeline which it controls (described by it's type).
 */
public class ShaderSource extends NativeObject {

    ShaderType sourceType;
    String language;
    String name;
    String source;
    String defines;

    public ShaderSource(ShaderType type){
        super();
        this.sourceType = type;
        if (type == null) {
            throw new IllegalArgumentException("The shader type must be specified");
        }
    }
    
    protected ShaderSource(ShaderSource ss){
        super(ss.id);
        // No data needs to be copied.
        // (This is a destructable clone)
    }

    public ShaderSource(){
        super();
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public ShaderType getType() {
        return sourceType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Shader language cannot be null");
        }
        this.language = language;
        setUpdateNeeded();
    }

    public void setSource(String source){
        if (source == null) {
            throw new IllegalArgumentException("Shader source cannot be null");
        }
        this.source = source;
        setUpdateNeeded();
    }

    public void setDefines(String defines){
        if (defines == null) {
            throw new IllegalArgumentException("Shader defines cannot be null");
        }
        this.defines = defines;
        setUpdateNeeded();
    }

    public String getSource(){
        return source;
    }

    public String getDefines(){
        return defines;
    }
    
    @Override
    public long getUniqueId() {
        return ((long)OBJTYPE_SHADERSOURCE << 32) | ((long)id);
    }
    
    @Override
    public String toString(){
        String nameTxt = "";
        if (name != null)
            nameTxt = "name="+name+", ";
        if (defines != null)
            nameTxt += "defines, ";
        

        return getClass().getSimpleName() + "["+nameTxt+"type="
                                          + sourceType.name()+", language=" + language + "]";
    }

    public void resetObject(){
        id = -1;
        setUpdateNeeded();
    }

    public void deleteObject(Object rendererObject){
        ((Renderer)rendererObject).deleteShaderSource(ShaderSource.this);
    }

    public NativeObject createDestructableClone(){
        return new ShaderSource(ShaderSource.this);
    }
}