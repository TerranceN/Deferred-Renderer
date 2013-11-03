package com.awesome.geometry

import scala.math._

import com.awesome.vectors._

class Line(newSlope:Vector3, val point:Vector3) {
  val slope = newSlope.normalized
  def intersection(plane:Plane):Option[Vector3] = {
    if (abs(slope.dot(plane.normal)) < 0.01) { // if plane normal is perpendicular to the line's slope there is no intersection point
      return None
    } else {
      var t = ((plane.distanceFromOrigin - plane.normal.dot(point)) / (plane.normal.dot(slope)))
      var i = slope * t + point
      //Console.println("t: " + t)
      //Console.println("line slope: " + slope.x + ", " + slope.y + ", " + slope.z)
      return Some(i)
    }
  }
}
