package com.awesome.geometry

import scala.math._

import com.awesome.vectors._

class Plane(newNormal:Vector3, pointOnPlane:Vector3) {
  val normal = newNormal.normalized
  val distanceFromOrigin = normal.dot(pointOnPlane)

  def whichSide(point:Vector3):Int = {
    val result = normal.dot(point) - distanceFromOrigin

    if (abs(result) < 0.01) {
      return 0
    } else if (result < 0) {
      return -1
    } else {
      return 1
    }
  }
}
