package com.awesome

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._

import java.nio._

import com.awesome.shaders._

object TextureType extends Enumeration {
  val GBUFFER_TEXTURE_TYPE_ALBEDO = 0
  val GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH = 1
  val GBUFFER_TEXTURE_TYPE_LIGHT_PASS = 2
  val GBUFFER_TEXTURE_TYPE_SPECULAR = 3
  val GBUFFER_NUM_TEXTURES = 4
}

class GBuffer {

  import TextureType._

  var gbufferShader:ShaderProgram = null
  var isSetup = false

  var textures = BufferUtils.createIntBuffer(GBUFFER_NUM_TEXTURES)
  var fbo = 0
  var renderBuffer = 0

  var screenWidth = 0
  var screenHeight = 0

  def setup(screenWidth:Int, screenHeight:Int) {
    this.screenWidth = screenWidth
    this.screenHeight = screenHeight

    if (!isSetup) {
      loadShaders()
      isSetup = true
    }

    setupFBO()
  }

  def loadShaders() {
    gbufferShader = new ShaderProgram("shaders/gbuffer.vert", "shaders/gbuffer.frag")
  }

  def setupFBO():Boolean = {
    // delete objects in case screen size changed
    glDeleteTextures(textures)
    glDeleteRenderbuffers(renderBuffer)
    glDeleteFramebuffers(fbo)

    // create an fbo
    fbo = glGenFramebuffers()
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)

    // create all gbuffer textures
    glGenTextures(textures)

    // albedo/diffuse (16-bit channel rgba)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_ALBEDO))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_FLOAT, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_ALBEDO, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_ALBEDO), 0)

    // normals + depth (32-bit RGBA float for accuracy)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, screenWidth, screenHeight, 0, GL_RGBA, GL_FLOAT, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH), 0)

    // lighting pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_LIGHT_PASS))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_LIGHT_PASS, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_LIGHT_PASS), 0)

    // specular pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SPECULAR))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_SPECULAR, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SPECULAR), 0)

    // create depth texture (we don't use this explicitly, but since we use depth testing when rendering + for our stencil pass, our FBO needs a depth buffer)
    // we make it a renderbuffer and not a texture as we'll never access it directly in a shader
    renderBuffer = glGenRenderbuffers()
    glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer)
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_STENCIL, screenWidth, screenHeight)
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderBuffer)

    // check status
    val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)

    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glBindRenderbuffer(GL_RENDERBUFFER, 0)
    glBindTexture(GL_TEXTURE_2D, 0)

    if (status != GL_FRAMEBUFFER_COMPLETE) {
      Console.println("GBuffer::setupFbo()" + "Could not create framebuffer")
      return false
    }

    return true
  }

  def getTexture(texType:Int):Int = {
    return textures.get(texType);
  }

  def bindForGeomPass(near:Float, far:Float) {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    glViewport(0, 0, screenWidth, screenHeight)

    val buffer = BufferUtils.createIntBuffer(3);
    buffer.put(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_ALBEDO)
    buffer.put(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH)
    buffer.put(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_SPECULAR)
    buffer.flip()

    glDrawBuffers(buffer)

    glDepthMask(true)
    glEnable(GL_DEPTH_TEST) 

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)
    glClear(GL_STENCIL_BUFFER_BIT)

    glDisable(GL_BLEND)

    gbufferShader.bind()
    gbufferShader.setUniform1f("uFarDistance", far)
  }

  def unbindForGeomPass() {
    gbufferShader.unbind()

    glDepthMask(false)

    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glDrawBuffer(GL_BACK)
  }

  def unbind() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glDrawBuffer(GL_BACK)
  }

  def bindForReading() {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
  }

  def unbindForReading() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
  }

  def bindForLightPass() {
    glDrawBuffer(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_LIGHT_PASS)

    //glStencilFunc(GL_NOTEQUAL, 0, 0xFF)
    //glDisable(GL_DEPTH_TEST)

    glEnable(GL_BLEND)
    glBlendEquation(GL_FUNC_ADD)
    glBlendFunc(GL_ONE, GL_ONE)

    glEnable(GL_CULL_FACE)
    glCullFace(GL_FRONT)
  }
}
