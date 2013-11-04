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
  class FallingLight(initPosition:Vector3, var velocity:Vector3, val initIrradiance:Vector3) {
    val light = new Light(initIrradiance, initPosition);
    var life:Double = 50;

    def update(deltaTime:Double) {
      velocity += new Vector3(0, -10, 0) * deltaTime.toFloat
      light.position += velocity * deltaTime.toFloat

      life -= 50 * deltaTime;
      light.intensity = initIrradiance * (life.toFloat / 50)
    }
  }

  val m = Model.fromFile("crate_multitexture.dae")
  m.genBuffers()
  val level_geom = Model.fromFile("test_level.dae")
  //val m2 = Model.fromFile("sphere.dae")
  val sceneGraph = Level.fromModel(level_geom, 0.5f)
  val screenVBO = glGenBuffers()
  val gbuffer = new GBuffer()
  gbuffer.setup(1280, 720)

  var lights:Array[FallingLight] = Array()
  var wasLeftButtonDown:Boolean = false;
  var wasRightButtonDown:Boolean = false;

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

    val mouseLightDistance = 5
    val hAngle = 90
    val vAngle = 59
    val xScale = tan((hAngle * Pi / 180) / 2) * 2 * mouseLightDistance
    val yScale = tan((vAngle * Pi / 180) / 2) * 2 * mouseLightDistance

    val lightIntensity = 1f

    if (!wasLeftButtonDown && Mouse.isButtonDown(0)) {
      lights = lights :+ new FallingLight(
        new Vector3(
          (((Mouse.getX / GLFrustum.screenWidth) * 2 - 1) * xScale).toFloat,
          (((Mouse.getY / GLFrustum.screenHeight) * 2 - 1) * yScale).toFloat,
          -5
        ),
        new Vector3(0, 0, 0),
        new Vector3(1, 0.5f, 0.5f) * lightIntensity
      )
    }

    if (!wasRightButtonDown && Mouse.isButtonDown(1)) {
      lights = lights :+ new FallingLight(
        new Vector3(
          (((Mouse.getX / GLFrustum.screenWidth) * 2 - 1) * xScale).toFloat,
          (((Mouse.getY / GLFrustum.screenHeight) * 2 - 1) * yScale).toFloat,
          -5
        ),
        new Vector3(0, 0, 0),
        new Vector3(0.5f, 0.5f, 1) * lightIntensity
      )
    }

    wasLeftButtonDown = Mouse.isButtonDown(0)
    wasRightButtonDown = Mouse.isButtonDown(1)

    var newLights:Array[FallingLight] = Array()
    for (l <- lights) {
      l.update(deltaTime)

      if (l.life > 0) {
        newLights = newLights :+ l
      }
    }

    lights = newLights
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

    //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

    gbuffer.bindForGeomPass(GLFrustum.nearClippingPlane, GLFrustum.farClippingPlane)
      m.renderSections(0).mtl.bind()
      glMatrixMode(GL_MODELVIEW)
      glPushMatrix()
        glTranslated(0.0, 0, -8.4)
        glRotated(60, 0, 1, 0)

        //m.draw()
        Console.println("Number of model draws: " + sceneGraph.draw())
      glPopMatrix()

      glPushMatrix()
        glTranslated(-0.0, 2 * sin(y + 3.14), -8.0)
        glRotated(angle, 0, 1, 0)

        m.draw()
      glPopMatrix()
    gbuffer.unbindForGeomPass()

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    gbuffer.bindForLightPass(GLFrustum.nearClippingPlane, GLFrustum.farClippingPlane)
      val program = ShaderProgram.getActiveShader()
      var lightsToDraw = lights map (_.light)

      val mouseLightDistance = 5
      val hAngle = 90
      val vAngle = 59
      val xScale = tan((hAngle * Pi / 180) / 2) * 2 * mouseLightDistance
      val yScale = tan((vAngle * Pi / 180) / 2) * 2 * mouseLightDistance

      lightsToDraw = lightsToDraw :+ new Light(
        new Vector3(1, 1, 1),
        new Vector3(
          (((Mouse.getX / GLFrustum.screenWidth) * 2 - 1) * xScale).toFloat,
          (((Mouse.getY / GLFrustum.screenHeight) * 2 - 1) * yScale).toFloat,
          -5
        )
      )

      lightsToDraw.grouped(10) foreach { lst =>
        val uNumLightsLocation = glGetUniformLocation(program.id, "uNumLights")
        glUniform1i(uNumLightsLocation, lst.length)

        for (i <- 0 until lst.length) {
          val irradianceLocation = glGetUniformLocation(program.id, "uLightIrradiance[" + i + "]")
          val positionLocation = glGetUniformLocation(program.id, "uLightPositions[" + i + "]")

          lst(i).set(irradianceLocation, positionLocation)
        }

        drawScreenVBO()
      }
    gbuffer.unbindForLightPass()

    mainSceneShader.bind()
      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, gbuffer.getTexture(TextureType.GBUFFER_TEXTURE_TYPE_LIGHT_PASS))
      mainSceneShader.setUniform1i("uSampler", 0)
      drawScreenVBO()
    mainSceneShader.unbind()

    Console.println(lights.length)

    checkError
  }
}
