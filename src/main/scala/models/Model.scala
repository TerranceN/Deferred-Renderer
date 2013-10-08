package com.awesome.models

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.util.glu.GLU._
import org.lwjgl.BufferUtils
import scala.xml._
import java.io.File
import scala.util.Random

import com.awesome.shaders._
import com.awesome.textures._
import com.awesome.vectors._

class Model(fileName:String) {
  class RenderSection(
  val mtl:Material,
  val data:Array[Float],
  val vertexOffset:Int,
  val normalOffset:Int,
  val texCoordOffset:Int,
  val colorOffset:Int) {
    val dataBufferId = glGenBuffers
    val dataBuffer = BufferUtils.createFloatBuffer(data.length)
    dataBuffer.put(data)
    dataBuffer.flip
    glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)
    glBufferData(GL_ARRAY_BUFFER, dataBuffer, GL_STATIC_DRAW)

    val stride = 3 + 3 + 2 + 4
    val numVerticies = data.length / stride

    data.grouped(stride) foreach { list =>
      val position = "(" + list(0) + ", " + list(1) + ", " + list(2) + ")"
      val texCoord = "(" + list(6) + ", " + list(7) + ")"
      val color = "(" + list(8) + ", " + list(9) + ", " + list(10) + ", " + list(11) + ")"
      Console.println(texCoord)
    }

    def checkError() {
      val error = glGetError
      if (error != GL_NO_ERROR) {
        Console.println("OpenGL Error: " + error)
        Console.println(gluErrorString(error))
      }
    }

    def draw() = {
      val program = ShaderProgram.getActiveShader

      val aCoordLocation = glGetAttribLocation(program.id, "aCoord")
      val aNormalLocation = glGetAttribLocation(program.id, "aNormal")
      val aTexCoordLocation = glGetAttribLocation(program.id, "aTexCoord")
      val aColorLocation = glGetAttribLocation(program.id, "aColor")

      val uSamplerLocation = glGetUniformLocation(program.id, "uSampler")

      glEnableVertexAttribArray(aCoordLocation)
      glEnableVertexAttribArray(aNormalLocation)
      glEnableVertexAttribArray(aTexCoordLocation)
      glEnableVertexAttribArray(aColorLocation)

      mtl.diffuse match {
        case Right(t) => {
          t.bind
          glUniform1i(uSamplerLocation, 0);
        }
        case _ => {
          glDisable(GL_TEXTURE_2D);
        }
      }

      glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)

      glVertexAttribPointer(aCoordLocation, 3, GL_FLOAT, false, stride*4, 0)
      glVertexAttribPointer(aNormalLocation, 3, GL_FLOAT, false, stride*4, 3*4)
      glVertexAttribPointer(aTexCoordLocation, 2, GL_FLOAT, false, stride*4, 6*4)
      glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, stride*4, 8*4)

      glDrawArrays(GL_TRIANGLES, 0, numVerticies)

      glDisableVertexAttribArray(aCoordLocation)
      glDisableVertexAttribArray(aNormalLocation)
      glDisableVertexAttribArray(aTexCoordLocation)
      glDisableVertexAttribArray(aColorLocation)
    }
  }

  var renderSections:Array[RenderSection] = Array()
  // load from file
  var file = XML.loadFile(new File(fileName))
  // load into an array of RenderSections

  var positions:Array[Float] = Array()
  var texCoords:Array[Float] = Array()
  var normals:Array[Float] = Array()

  val geometry = (file \\ "geometry")(0)
  val geometryName:String = (geometry \ "@name").text

  val meshId = geometryName + "-mesh-positions-array"
  val normalsId = geometryName + "-mesh-normals-array"
  val mapId = geometryName + "-mesh-map-0-array"

  (geometry \\ "float_array") map (floatArray => (floatArray \ "@id").text match {
    case `meshId` => {
      positions = floatArray.text.split(" ") map (_.toFloat)
    }
    case `normalsId` => {
      normals = floatArray.text.split(" ") map (_.toFloat)
    }
    case `mapId` => {
      texCoords = floatArray.text.split(" ") map (_.toFloat)
    }
  })

  (geometry \\ "polylist") map loadPolylist

  def loadPolylist(xml:Node) {
    val material = (file \\ "material" filter(m => (m \ "@id").text == (xml \ "@material").text))(0)
    val materialName = (material \ "@name").text

    val effect = (file \\ "effect" filter(m => (m \ "@id").text == (materialName + "-effect")))(0)
    val diffuse = (effect \\ "diffuse")(0)

    var diffuseColor:Array[Float] = Array()

    //if () { // has texture
    //} else if () { // has diffuse color
    //}

    val random = new Random(System.currentTimeMillis())
    if (diffuseColor.length == 0) {
      diffuseColor = Array(1.0f, 1.0f, 1.0f, 1.0f)
    }

    val materialObject = new Material(Right(Texture.fromImage("crate.jpg")))
    var data:Array[Float] = Array()

    (xml \ "p").text.split(" ").grouped(3) foreach { list =>
      //val r:Float = random.nextFloat() * 0.9f + 0.1f
      //random.nextInt(4) match {
      //  case 0 => {
      //    diffuseColor = Array(r, 0.0f, 0.0f, 1.0f)
      //  }
      //  case 1 => {
      //    diffuseColor = Array(0.0f, r, 0.0f, 1.0f)
      //  }
      //  case 2 => {
      //    diffuseColor = Array(r, r, 0.0f, 1.0f)
      //  }
      //  case 3 => {
      //    diffuseColor = Array(0.0f, 0.0f, r, 1.0f)
      //  }
      //}

      val indicies = list map (_.toInt)
      val newPositions = (positions.slice(indicies(0)*3, indicies(0)*3 + 3))
      val newNormals = (normals.slice(indicies(1)*3, indicies(1)*3 + 3))
      val newTexCoords = (texCoords.slice(indicies(2)*2, indicies(2)*2 + 2))

      data = data ++ newPositions ++ newNormals ++ newTexCoords ++ diffuseColor
    }

    renderSections = renderSections :+ new RenderSection(materialObject, data, 3, 3, 2, 4)
  }

  def draw() = {
    renderSections map (_.draw)
  }
}
