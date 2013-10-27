package com.awesome.vectors

class Vector3(var x:Float, var y:Float, var z:Float) {
  def this(x:Float) = this(x, x, x)
  def +(other:Vector3):Vector3 = new Vector3(x + other.x, y + other.y, z + other.z)
  def -(other:Vector3):Vector3 = new Vector3(x - other.x, y - other.y, z - other.z)
  def *(scale:Float):Vector3 = new Vector3(x * scale, y * scale, z * scale)
}
