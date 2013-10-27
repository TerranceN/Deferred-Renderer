package com.awesome 

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.{
  Display,
  DisplayMode
}
import org.lwjgl.BufferUtils
import org.lwjgl.input._
import scala.util.Random
import scala.math._
import org.lwjgl.util.glu.GLU._

import textures._
import shaders._
import models._
import vectors._
import lighting._

class GS_Game extends GameState {
  val m = new Model("crate_multitexture.dae")
  val m2 = new Model("sphere.dae")
  val screenVBO = glGenBuffers()
  val gbuffer = new GBuffer()
  gbuffer.setup(1280, 720)

  val mainSceneShader = new ShaderProgram(
    new VertexShader("shaders/mainScene.vert"),
    new FragmentShader("shaders/mainScene.frag")
  )

  var y:Double = 0
  var angle:Double = 0

  def init() = {
    setupScreenVBO()
  }

  def setupScreenVBO() {
    val vertexBuffer = BufferUtils.createFloatBuffer(16)
    vertexBuffer.put(-1.0f); vertexBuffer.put(-1.0f)
    vertexBuffer.put( 0.0f); vertexBuffer.put( 0.0f)

    vertexBuffer.put( 1.0f); vertexBuffer.put(-1.0f)
    vertexBuffer.put( 1.0f); vertexBuffer.put( 0.0f)

    vertexBuffer.put( 1.0f); vertexBuffer.put( 1.0f)
    vertexBuffer.put( 1.0f); vertexBuffer.put( 1.0f)

    vertexBuffer.put(-1.0f); vertexBuffer.put( 1.0f)
    vertexBuffer.put( 0.0f); vertexBuffer.put( 1.0f)
    vertexBuffer.flip()

    glBindBuffer(GL_ARRAY_BUFFER, screenVBO)
    glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)
  }

  def drawScreenVBO() {
    glMatrixMode(GL_PROJECTION)
    glPushMatrix()
    glLoadIdentity()
    glMatrixMode(GL_MODELVIEW)
    glPushMatrix()
    glLoadIdentity()

    val program = ShaderProgram.getActiveShader()

    val aCoordLocation = glGetAttribLocation(program.id, "aCoord")
    val aTexCoordLocation = glGetAttribLocation(program.id, "aTexCoord")

    glEnableVertexAttribArray(aCoordLocation)
    glEnableVertexAttribArray(aTexCoordLocation)

    glBindBuffer(GL_ARRAY_BUFFER, screenVBO)
    glVertexAttribPointer(aCoordLocation, 3, GL_FLOAT, false, 4 * 4, 0)

    glVertexAttribPointer(aTexCoordLocation, 2, GL_FLOAT, false, 4 * 4, 2 * 4)

    glDrawArrays(GL_QUADS, 0, 4)

    glDisableVertexAttribArray(aCoordLocation)
    glDisableVertexAttribArray(aTexCoordLocation)

    glMatrixMode(GL_MODELVIEW)
    glPopMatrix()
    glMatrixMode(GL_PROJECTION)
    glPopMatrix()
    glMatrixMode(GL_MODELVIEW)
  }

  def update(deltaTime:Double) = {
    y += 1 * deltaTime
    angle += 40 * deltaTime
  }

  def checkError() {
    val error = glGetError
    if (error != GL_NO_ERROR) {
      Console.println("OpenGL Error: " + error)
      Console.println(gluErrorString(error))
    }
  }

  def draw() = {
    glLoadIdentity
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)

    gbuffer.bindForGeomPass(0.1f, 100f)
      glMatrixMode(GL_MODELVIEW)
      glPushMatrix()
        glTranslated(0.0, 2 * sin(y), -8.4)
        glRotated(angle, 0, 1, 0)

        m.draw()
      glPopMatrix()

      glPushMatrix()
        glTranslated(-0.0, 2 * sin(y + 3.14), -8.0)
        glRotated(angle, 0, 1, 0)

        m2.draw()
      glPopMatrix()
    gbuffer.unbindForGeomPass()

    mainSceneShader.bind()
      val lightDistance = 3.0;

      val mouseLightDistance = 5
      val hAngle = 90
      val vAngle = 59
      val xScale = tan((hAngle * Pi / 180) / 2) * 2 * mouseLightDistance
      val yScale = tan((vAngle * Pi / 180) / 2) * 2 * mouseLightDistance

      val lights = Array(
        new Light(
          new Vector3(1, 0, 0),
          new Vector3(
            (lightDistance * cos(y)).toFloat,
            0,
            (lightDistance * sin(y)).toFloat - 8
          )
        ),
        new Light(
          new Vector3(0, 0, 1),
          new Vector3(
            (lightDistance * cos(2 * y)).toFloat,
            0,
            (lightDistance * sin(2 * y)).toFloat - 8
          )
        ),
        new Light(
          new Vector3(1, 1, 1),
          new Vector3(
            (((Mouse.getX / 1280.0) * 2 - 1) * xScale).toFloat,
            (((Mouse.getY / 720.0) * 2 - 1) * yScale).toFloat,
            -5
          )
        )
      )

      val uNumLightsLocation = glGetUniformLocation(mainSceneShader.id, "uNumLights")
      glUniform1i(uNumLightsLocation, lights.length)

      for (i <- 0 until lights.length) {
        val irradianceLocation = glGetUniformLocation(mainSceneShader.id, "uLightIrradiance[" + i + "]")
        val positionLocation = glGetUniformLocation(mainSceneShader.id, "uLightPositions[" + i + "]")

        lights(i).set(irradianceLocation, positionLocation)
      }

      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, gbuffer.getTexture(TextureType.GBUFFER_TEXTURE_TYPE_ALBEDO))
      glActiveTexture(GL_TEXTURE1)
      glBindTexture(GL_TEXTURE_2D, gbuffer.getTexture(TextureType.GBUFFER_TEXTURE_TYPE_NORMALS_DEPTH))
      glActiveTexture(GL_TEXTURE2)
      glBindTexture(GL_TEXTURE_2D, gbuffer.getTexture(TextureType.GBUFFER_TEXTURE_TYPE_SPECULAR))
      mainSceneShader.setUniform1i("uDiffuseSampler", 0)
      mainSceneShader.setUniform1i("uNormalsDepthSampler", 1)
      mainSceneShader.setUniform1i("uSpecularSampler", 2)
      mainSceneShader.setUniform1f("uFarDistance", 100)
      mainSceneShader.setUniform1f("uNearDistance", 0.1f)
      drawScreenVBO()
    mainSceneShader.unbind()

    checkError
  }
}
