package com.awesome

import scala.io.Source
import org.lwjgl.opengl.ARBFragmentShader._
import org.lwjgl.opengl.ARBVertexShader._
import org.lwjgl.opengl.ARBShaderObjects._
import org.lwjgl.opengl.GL11._

class ShaderProgram(vert:VertexShader, frag:FragmentShader) {
  var id = glCreateProgramObjectARB()

  glAttachObjectARB(id, vert.id)
  glAttachObjectARB(id, frag.id)

  glLinkProgramARB(id)

  def bind() {
    glUseProgramObjectARB(id)
  }
}

object ShaderProgram {
  def useNone() {
    glUseProgramObjectARB(0)
  }
}

class VertexShader(file:String) extends Shader {
  loadFromFile(file, GL_VERTEX_SHADER_ARB)
}

class FragmentShader(file:String) extends Shader {
  loadFromFile(file, GL_FRAGMENT_SHADER_ARB)
}

trait Shader {
  var id = 0
  def loadFromFile(file:String, shaderType:Int) {
    try {
      id = glCreateShaderObjectARB(shaderType)
      glShaderSourceARB(id, Source.fromFile(file).mkString)
      glCompileShaderARB(id)

      if (glGetObjectParameteriARB(id, GL_OBJECT_COMPILE_STATUS_ARB) == GL_FALSE) {
        throw new Exception("Error creating shader: " + getShaderError())
      }
    } catch {
      case e:Exception => {
        e.printStackTrace
      }
    }
  }

  def getShaderError():String = {
    return glGetInfoLogARB(id, glGetObjectParameteriARB(id, GL_OBJECT_INFO_LOG_LENGTH_ARB))
  }
}
