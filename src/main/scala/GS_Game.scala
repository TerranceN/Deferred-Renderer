package com.awesome 

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.{
  Display,
  DisplayMode
}
import org.lwjgl.input._
import scala.util.Random
import scala.math._


class GS_Game extends GameState {
  val m = Model.fromObjectFile("test2.obj")
  val t = Texture.fromImage("crate.jpg")

  var y:Double = 0
  var angle:Double = 0

  def init() = {
  }

  def update(deltaTime:Double) = {
    y += 1 * deltaTime
    angle += 10 * deltaTime
  }

  def draw() = {
    glLoadIdentity
    glClear(GL_COLOR_BUFFER_BIT)
    glClear(GL_DEPTH_BUFFER_BIT)

    glTranslated(0.0f, -2 * cos(y) - 3, -10.0f)
    glRotated(angle, 0, 1, 0)

    m.draw()
  }
}
