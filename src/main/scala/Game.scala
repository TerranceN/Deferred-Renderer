package com.awesome 

// What GL version you plan on using
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.{
  Display, 
  DisplayMode
}
import org.lwjgl.input.Keyboard
import Keyboard._
import scala.collection.mutable.Stack

object Game extends App {
  Console.println(System.getProperty("java.library.path"))
  val fps = 60
  var gameStates = new Stack[GameState]
  var lastFrameTime:Long = 0
  gameStates.push(new GS_Init)
  gameStates.head.init

  var fpsCountStart:Long = System.nanoTime
  var fpsFrameCount:Int = 0

  while (!Display.isCloseRequested && !gameStates.isEmpty) {
    val startTime = System.nanoTime
    val currentState = gameStates.head

    // update the current state
    currentState.update(lastFrameTime / 1000000000d)

    // if that update caused the state to die, remove to from the stack
    // otherwise, draw the state
    if (!currentState.isAlive) {
      gameStates.pop
    } else {
      currentState.draw
    }

    // update window, handle events, etc
    Display.update

    // check if there is a next state
    if (currentState.hasNextState) {
      gameStates.push(currentState.takeNextState)
      gameStates.head.init
    }

    // calculate the time taken and delay the game accordingly
    //val endTime = System.nanoTime
    //val delayTime = (1000d / fps.toDouble) - ((endTime - startTime) / 1000000)
    //if (delayTime > 0) Thread.sleep(delayTime.toInt)
    fpsFrameCount += 1
    val fpsTimeDiff = System.nanoTime - fpsCountStart
    if (fpsTimeDiff >= 1000000000) {
      Console.println("fps: " + (1000000000d / (fpsTimeDiff / fpsFrameCount)))
      fpsFrameCount = 0
      fpsCountStart = System.nanoTime
    }
    lastFrameTime = System.nanoTime - startTime
  }

  Display.destroy()
  sys.exit(0)
}
