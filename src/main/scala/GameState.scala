package com.awesome 

trait GameState {
  // functions to run the game state
  def init():Unit
  def update(deltaTime:Double):Unit
  def draw():Unit

  // next states
  private var nextState:GameState = null
  def hasNextState():Boolean = nextState != null
  def peekNextState():GameState = nextState
  def takeNextState():GameState = {
    val retVal = nextState
    nextState = null
    return retVal
  }
  def setNextState(newNextState:GameState) = {
    nextState = newNextState
  }

  // killing states
  private var alive = true
  def killState() = {
    alive = false
  }
  def isAlive():Boolean = alive
}
