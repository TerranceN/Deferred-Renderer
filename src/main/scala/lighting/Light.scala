package com.awesome.lighting

import org.lwjgl.opengl.GL20._

import com.awesome.vectors._

class Light(var intensity:Vector3, var position:Vector3) {
  def set(intensityLocation:Int, positionLocation:Int) {
    glUniform3f(intensityLocation, intensity.x, intensity.y, intensity.z)
    glUniform3f(positionLocation, position.x, position.y, position.z)
  }
}
