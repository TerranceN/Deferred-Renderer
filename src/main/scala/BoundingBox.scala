package com.awesome

import scala.math._

import vectors._

// Axis-aligned
class BoundingBox(val lower:Vector3, val upper:Vector3) {
  def combine(other:BoundingBox):BoundingBox =
    new BoundingBox(
      new Vector3(min(lower.x, other.lower.x), min(lower.y, other.lower.y), min(lower.z, other.lower.z)),
      new Vector3(max(upper.x, other.upper.x), max(upper.y, other.upper.y), max(upper.z, other.upper.z)))
  def contains(point:Vector3):Boolean = {
    return (point.x >= lower.x && point.x <= upper.x
         && point.y >= lower.y && point.y <= upper.y
         && point.z >= lower.z && point.z <= upper.z)
  }
  def containsStrict(point:Vector3):Boolean = {
    return (point.x > lower.x && point.x < upper.x
         && point.y > lower.y && point.y < upper.y
         && point.z > lower.z && point.z < upper.z)
  }
  def size():Vector3 = upper - lower
}

object BoundingBox {
  def fromPoints(p1:Vector3, p2:Vector3):BoundingBox = {
    return new BoundingBox(
      new Vector3(min(p1.x, p2.x), min(p1.y, p2.y), min(p1.z, p2.z)),
      new Vector3(max(p1.x, p2.x), max(p1.y, p2.y), max(p1.z, p2.z)))
  }
}
