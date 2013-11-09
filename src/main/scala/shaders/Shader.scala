package com.awesome.shaders

import scala.io.Source
import org.lwjgl.opengl.ARBFragmentShader._
import org.lwjgl.opengl.ARBVertexShader._
import org.lwjgl.opengl.ARBShaderObjects._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import java.nio.FloatBuffer

class ShaderProgram(vert:VertexShader, frag:FragmentShader) {
  def this(vert:String, frag:String) = this(new VertexShader(vert), new FragmentShader(frag))

  var id = glCreateProgramObjectARB()

  try {
    glAttachObjectARB(id, vert.id)
    glAttachObjectARB(id, frag.id)

    glLinkProgramARB(id)

    if (glGetObjectParameteriARB(id, GL_OBJECT_LINK_STATUS_ARB) == GL_FALSE) {
      throw new Exception("Error creating shader: " + getShaderError())
    }
  } catch {
    case e:Exception => {
      e.printStackTrace
    }
  }

  def bind() {
    glUseProgramObjectARB(id)
    ShaderProgram.activeShader = this
  }

  def unbind() {
    ShaderProgram.useNone
  }

  def getShaderError():String = {
    return glGetInfoLogARB(id, glGetObjectParameteriARB(id, GL_OBJECT_INFO_LOG_LENGTH_ARB))
  }

  def setUniform1i(name:String, x:Int) {
    glUniform1i(glGetUniformLocation(id, name), x)
  }

  def setUniform2i(name:String, x:Int, y:Int) {
    glUniform2i(glGetUniformLocation(id, name), x, y)
  }

  def setUniform3i(name:String, x:Int, y:Int, z:Int) {
    glUniform3i(glGetUniformLocation(id, name), x, y, z)
  }

  def setUniform4i(name:String, x:Int, y:Int, z:Int, w:Int) {
    glUniform4i(glGetUniformLocation(id, name), x, y, z, w)
  }

  def setUniform1f(name:String, x:Float) {
    glUniform1f(glGetUniformLocation(id, name), x)
  }

  def setUniform2f(name:String, x:Float, y:Float) {
    glUniform2f(glGetUniformLocation(id, name), x, y)
  }

  def setUniform3f(name:String, x:Float, y:Float, z:Float) {
    glUniform3f(glGetUniformLocation(id, name), x, y, z)
  }

  def setUniform4f(name:String, x:Float, y:Float, z:Float, w:Float) {
    glUniform4f(glGetUniformLocation(id, name), x, y, z, w)
  }

  def setUniformMatrix4(name:String, buf:FloatBuffer) {
    glUniformMatrix4(glGetUniformLocation(id, name), false, buf)
  }
}

object ShaderProgram {
  var activeShader:ShaderProgram = null
  def useNone() {
    glUseProgramObjectARB(0)
    activeShader = null
  }
  def getActiveShader():ShaderProgram = {
    return activeShader
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
      val source = Source.fromFile(file)
      val sourceString = source.mkString
      glShaderSourceARB(id, sourceString)
      glCompileShaderARB(id)
      source.close

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
