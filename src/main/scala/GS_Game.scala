package com.awesome 

import org.lwjgl.opengl.GL11._
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

    glPushMatrix()
      glTranslated(2.0f, -2 * sin(y), -10.0f)
      glRotated(angle, 0, 1, 0)

      m.draw()
    glPopMatrix()

    glPushMatrix()
      glTranslated(-2.0f, -2 * sin(y + 3.14f), -10.0f)
      glRotated(angle, 0, 1, 0)

      m2.draw()
    glPopMatrix()

    ShaderProgram.useNone

    checkError
  }
}
