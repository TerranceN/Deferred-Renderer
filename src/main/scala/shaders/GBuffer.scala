package com.awesome.shaders

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL14._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._

import scala.util.Random
import java.nio._
import scala.collection.mutable.ArrayBuffer

import com.awesome.GLFrustum
import com.awesome.vectors._

object TextureType extends Enumeration {
  val GBUFFER_TEXTURE_TYPE_ALBEDO = 0
  val GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH = 1
  val GBUFFER_TEXTURE_TYPE_LIGHT_PASS = 2
  val GBUFFER_TEXTURE_TYPE_SPECULAR = 3
  val GBUFFER_TEXTURE_TYPE_SSAO = 4
  val GBUFFER_TEXTURE_TYPE_SSAO_NOISE = 5
  val GBUFFER_NUM_TEXTURES = 6
}

class GBuffer {
  import TextureType._

  var gbufferShader:ShaderProgram = null
  var lightingShader:ShaderProgram = null
  var ssaoShader:ShaderProgram = null
  var isSetup = false

  val ssaoNoiseSize = 4
  val ssaoRadius = 1f
  val numKernPoints = 20

  val random = new Random()
  val kernel:ArrayBuffer[Vector3] = new ArrayBuffer(numKernPoints)

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
    lightingShader = new ShaderProgram("shaders/lighting.vert", "shaders/lighting.frag")
    ssaoShader = new ShaderProgram("shaders/ssao.vert", "shaders/ssao.frag")
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

    // specular pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SPECULAR))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_SPECULAR, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SPECULAR), 0)

    // ssao pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SSAO))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_SSAO, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SSAO), 0)

    // lighting pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_LIGHT_PASS))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null:FloatBuffer)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_LIGHT_PASS, GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_LIGHT_PASS), 0)

    generateSSAONoise(ssaoNoiseSize)

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

  def randRange(min:Float, max:Float):Float = {
    return min + random.nextFloat() * (max - min)
  }

  def generateSSAONoise(noiseSize:Int) {
    val buffer = BufferUtils.createFloatBuffer(noiseSize*noiseSize*4)
    for (i <- 0 until (noiseSize*noiseSize)) {
      val v = (new Vector3(randRange(-1, 1), randRange(-1, 1), 0)).normalized
      Console.println("noise vec: " + v.x + ", " + v.y + ", " + v.z)
      buffer.put(v.x)
      buffer.put(v.y)
      buffer.put(v.z)
    }

    buffer.flip()

    // ssao pass (16-bit RGBA)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SSAO_NOISE))
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, noiseSize, noiseSize, 0, GL_RGB, GL_FLOAT, buffer)

    // initialize kernel with random points within a circle around (0, 0)
    for (i <- 0 until numKernPoints) {
      var scale = i.toFloat / numKernPoints
      scale = (0.1f) + (1.0f - 0.1f) * (scale * scale)
      kernel += new Vector3(randRange(-1, 1), randRange(-1, 1), randRange(0, 1)).normalized * scale
      Console.println("kernel point: " + kernel(i).x + ", " + kernel(i).y + ", " + kernel(i).z)
    }
  }

  def getTexture(texType:Int):Int = {
    return textures.get(texType);
  }

  def bindForGeomPass(near:Float, far:Float) {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    glViewport(0, 0, screenWidth, screenHeight)

    val buffer = BufferUtils.createIntBuffer(3)
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

  def bindForSSAOPass(near:Float, far:Float) {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    glViewport(0, 0, screenWidth, screenHeight)

    val buffer = BufferUtils.createIntBuffer(1)
    buffer.put(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_SSAO)
    buffer.flip()

    glDrawBuffers(buffer)

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)
    glClear(GL_STENCIL_BUFFER_BIT)

    ssaoShader.bind()
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SSAO_NOISE))
    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH))
    ssaoShader.setUniform1i("uSSAONoiseSampler", 0)
    ssaoShader.setUniform1i("uNormalsDepthSampler", 1)
    ssaoShader.setUniform1f("uFarDistance", far)
    ssaoShader.setUniform1f("uNearDistance", near)
    ssaoShader.setUniform2f("uNoiseScale", screenWidth.toFloat / ssaoNoiseSize, screenHeight.toFloat / ssaoNoiseSize)
    ssaoShader.setUniform1i("uNumKernPoints", numKernPoints)
    ssaoShader.setUniform1f("uRadius", ssaoRadius);
    for (i <- 0 until numKernPoints) {
      val v = kernel(i);
      ssaoShader.setUniform3f("uKernPoints[" + i + "]", v.x, v.y, v.z)
    }
  }

  def unbindForSSAOPass() {
    ssaoShader.unbind()

    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glDrawBuffer(GL_BACK)
  }

  def bindForLightPass(near:Float, far:Float) {
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)
    glViewport(0, 0, screenWidth, screenHeight)

    val buffer = BufferUtils.createIntBuffer(1)
    buffer.put(GL_COLOR_ATTACHMENT0 + GBUFFER_TEXTURE_TYPE_LIGHT_PASS)
    buffer.flip()

    glDrawBuffers(buffer)

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)
    glClear(GL_STENCIL_BUFFER_BIT)

    lightingShader.bind()
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_ALBEDO))
    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH))
    glActiveTexture(GL_TEXTURE2)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SPECULAR))
    glActiveTexture(GL_TEXTURE3)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_LIGHT_PASS))
    glActiveTexture(GL_TEXTURE4)
    glBindTexture(GL_TEXTURE_2D, textures.get(GBUFFER_TEXTURE_TYPE_SSAO))
    lightingShader.setUniform1i("uDiffuseSampler", 0)
    lightingShader.setUniform1i("uNormalsDepthSampler", 1)
    lightingShader.setUniform1i("uSpecularSampler", 2)
    lightingShader.setUniform1i("uPreviousLightingSampler", 3)
    lightingShader.setUniform1i("uAmbientOcclusionSampler", 4)
    lightingShader.setUniform1f("uFarDistance", far)
    lightingShader.setUniform1f("uNearDistance", near)
  }

  def unbindForLightPass() {
    lightingShader.unbind()

    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glDrawBuffer(GL_BACK)
  }
}
