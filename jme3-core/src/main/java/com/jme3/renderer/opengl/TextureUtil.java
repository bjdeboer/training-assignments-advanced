/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.RendererException;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal utility class used by {@link GLRenderer} to manage textures.
 * 
 * @author Kirill Vainer
 */
final class TextureUtil {

    private static final Logger logger = Logger.getLogger(TextureUtil.class.getName());

    private final GL gl;
    private final GL2 gl2;
    private final GLExt glext;
    private GLImageFormat[][] formats;
    
    public TextureUtil(GL gl, GL2 gl2, GLExt glext) {
        this.gl = gl;
        this.gl2 = gl2;
        this.glext = glext;
    }
    
    public void initialize(EnumSet<Caps> caps) {
        this.formats = GLImageFormats.getFormatsForCaps(caps);
        if (logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Supported texture formats: \n");
            for (int i = 0; i < Format.values().length; i++) {
                Format format = Format.values()[i];
                if (formats[0][i] != null) {
                    boolean srgb = formats[1][i] != null;
                    sb.append("\t").append(format.toString());
                    sb.append(" (Linear");
                    if (srgb) sb.append("/sRGB");
                    sb.append(")\n");
                }
            }
            logger.log(Level.FINE, sb.toString());
        }
    }

    public GLImageFormat getImageFormat(Format fmt, boolean isSrgb) {
        if (isSrgb) {
            return formats[1][fmt.ordinal()];
        } else {
            return formats[0][fmt.ordinal()];
        }
    }

    public GLImageFormat getImageFormatWithError(Format fmt, boolean isSrgb) {
        //if the passed format is one kind of depth there isno point in getting the srgb format;
        isSrgb = isSrgb && fmt != Format.Depth && fmt != Format.Depth16 && fmt != Format.Depth24 && fmt != Format.Depth24Stencil8 && fmt != Format.Depth32 && fmt != Format.Depth32F;
        GLImageFormat glFmt = getImageFormat(fmt, isSrgb);
        if (glFmt == null && isSrgb) {
            glFmt = getImageFormat(fmt, false);               
            logger.log(Level.WARNING, "No sRGB format available for ''{0}''. Failling back to linear.", fmt);
        }
        if (glFmt == null) { 
            throw new RendererException("Image format '" + fmt + "' is unsupported by the video hardware.");
        }
        return glFmt;
    }
    
