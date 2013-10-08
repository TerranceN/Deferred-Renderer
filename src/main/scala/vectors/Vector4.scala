package com.awesome.vectors

class Vector4(var x:Float, var y:Float, var z:Float, var w:Float) {
  def plus(other:Vector4):Vector4 = new Vector4(x + other.x, y + other.y, z + other.z, w + other.w)
}
