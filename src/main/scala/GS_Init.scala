package com.awesome 

import org.lwjgl.opengl.GL11._
import org.lwjgl.util.glu.GLU;
import org.lwjgl.opengl.{
  Display, 
  DisplayMode
}

class GS_Init extends GameState {
  def init() = {
    // screen size
    val screenWidth = 1280
    val screenHeight = 720
    val displayMode = new DisplayMode(screenWidth, screenHeight)

    // create a new window
    Display.setTitle("LWJGL Test")
    Display.setDisplayMode(displayMode)
    Display.create()

    // opengl settings
    glClearColor(0f, 0f, 0f, 1f)
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity
    GLU.gluPerspective(60, screenWidth.toFloat / screenHeight, 0.1f, 1000)
    glMatrixMode(GL_MODELVIEW)
    glLoadIdentity

    glClearDepth(1.0f);                   // Set background depth to farthest
    glEnable(GL_DEPTH_TEST)
    glDepthFunc(GL_LEQUAL);    // Set the type of depth-test
    glShadeModel(GL_SMOOTH);   // Enable smooth shading
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);  // Nice perspective corrections

    // next next state and kill this state
    setNextState(new GS_Game)
    killState
  }

  def update(deltaTime:Double) = {}
  def draw() = {}
}