    private void setupTextureSwizzle(int target, Format format) {
        // Needed for OpenGL 3.3 to support luminance / alpha formats
        switch (format) {
            case Alpha8:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_ZERO);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_RED);
                break;
            case Luminance8:
            case Luminance16F:
            case Luminance32F:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_ONE);
                break;
            case Luminance8Alpha8:
            case Luminance16FAlpha16F:
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_R, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_G, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_B, GL.GL_RED);
                gl.glTexParameteri(target, GL3.GL_TEXTURE_SWIZZLE_A, GL.GL_GREEN);
                break;
        }
    }
    
    
    
    class TextureImage{
    	
    	int level;
    	int width;
    	int height;
    	int depth;
    	ByteBuffer data;
    	
    	public TextureImage(int level, int width, int height, int depth) {
    		this(level, width, height, depth, null);
    	}
    	
    	public TextureImage(int level, int width, int height, int depth, ByteBuffer data) {
    		this.level = level;
    		this.width = width;
    		this.height = height;
    		this.depth = depth;
    		this.data = data;
    	}
    }
    
    class Slicer {
    	
    	int slice;
    	int sliceCount;
    	
    	public Slicer(int slice, int sliceCount) {
    		this.slice = slice;
    		this.sliceCount = sliceCount;
    	}
    }

    private void uploadTextureLevel(GLImageFormat format, int target, TextureImage tImage, Slicer slicer, 
    		int samples) {
        if (format.compressed && tImage.data != null) {
            if (target == GL2.GL_TEXTURE_3D) {
                // For 3D textures, we upload the entire mipmap level.
                gl2.glCompressedTexImage3D(target,
                                           tImage.level,
                                           format.internalFormat,
                                           tImage.width,
                                           tImage.height,
                                           tImage.depth,
                                           0,
                                           tImage.data);
            } else if (target == GLExt.GL_TEXTURE_2D_ARRAY_EXT) {
                // For texture arrays, only upload 1 slice at a time.
                // zoffset specifies slice index, and depth is 1 to indicate
                // a single texture in the array.
                gl2.glCompressedTexSubImage3D(target,
                							  tImage.level,
                							  0,
                                              0,
                                              slicer.slice,
                                              tImage.width,
                                              tImage.height,
                                              1,
                                              format.internalFormat,
                                              tImage.data);
            } else {
                // Cubemaps also use 2D upload.
                gl2.glCompressedTexImage2D(target,
                						   tImage.level,
                                           format.internalFormat,
                                           tImage.width,
                                           tImage.height,
                                           0,
                                           tImage.data);
            }
        } else {
            // (Non-compressed OR allocating texture storage for FBO)
            if (target == GL2.GL_TEXTURE_3D) {
                gl2.glTexImage3D(target,
                			     tImage.level,
                                 format.internalFormat,
                                 tImage.width,
                                 tImage.height,
                                 tImage.depth,
                                 0,
                                 format.format,
                                 format.dataType,
                                 tImage.data);
            } else if (target == GLExt.GL_TEXTURE_2D_ARRAY_EXT) {
                if (slicer.slice == -1) {
                    // Allocate texture storage (data is NULL)
                    gl2.glTexImage3D(target,
                    				 tImage.level,
                                     format.internalFormat,
                                     tImage.width,
                                     tImage.height,
                                     slicer.sliceCount, //# of slices
                                     0,
                                     format.format,
                                     format.dataType,
                                     tImage.data);
                } else {
                    // For texture arrays, only upload 1 slice at a time.
                    // zoffset specifies slice index, and depth is 1 to indicate
                    // a single texture in the array.
                    gl2.glTexSubImage3D(target,
                                        tImage.level,          // level
                                        0,              // xoffset
                                        0,              // yoffset
                                        slicer.slice,          // zoffset
                                        tImage.width,          // width
                                        tImage.height,         // height
                                        1,              // depth
                                        format.format,
                                        format.dataType,
                                        tImage.data);
                }
            } else {
                // 2D multisampled image.
                if (samples > 1) {
                    glext.glTexImage2DMultisample(target,
                                                  samples,
                                                  format.internalFormat,
                                                  tImage.width,
                                                  tImage.height,
                                                  true);
                } else {
                    // Regular 2D image
                    gl.glTexImage2D(target,
                                    tImage.level,
                                    format.internalFormat,
                                    tImage.width,
                                    tImage.height,
                                    0,
                                    format.format,
                                    format.dataType,
                                    tImage.data);
                }
            }
        }
    }

    public void uploadTexture(Image image,
                              int target,
                              int index,
                              boolean linearizeSrgb) {

        boolean getSrgbFormat = image.getColorSpace() == ColorSpace.sRGB && linearizeSrgb;
        Image.Format jmeFormat = image.getFormat();
        GLImageFormat oglFormat = getImageFormatWithError(jmeFormat, getSrgbFormat);

        ByteBuffer data = null;
        int sliceCount = 1;
        
        if (index >= 0) {
            data = image.getData(index);
        }
        
        if (image.getData() != null && image.getData().size() > 0) {
            sliceCount = image.getData().size();
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth = image.getDepth();
        
        int[] mipSizes = image.getMipMapSizes();
        int pos = 0;
        // TODO: Remove unneccessary allocation
        if (mipSizes == null) {
            if (data != null) {
                mipSizes = new int[]{data.capacity()};
            } else {
                mipSizes = new int[]{width * height * jmeFormat.getBitsPerPixel() / 8};
            }
        }

        int samples = image.getMultiSamples();
        
        // For OGL3 core: setup texture swizzle.
        if (oglFormat.swizzleRequired) {
            setupTextureSwizzle(target, jmeFormat);
        }

        for (int i = 0; i < mipSizes.length; i++) {
            int mipWidth = Math.max(1, width >> i);
            int mipHeight = Math.max(1, height >> i);
            int mipDepth = Math.max(1, depth >> i);

            if (data != null) {
                data.position(pos);
                data.limit(pos + mipSizes[i]);
            }

            TextureImage tImage = new TextureImage(i, mipWidth, mipHeight, mipDepth, data);
            Slicer slicer = new Slicer(index, sliceCount);
            uploadTextureLevel(oglFormat, target, tImage, slicer, samples);

            pos += mipSizes[i];
        }
    }

    public void uploadSubTexture(Image image, int target, int index, int x, int y, boolean linearizeSrgb) {
        if (target != GL.GL_TEXTURE_2D || image.getDepth() > 1) {
            throw new UnsupportedOperationException("Updating non-2D texture is not supported");
        }
        
        if (image.getMipMapSizes() != null) {
            throw new UnsupportedOperationException("Updating mip-mappped images is not supported");
        }
        
        if (image.getMultiSamples() > 1) {
            throw new UnsupportedOperationException("Updating multisampled images is not supported");
        }
        
        Image.Format jmeFormat = image.getFormat();
        
        if (jmeFormat.isCompressed()) {
            throw new UnsupportedOperationException("Updating compressed images is not supported");
        } else if (jmeFormat.isDepthFormat()) {
            throw new UnsupportedOperationException("Updating depth images is not supported");
        }
        
        boolean getSrgbFormat = image.getColorSpace() == ColorSpace.sRGB && linearizeSrgb;
        GLImageFormat oglFormat = getImageFormatWithError(jmeFormat, getSrgbFormat);
        
        ByteBuffer data = null;
        
        if (index >= 0) {
            data = image.getData(index);
        }
        
        if (data == null) {
            throw new IndexOutOfBoundsException("The image index " + index + " is not valid for the given image");
        }

        data.position(0);
        data.limit(data.capacity());
        
        gl.glTexSubImage2D(target, 0, x, y, image.getWidth(), image.getHeight(), 
                           oglFormat.format, oglFormat.dataType, data);
    }
}
