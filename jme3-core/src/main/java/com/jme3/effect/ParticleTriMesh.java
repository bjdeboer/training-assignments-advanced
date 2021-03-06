/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.effect;

import com.jme3.math.matrix.Matrix3f;
import com.jme3.math.utility.FastMath;
import com.jme3.math.vector.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ParticleTriMesh extends ParticleMesh {

    private int imagesX = 1;
    private int imagesY = 1;
    private boolean uniqueTexCoords = false;
//    private ParticleComparator comparator = new ParticleComparator();
    private ParticleEmitter emitter;
    private final boolean FACING_VELOCITY = emitter.isFacingVelocity(); 
    private final Vector3f FACENORMAL = emitter.getFaceNormal();

    //    private Particle[] particlesCopy;

    @Override
    public void initParticleData(ParticleEmitter emitter, int numParticles) {
        setMode(Mode.Triangles);

        this.emitter = emitter;

//        particlesCopy = new Particle[numParticles];

        // set positions
        FloatBuffer pb = BufferUtils.createVector3Buffer(numParticles * 4);
        // if the buffer is already set only update the data
        VertexBuffer buf = getBuffer(VertexBuffer.Type.Position);
        if (buf != null) {
            buf.updateData(pb);
        } else {
            VertexBuffer pvb = new VertexBuffer(VertexBuffer.Type.Position);
            pvb.setupData(Usage.Stream, 3, Format.Float, pb);
            setBuffer(pvb);
        }
        
        // set colors
        ByteBuffer cb = BufferUtils.createByteBuffer(numParticles * 4 * 4);
        buf = getBuffer(VertexBuffer.Type.Color);
        if (buf != null) {
            buf.updateData(cb);
        } else {
            VertexBuffer cvb = new VertexBuffer(VertexBuffer.Type.Color);
            cvb.setupData(Usage.Stream, 4, Format.UnsignedByte, cb);
            cvb.setNormalized(true);
            setBuffer(cvb);
        }

        // set texcoords
        FloatBuffer tb = BufferUtils.createVector2Buffer(numParticles * 4);
        uniqueTexCoords = false;
        for (int i = 0; i < numParticles; i++){
            tb.put(0f).put(1f);
            tb.put(1f).put(1f);
            tb.put(0f).put(0f);
            tb.put(1f).put(0f);
        }
        tb.flip();
        
        buf = getBuffer(VertexBuffer.Type.TexCoord);
        if (buf != null) {
            buf.updateData(tb);
        } else {
            VertexBuffer tvb = new VertexBuffer(VertexBuffer.Type.TexCoord);
            tvb.setupData(Usage.Static, 2, Format.Float, tb);
            setBuffer(tvb);
        }

        // set indices
        ShortBuffer ib = BufferUtils.createShortBuffer(numParticles * 6);
        for (int i = 0; i < numParticles; i++){
            int startIdx = (i * 4);

            // triangle 1
            ib.put((short)(startIdx + 1))
              .put((short)(startIdx + 0))
              .put((short)(startIdx + 2));

            // triangle 2
            ib.put((short)(startIdx + 1))
              .put((short)(startIdx + 2))
              .put((short)(startIdx + 3));
        }
        ib.flip();

        buf = getBuffer(VertexBuffer.Type.Index);
        if (buf != null) {
            buf.updateData(ib);
        } else {
            VertexBuffer ivb = new VertexBuffer(VertexBuffer.Type.Index);
            ivb.setupData(Usage.Static, 3, Format.UnsignedShort, ib);
            setBuffer(ivb);
        }
        
        updateCounts();
    }
    
    @Override
    public void setImagesXY(int imagesX, int imagesY) {
        this.imagesX = imagesX;
        this.imagesY = imagesY;
        if (imagesX != 1 || imagesY != 1){
            uniqueTexCoords = true;
            getBuffer(VertexBuffer.Type.TexCoord).setUsage(Usage.Stream);
        }
    }
      
    @Override
    public void updateParticleData(Particle[] particles, Camera cam, Matrix3f inverseRotation) {

    	//Created an inner class to provide the particle properties
    	//Method object extraction
    	ParticlePropertiesProvider ppp = new ParticlePropertiesProvider(this);
    	FloatBuffer positions = ppp.getPositions();
        ByteBuffer colors = ppp.getColors();
        FloatBuffer texcoords = ppp.getTextCoords();        

        Vector3f camUp   = cam.getUp();
        Vector3f camLeft = cam.getLeft();
        Vector3f camDir  = cam.getDirection();
        Vector3f up = new Vector3f();
        Vector3f left = new Vector3f();

        //Splitted up much the functionality in separate methods
        //Method extraction
        setInverseRotation(inverseRotation, camUp, camLeft, camDir);
        adjustVectors(up, left, camUp, camLeft);
        resetVertexBuffers(positions, colors, texcoords);     
        
        for (int i = 0; i < particles.length; i++){
            Particle p = particles[i];
            boolean dead = p.life == 0;
            if (dead){
            	resetPositions(positions);
                continue;
            }           
            if (FACING_VELOCITY){
            	adjustToFacingVelocity(p, left, camDir, up);
            }else if (FACENORMAL != null){
            	adjustToNotFaceNormal(p, up, FACENORMAL, left);
            }else if (p.angle != 0){
            	adjustToAngle(p, left, up, camLeft, camUp);
            }else{
            	adjustDefault(p, up, left, camUp, camLeft);
            }
            reAdjustPositions(positions, p, left, up);
            if (uniqueTexCoords){	
            	adjustTexCoords(p, texcoords);
            }
            reAdjustColors(p, colors);
        }
        positions.clear();
        colors.clear();
        texcoords.clear();
        if(uniqueTexCoords) {
        	ppp.updateAll(positions, colors, texcoords);
        }
        else {
        	ppp.updatePositions(positions);
        	ppp.updateColors(colors);
        }
    }
    
    
    private void resetVertexBuffers(FloatBuffer positions, ByteBuffer colors, FloatBuffer texcoords) {
        positions.clear();
        colors.clear();
        texcoords.clear();
    }
    
    private void adjustVectors(Vector3f up, Vector3f left, Vector3f camUp, Vector3f camLeft) {
        if (!FACING_VELOCITY){
            up.set(camUp);
            left.set(camLeft);
        }
    }
    
    private void resetPositions(FloatBuffer positions) { 	
        positions.put(0).put(0).put(0);
        positions.put(0).put(0).put(0);
        positions.put(0).put(0).put(0);
        positions.put(0).put(0).put(0);
    }
    
    private void adjustToFacingVelocity(Particle p, Vector3f left, Vector3f camDir, Vector3f up) {
        left.set(p.velocity).normalizeLocal();
        camDir.cross(left, up);
        up.multLocal(p.size);
        left.multLocal(p.size);
    }
    
    private void adjustToNotFaceNormal(Particle p, Vector3f up, Vector3f faceNormal, Vector3f left) {
        up.set(faceNormal).crossLocal(Vector3f.UNIT_X);
        faceNormal.cross(up, left);
        up.multLocal(p.size);
        left.multLocal(p.size);
        if (p.angle != 0) {
            TempVars vars = TempVars.get();
            vars.vect1.set(faceNormal).normalizeLocal();
            vars.quat1.fromAngleNormalAxis(p.angle, vars.vect1);
            vars.quat1.multLocal(left);
            vars.quat1.multLocal(up);
            vars.release();
        }   
    }
    
    private void adjustToAngle(Particle p,Vector3f left, Vector3f up, Vector3f camLeft, Vector3f camUp) {
    	float cos = FastMath.cos(p.angle) * p.size;
        float sin = FastMath.sin(p.angle) * p.size;
        
        left.x = camLeft.x * cos + camUp.x * sin;
        left.y = camLeft.y * cos + camUp.y * sin;
        left.z = camLeft.z * cos + camUp.z * sin;

        up.x = camLeft.x * -sin + camUp.x * cos;
        up.y = camLeft.y * -sin + camUp.y * cos;
        up.z = camLeft.z * -sin + camUp.z * cos;
    }
    
    private void adjustDefault(Particle p, Vector3f left, Vector3f up, Vector3f camUp, Vector3f camLeft) {
        up.set(camUp);
        left.set(camLeft);
        up.multLocal(p.size);
        left.multLocal(p.size);
    }
    
    private void reAdjustPositions(FloatBuffer positions, Particle p, Vector3f left, Vector3f up) {
        positions.put(p.position.x + left.x + up.x)
        .put(p.position.y + left.y + up.y)
        .put(p.position.z + left.z + up.z);

        positions.put(p.position.x - left.x + up.x)
        .put(p.position.y - left.y + up.y)
        .put(p.position.z - left.z + up.z);

        positions.put(p.position.x + left.x - up.x)
        .put(p.position.y + left.y - up.y)
        .put(p.position.z + left.z - up.z);

        positions.put(p.position.x - left.x - up.x)
        .put(p.position.y - left.y - up.y)
        .put(p.position.z - left.z - up.z);
    }
    
    private void adjustTexCoords(Particle p, FloatBuffer texcoords) {
        int imgX = p.imageIndex % imagesX;
        int imgY = (p.imageIndex - imgX) / imagesY;

        float startX = ((float) imgX) / imagesX;
        float startY = ((float) imgY) / imagesY;
        float endX   = startX + (1f / imagesX);
        float endY   = startY + (1f / imagesY);

        texcoords.put(startX).put(endY);
        texcoords.put(endX).put(endY);
        texcoords.put(startX).put(startY);
        texcoords.put(endX).put(startY);
    }
    
    private void reAdjustColors(Particle p, ByteBuffer colors) {
    int abgr = p.color.asIntABGR();
    colors.putInt(abgr);
    colors.putInt(abgr);
    colors.putInt(abgr);
    colors.putInt(abgr);
    }
    
    private void setInverseRotation(Matrix3f inverseRotation, Vector3f camUp, Vector3f camLeft, Vector3f camDir) {
        inverseRotation.multLocal(camUp);
        inverseRotation.multLocal(camLeft);
        inverseRotation.multLocal(camDir);
    }
    
}
