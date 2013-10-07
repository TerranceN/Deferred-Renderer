package com.awesome

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.BufferUtils
import scala.io.Source
import java.nio.FloatBuffer
import java.nio.ByteBuffer
import scala.util.Random

class Model(
val verticies:Array[Float],
val texCoords:Array[Float]) {
  val vertexBuffer = glGenBuffers()
  glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
  val vertexFloatBuffer = BufferUtils.createFloatBuffer(verticies.length)
  vertexFloatBuffer.put(verticies)
  vertexFloatBuffer.flip
  glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
  glBufferData(GL_ARRAY_BUFFER, vertexFloatBuffer, GL_STATIC_DRAW)

  val random = new Random(System.currentTimeMillis())

  val colorBuffer = glGenBuffers()
  var colors:Array[Float] = Array()
  for (i <- 0 until verticies.length) {
    val step = 9
    val r:Float = random.nextFloat() * 0.9f + 0.1f
    if (i % step == 0) {
      random.nextInt(4) match {
        case 0 => {
          colors = colors ++ Array(r, 0.0f, 0.0f, 1.0f)
          colors = colors ++ Array(r, 0.0f, 0.0f, 1.0f)
          colors = colors ++ Array(r, 0.0f, 0.0f, 1.0f)
        }
        case 1 => {
          colors = colors ++ Array(0.0f, r, 0.0f, 1.0f)
          colors = colors ++ Array(0.0f, r, 0.0f, 1.0f)
          colors = colors ++ Array(0.0f, r, 0.0f, 1.0f)
        }
        case 2 => {
          colors = colors ++ Array(r, r, 0.0f, 1.0f)
          colors = colors ++ Array(r, r, 0.0f, 1.0f)
          colors = colors ++ Array(r, r, 0.0f, 1.0f)
        }
        case 3 => {
          colors = colors ++ Array(0.0f, 0.0f, r, 1.0f)
          colors = colors ++ Array(0.0f, 0.0f, r, 1.0f)
          colors = colors ++ Array(0.0f, 0.0f, r, 1.0f)
        }
      }
    }
  }
  val colorFloatBuffer = BufferUtils.createFloatBuffer(colors.length)
  colorFloatBuffer.put(colors)
  colorFloatBuffer.flip
  glBindBuffer(GL_ARRAY_BUFFER, colorBuffer)
  glBufferData(GL_ARRAY_BUFFER, colorFloatBuffer, GL_STATIC_DRAW)

  val texCoordBuffer = glGenBuffers()
  val texCoordFloatBuffer = BufferUtils.createFloatBuffer(texCoords.length)
  texCoordFloatBuffer.put(texCoords)
  texCoordFloatBuffer.flip
  glBindBuffer(GL_ARRAY_BUFFER, texCoordBuffer)
  //glBufferData(GL_ARRAY_BUFFER, texCoordFloatBuffer, GL_STATIC_DRAW)

  val program = new ShaderProgram(
    new VertexShader("test.vert"),
    new FragmentShader("test.frag"))

  def checkError() {
    val error = glGetError
    if (error != GL_NO_ERROR) {
      Console.println("OpenGL Error: " + error)
    }
  }

  def draw() {
    program.bind

    val aCoordLocation = glGetAttribLocation(program.id, "aCoord")
    val aColorLocation = glGetAttribLocation(program.id, "aColor")
    val aTexCoordLocation = glGetAttribLocation(program.id, "aTexCoord")

    glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
    glVertexAttribPointer(aCoordLocation, 3, GL_FLOAT, false, 0, 0)
    glEnableVertexAttribArray(aCoordLocation)

    glBindBuffer(GL_ARRAY_BUFFER, colorBuffer)
    glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, 0)
    glEnableVertexAttribArray(aColorLocation)

    //glBindBuffer(GL_ARRAY_BUFFER, texCoordBuffer)
    //glVertexAttribPointer(aTexCoordLocation, 3, GL_FLOAT, false, 0, 0)
    //glEnableVertexAttribArray(aTexCoordLocation)

    glDrawArrays(GL_TRIANGLES, 0, verticies.length / 3)

    glBindBuffer(GL_ARRAY_BUFFER, 0)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    glDisableVertexAttribArray(aCoordLocation)
    glDisableVertexAttribArray(aColorLocation)
    //glDisableVertexAttribArray(aTexCoordLocation)

    ShaderProgram.useNone

    checkError
  }
}

object Model {
  def fromObjectFile(fileName:String):Model = {
    var verticies:Array[Float] = Array()
    var indicies:Array[Short] = Array()

    def addIndicies(str:String) = {
      val bufferIndicies = str.split("/")
      indicies = indicies :+ (bufferIndicies(0).toByte - 1).toShort
    }

    try {
      for(line <- Source.fromFile(fileName).getLines()) {
        val tokens = line.split(" ")

        tokens(0) match {
          case "v" => {
            verticies = verticies :+ tokens(1).toFloat
            verticies = verticies :+ tokens(2).toFloat
            verticies = verticies :+ tokens(3).toFloat
          }
          case "f" => {
            addIndicies(tokens(1))
            addIndicies(tokens(2))
            addIndicies(tokens(3))
          }
          case _ => {}
        }
      }
    } catch {
      case e:Exception => {
        Console.println("Inconsistent file: " + fileName)
        e.printStackTrace
      }
    }

    var newVerticies:Array[Float] = Array()

    for (i <- 0 until indicies.length) {
      val index = 3 * indicies(i)
      newVerticies = newVerticies ++ Array(verticies(index), verticies(index+1), verticies(index+2))
    }

    return new Model(newVerticies, Array())
  }
}
