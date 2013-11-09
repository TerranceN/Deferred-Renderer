package com.awesome

import org.lwjgl.util.glu.GLU;

import scala.math._
import matricies._

object GLFrustum {
  var screenWidth:Float = 0
  var screenHeight:Float = 0
  var horizontalViewAngle:Float = 0
  var verticalViewAngle:Float = 0
  var nearClippingPlane:Float = 0
  var farClippingPlane:Float = 0
  var aspectRatio:Float = 0

  var projectionStack:List[Matrix4] = List(new Matrix4())
  var modelviewStack:List[Matrix4] = List(new Matrix4())

  def projectionMatrix = projectionStack.head
  def modelviewMatrix = modelviewStack.head

  def projectionMatrix_=(m:Matrix4) {
    projectionStack = m :: projectionStack.tail
  }

  def modelviewMatrix_=(m:Matrix4) {
    modelviewStack = m :: modelviewStack.tail
  }

  def pushProjection() {
    projectionStack = projectionMatrix.copy() :: projectionStack
  }
  def popProjection() {
    projectionStack = projectionStack.tail
  }

  def pushModelview() {
    modelviewStack = modelviewMatrix.copy() :: modelviewStack
  }
  def popModelview() {
    modelviewStack = modelviewStack.tail
  }

  def setFrustrum(hViewAngle:Float, width:Float, height:Float, near:Float, far:Float) {
    screenWidth = width
    screenHeight = height
    horizontalViewAngle = (hViewAngle * Pi / 180).toFloat
    aspectRatio = screenWidth / screenHeight
    nearClippingPlane = near
    farClippingPlane = far
    verticalViewAngle = 2 * atan((1 / aspectRatio) / tan(horizontalViewAngle / 2)).toFloat

    applyFrustrum()
  }

  def applyFrustrum() {
    projectionMatrix = (Matrix4.perspective(horizontalViewAngle, aspectRatio, nearClippingPlane, farClippingPlane))
    GLU.gluPerspective((horizontalViewAngle * 180 / Pi).toFloat, aspectRatio, nearClippingPlane, farClippingPlane)
  }
}
