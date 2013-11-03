package com.awesome

import vectors._
import models._

abstract class GameObject {
  var position:Vector3
  def draw()
  def getBoundingBox():BoundingBox
}
