package com.awesome.models

import com.awesome.textures._
import com.awesome.vectors._

class Material(
val diffuse:Either[Vector4, Texture],
val specular:Either[Vector4, Texture],
val shininess:Float) {
}
