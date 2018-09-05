package com.jme3.effect;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

public class ParticlePropertiesProvider {
	
	private VertexBuffer positionBuffer;
	private VertexBuffer colorBuffer;
	private VertexBuffer texCoordBuffer;
	
	public ParticlePropertiesProvider(Mesh mesh) {
		this.positionBuffer = mesh.getBuffer(VertexBuffer.Type.Position);
		this.colorBuffer = mesh.getBuffer(VertexBuffer.Type.Color);
		this.texCoordBuffer = mesh.getBuffer(VertexBuffer.Type.TexCoord);
	}

	public FloatBuffer getPositions() {		
        FloatBuffer positions = (FloatBuffer) positionBuffer.getData();
        return positions;
	}
	
	public ByteBuffer getColors() {
		ByteBuffer colors = (ByteBuffer) colorBuffer.getData();
        return colors;
	}
	
    public FloatBuffer getTextCoords(){    	
        FloatBuffer texcoords = (FloatBuffer) texCoordBuffer.getData();
		return texcoords;
    }
    
    public void updateAll(FloatBuffer positions, ByteBuffer colors, FloatBuffer texCoords) {
        positionBuffer.updateData(positions);
        colorBuffer.updateData(colors);
        texCoordBuffer.updateData(texCoords);
    }
    
    public void updatePositions(FloatBuffer positions) {
    	positionBuffer.updateData(positions);
    }
    
    public void updateColors(ByteBuffer colors) {
        colorBuffer.updateData(colors);
    }

}

