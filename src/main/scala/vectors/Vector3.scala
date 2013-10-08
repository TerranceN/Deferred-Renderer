package com.awesome.vectors

class Vector3(var x:Float, var y:Float, var z:Float) {
  def plus(other:Vector3):Vector3 = new Vector3(x + other.x, y + other.y, z + other.z)
}
