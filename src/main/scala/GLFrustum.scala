package com.awesome

import org.lwjgl.util.glu.GLU;

import scala.math._

object GLFrustum {
  var screenWidth:Float = 0
  var screenHeight:Float = 0
  var horizontalViewAngle:Float = 0
  var verticalViewAngle:Float = 0
  var nearClippingPlane:Float = 0
  var farClippingPlane:Float = 0
  var aspectRatio:Float = 0

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
    GLU.gluPerspective((horizontalViewAngle * 180 / Pi).toFloat, aspectRatio, nearClippingPlane, farClippingPlane)
  }
}
