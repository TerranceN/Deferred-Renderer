package com.awesome.models

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
//import org.lwjgl.opengl.GL15._

import com.awesome.textures._
import com.awesome.vectors._
import com.awesome.shaders._

class Material(
val diffuse:Either[Vector4, Texture],
val specular:Either[Vector4, Texture],
val shininess:Float) {

  def bind() {
    val program = ShaderProgram.getActiveShader

    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, 0)
    diffuse match {
      case Right(t) => {
        t.bind
        program.setUniform1i("uDiffuseNumComponents", t.numComponents)
        program.setUniform1i("uDiffuseSampler", 1)
        program.setUniform1f("uDiffuseTexture", 1.0f)
      }
      case Left(color) => {
        glDisable(GL_TEXTURE_2D)
        program.setUniform4f("uDiffuseColor", color.x, color.y, color.z, color.w)
        program.setUniform1f("uDiffuseTexture", 0.0f)
      }
    }

    glActiveTexture(GL_TEXTURE2)
    glBindTexture(GL_TEXTURE_2D, 0)
    specular match {
      case Right(t) => {
        t.bind
        program.setUniform1i("uSpecularNumComponents", t.numComponents)
        program.setUniform1i("uSpecularSampler", 2)
        program.setUniform1f("uSpecularTexture", 1.0f)
      }
      case Left(color) => {
        glDisable(GL_TEXTURE_2D);
        program.setUniform4f("uSpecularColor", color.x, color.y, color.z, color.w)
        program.setUniform1f("uSpecularTexture", 0.0f)
      }
    }

    program.setUniform1f("uShininess", shininess)
  }
}
