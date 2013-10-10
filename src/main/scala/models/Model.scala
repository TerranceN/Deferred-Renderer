package com.awesome.models

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.util.glu.GLU._
import org.lwjgl.BufferUtils
import org.lwjgl.input.Mouse
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

    //data.grouped(stride) foreach { list =>
    //  val position = "(" + list(0) + ", " + list(1) + ", " + list(2) + ")"
    //  val texCoord = "(" + list(6) + ", " + list(7) + ")"
    //  val color = "(" + list(8) + ", " + list(9) + ", " + list(10) + ", " + list(11) + ")"
    //  Console.println(texCoord)
    //}

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

      val uMousePosLocation = glGetUniformLocation(program.id, "uMousePos")

      val uDiffuseSamplerLocation = glGetUniformLocation(program.id, "uDiffuseSampler")
      val uDiffuseColorLocation = glGetUniformLocation(program.id, "uDiffuseColor")
      val uDiffuseTextureLocation = glGetUniformLocation(program.id, "uDiffuseTexture")

      val uSpecularSamplerLocation = glGetUniformLocation(program.id, "uSpecularSampler")
      val uSpecularColorLocation = glGetUniformLocation(program.id, "uSpecularColor")
      val uSpecularTextureLocation = glGetUniformLocation(program.id, "uSpecularTexture")
      val uShininessLocation = glGetUniformLocation(program.id, "uShininess")

      glUniform2f(uMousePosLocation, Mouse.getX, Mouse.getY)

      glEnableVertexAttribArray(aCoordLocation)
      glEnableVertexAttribArray(aNormalLocation)
      glEnableVertexAttribArray(aTexCoordLocation)
      glEnableVertexAttribArray(aColorLocation)

      mtl.diffuse match {
        case Right(t) => {
          t.bind
          glUniform1i(uDiffuseSamplerLocation, 0);
          glUniform1f(uDiffuseTextureLocation, 1.0f);
        }
        case Left(color) => {
          glDisable(GL_TEXTURE_2D);
          glUniform1f(uDiffuseTextureLocation, 0.0f);
          glUniform4f(uDiffuseColorLocation, color.x, color.y, color.z, color.w)
        }
      }

      mtl.specular match {
        case Right(t) => {
          t.bind
          glUniform1i(uSpecularSamplerLocation, 0);
          glUniform1f(uSpecularTextureLocation, 1.0f);
        }
        case Left(color) => {
          glDisable(GL_TEXTURE_2D);
          glUniform1f(uSpecularTextureLocation, 0.0f);
          glUniform4f(uSpecularColorLocation, color.x, color.y, color.z, color.w)
        }
      }

      glUniform1f(uShininessLocation, mtl.shininess)

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

  val imageLibrary = (file \\ "library_images")(0)

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

  def loadColorOrTexture(xml:Node):Either[Vector4, Texture] = {
    if ((xml \ "texture").length != 0) {
      val samplerName = ((xml \ "texture")(0) \ "@texture").text
      val sampler = ((file \\ "newparam") filter (x => (x \ "@sid").text == samplerName)) \ "sampler2D"
      val surfaceName = (sampler \ "source").text
      val surface = ((file \\ "newparam") filter (x => (x \ "@sid").text == surfaceName)) \ "surface"
      val imageName = (surface \ "init_from").text
      val image = ((imageLibrary \ "image") filter (x => (x \ "@id").text == imageName))
      val imageFile = (image \ "init_from").text
      return Right(Texture.fromImage(imageFile))
    } else if ((xml \ "color").length != 0) {
      val buf = (xml \ "color").text.split(" ") map (_.toFloat)
      return Left(new Vector4(buf(0), buf(1), buf(2), buf(3)))
    } else {
      return null;
    }
  }

  def loadPolylist(xml:Node) {
    val material = (file \\ "material" filter(m => (m \ "@id").text == (xml \ "@material").text))(0)
    val materialName = (material \ "@name").text

    val effect = (file \\ "effect" filter(m => (m \ "@id").text == (materialName + "-effect")))(0)
    val diffuse = (effect \\ "diffuse")(0)
    val specular = (effect \\ "specular")(0)

    var diffuseColor:Array[Float] = Array()

    var materialObject:Material = new Material(loadColorOrTexture(diffuse), loadColorOrTexture(specular), 50);

    val random = new Random(System.currentTimeMillis())
    if (diffuseColor.length == 0) {
      diffuseColor = Array(1.0f, 1.0f, 1.0f, 1.0f)
    }

    var data:Array[Float] = Array()

    var hasTexCoords = false;

    if (((xml \\ "input") map (_ \ "@semantic") filter (_.text == "TEXCOORD")).length == 1) {
      hasTexCoords = true;
    }

    (xml \ "p").text.split(" ").grouped(if (hasTexCoords) 3 else 2) foreach { list =>
      val indicies = list map (_.toInt)
      val newPositions = (positions.slice(indicies(0)*3, indicies(0)*3 + 3))
      val newNormals = (normals.slice(indicies(1)*3, indicies(1)*3 + 3))
      var newTexCoords:Array[Float] = Array();
      if (hasTexCoords) {
         newTexCoords = (texCoords.slice(indicies(2)*2, indicies(2)*2 + 2))
      }

      data = data ++ newPositions ++ newNormals ++ newTexCoords ++ diffuseColor
    }

    renderSections = renderSections :+ new RenderSection(materialObject, data, 3, 3, if (hasTexCoords) 2 else 0, 4)
  }

  def draw() = {
    renderSections map (_.draw)
  }
}
