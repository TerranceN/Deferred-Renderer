package com.awesome 

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.{
  Display,
  DisplayMode
}
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

  val program = new ShaderProgram(
    new VertexShader("test.vert"),
    new FragmentShader("test.frag"))

  var y:Double = 0
  var angle:Double = 0

  def init() = {
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

    program.bind

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

    val uNumLightsLocation = glGetUniformLocation(program.id, "uNumLights")
    glUniform1i(uNumLightsLocation, lights.length)

    for (i <- 0 until lights.length) {
      val irradianceLocation = glGetUniformLocation(program.id, "uLightIrradiance[" + i + "]")
      val positionLocation = glGetUniformLocation(program.id, "uLightPositions[" + i + "]")

      lights(i).set(irradianceLocation, positionLocation)
    }

    glPushMatrix()
      glTranslated(2.0, 2 * sin(y), -8.0)
      glRotated(angle, 0, 1, 0)

      m.draw()
    glPopMatrix()

    glPushMatrix()
      glTranslated(-2.0, 2 * sin(y + 3.14), -8.0)
      glRotated(angle, 0, 1, 0)

      m2.draw()
    glPopMatrix()

    ShaderProgram.useNone

    checkError
  }
}
