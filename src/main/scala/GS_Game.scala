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
    var life:Double = 1;
    var lifeDirection = 4;

    def update(deltaTime:Double) {
      velocity += new Vector3(0, -10, 0) * deltaTime.toFloat
      light.position += velocity * deltaTime.toFloat

      life += lifeDirection * 50 * deltaTime;
      if (life >= 50) {
        life = 50
        lifeDirection = -1
      }
      light.intensity = initIrradiance * (life.toFloat / 50)
    }
  }

  val normalMap = Texture.fromImage("assets/normal.jpg")

  val m = Model.fromFile("assets/crate_multitexture.dae")
  m.genBuffers()
  val level_geom = Model.fromFile("assets/test_level.dae")
  val m2 = Model.fromFile("assets/sphere2.dae")
  m2.genBuffers()
  val sceneGraph = Level.fromModel(level_geom, 100)
  val screenVBO = glGenBuffers()
  val gbuffer = new GBuffer()
  gbuffer.setup(1280, 720)

  var lights:Array[FallingLight] = Array()
  var wasLeftButtonDown:Boolean = false
  var wasRightButtonDown:Boolean = false
  var wasMDown:Boolean = false
  var wasNDown:Boolean = false
  var mouseLightEnabled = true
  var normalMapEnabled = true

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
    var normalizedMouse = new Vector2(
      (Mouse.getX / GLFrustum.screenWidth) * 2 - 1,
      (Mouse.getY / GLFrustum.screenHeight) * 2 - 1
    )
    var clip = normalizedMouse * (mouseLightDistance)
    val hAngle = GLFrustum.horizontalViewAngle
    val vAngle = GLFrustum.verticalViewAngle
    val f = 1 / tan(hAngle / 2)

    val view = new Vector3((clip.x * GLFrustum.aspectRatio / f).toFloat, (clip.y / f).toFloat, -mouseLightDistance)

    val lightIntensity = 1f

    if (!wasLeftButtonDown && Mouse.isButtonDown(0)) {
      lights = lights :+ new FallingLight(
        view,
        new Vector3(0, 0, 0),
        new Vector3(1, 0.5f, 0.5f) * lightIntensity
      )
    }

    if (!wasRightButtonDown && Mouse.isButtonDown(1)) {
      lights = lights :+ new FallingLight(
        view,
        new Vector3(0, 0, 0),
        new Vector3(0.5f, 0.5f, 1) * lightIntensity
      )
    }

    if (Keyboard.isKeyDown(Keyboard.KEY_M) && !wasMDown) {
      mouseLightEnabled = !mouseLightEnabled
    }

    if (Keyboard.isKeyDown(Keyboard.KEY_N) && !wasNDown) {
      normalMapEnabled = !normalMapEnabled
    }

    wasLeftButtonDown = Mouse.isButtonDown(0)
    wasRightButtonDown = Mouse.isButtonDown(1)
    wasMDown = Keyboard.isKeyDown(Keyboard.KEY_M)
    wasNDown = Keyboard.isKeyDown(Keyboard.KEY_N)

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

  def drawGeometry() {
    glMatrixMode(GL_MODELVIEW)
    glPushMatrix()
      glTranslated(0.0, 0, -10.4)
      glRotated(90, 0, 1, 0)

      //m.draw()
      sceneGraph.draw()
      //Console.println("Number of model draws: " + sceneGraph.draw())
    glPopMatrix()

    ShaderProgram.getActiveShader.setUniform1f("uNormalMapTexture", 0.0f)

    glPushMatrix()
      glTranslated(1.5, 2 * sin(y + 3.14), -8.0)
      glRotated(angle, 0, 1, 0)

      m.draw()
    glPopMatrix()

    glPushMatrix()
      glTranslated(-1.5, 2 * sin(y), -8.0)
      glRotated(angle, 0, 1, 0)

      m2.draw()
    glPopMatrix()
  }

  def draw() = {
    glLoadIdentity
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)

    //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

    gbuffer.bindForGeomPass(GLFrustum.nearClippingPlane, GLFrustum.farClippingPlane)
      glActiveTexture(GL_TEXTURE3)
      normalMap.bind()
      ShaderProgram.getActiveShader.setUniform1i("uNormalMapSampler", 3)
      if (normalMapEnabled) {
        ShaderProgram.getActiveShader.setUniform1f("uNormalMapTexture", 1.0f)
      }
      drawGeometry()
    gbuffer.unbindForGeomPass()

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    gbuffer.bindForLightPass(GLFrustum.nearClippingPlane, GLFrustum.farClippingPlane)
      val program = ShaderProgram.getActiveShader()
      var lightsToDraw = lights map (_.light)

      val mouseLightDistance = 5
      var normalizedMouse = new Vector2(
        (Mouse.getX / GLFrustum.screenWidth) * 2 - 1,
        (Mouse.getY / GLFrustum.screenHeight) * 2 - 1
      )
      var clip = normalizedMouse * (mouseLightDistance)
      val hAngle = GLFrustum.horizontalViewAngle
      val vAngle = GLFrustum.verticalViewAngle
      val f = 1 / tan(hAngle / 2)

      val view = new Vector3((clip.x * GLFrustum.aspectRatio / f).toFloat, (clip.y / f).toFloat, -mouseLightDistance)

      if (mouseLightEnabled) {
        lightsToDraw = lightsToDraw :+ new Light(new Vector3(1, 1, 1), view)
      }

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

    checkError
  }
}
