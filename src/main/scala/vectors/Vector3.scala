package com.awesome.vectors

import scala.math._

class Vector3(var x:Float, var y:Float, var z:Float) {
  def this(x:Float) = this(x, x, x)
  def this(x:Vector2, z:Float) = this(x.x, x.y, z)
  def +(other:Vector3):Vector3 = new Vector3(x + other.x, y + other.y, z + other.z)
  def -(other:Vector3):Vector3 = new Vector3(x - other.x, y - other.y, z - other.z)
  def *(scale:Float):Vector3 = new Vector3(x * scale, y * scale, z * scale)
  def dot(other:Vector3):Float = x*other.x + y*other.y + z*other.z
  def length():Float = sqrt(x*x + y*y + z*z).toFloat
  def normalized():Vector3 = {
    val l = length()
    if (l > 0) {
      return new Vector3(x/l, y/l, z/l)
    } else {
      return new Vector3(0)
    }
  }
  def lerp(other:Vector3, t:Float):Vector3 = {
    val diff = other - this
    return this + diff * t
  }
}
