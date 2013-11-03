package com.awesome.geometry

import com.awesome.vectors._
import com.awesome.BoundingBox

class LineSegment(val point1:Vector3, val point2:Vector3) {
  val slope = point2 - point1
  val line = new Line(slope, point1)

  def intersection(plane:Plane):Option[Vector3] = {
    line.intersection(plane) match {
      case None => None
      case Some(i) => if (BoundingBox.fromPoints(point1, point2).contains(i)) Some(i) else None
    }
  }
  def intersectionStrict(plane:Plane):Option[Vector3] = {
    line.intersection(plane) match {
      case None => None
      case Some(i) => if (BoundingBox.fromPoints(point1, point2).containsStrict(i)) Some(i) else None
    }
  }
}
