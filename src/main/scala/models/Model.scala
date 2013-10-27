package com.awesome.models

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
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
  val texCoordOffset:Int) {
    val dataBufferId = glGenBuffers
    val dataBuffer = BufferUtils.createFloatBuffer(data.length)
    dataBuffer.put(data)
    dataBuffer.flip
    glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)
    glBufferData(GL_ARRAY_BUFFER, dataBuffer, GL_STATIC_DRAW)

    val stride = vertexOffset + normalOffset + texCoordOffset
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

      glEnableVertexAttribArray(aCoordLocation)
      glEnableVertexAttribArray(aNormalLocation)
      glEnableVertexAttribArray(aTexCoordLocation)

      glActiveTexture(GL_TEXTURE1)
      glBindTexture(GL_TEXTURE_2D, 0)
      mtl.diffuse match {
        case Right(t) => {
          t.bind
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
      mtl.specular match {
        case Right(t) => {
          t.bind
          program.setUniform1i("uSpecularSampler", 2)
          program.setUniform1f("uSpecularTexture", 1.0f)
        }
        case Left(color) => {
          glDisable(GL_TEXTURE_2D);
          program.setUniform4f("uSpecularColor", color.x, color.y, color.z, color.w)
          program.setUniform1f("uSpecularTexture", 0.0f)
        }
      }

      program.setUniform1f("uShininess", mtl.shininess)

      glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)

      glVertexAttribPointer(aCoordLocation, 3, GL_FLOAT, false, stride*4, 0)
      glVertexAttribPointer(aNormalLocation, 3, GL_FLOAT, false, stride*4, 3*4)
      glVertexAttribPointer(aTexCoordLocation, 2, GL_FLOAT, false, stride*4, 6*4)

      glDrawArrays(GL_TRIANGLES, 0, numVerticies)

      glDisableVertexAttribArray(aCoordLocation)
      glDisableVertexAttribArray(aNormalLocation)
      glDisableVertexAttribArray(aTexCoordLocation)
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
    val diffuse = (effect \\ "diffuse")
    val specular = (effect \\ "specular")
    val shininess = (effect \\ "shininess")

    var diffuseObject:Either[Vector4, Texture] = Left(new Vector4(1.0f, 1.0f, 1.0f, 1.0f))
    var specularObject:Either[Vector4, Texture] = Left(new Vector4(0.0f, 0.0f, 0.0f, 0.0f))
    var shininessObject = 0f

    if (diffuse.length > 0) {
      diffuseObject = loadColorOrTexture(diffuse(0))
    }

    if (specular.length > 0 && shininess.length > 0) {
      specularObject = loadColorOrTexture(specular(0))
      shininessObject = (shininess \ "float").text.toFloat
    }

    var materialObject:Material = new Material(diffuseObject, specularObject, shininessObject);

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

      data = data ++ newPositions ++ newNormals ++ newTexCoords
    }

    renderSections = renderSections :+ new RenderSection(materialObject, data, 3, 3, if (hasTexCoords) 2 else 0)
  }

  def draw() = {
    renderSections map (_.draw)
  }
}
