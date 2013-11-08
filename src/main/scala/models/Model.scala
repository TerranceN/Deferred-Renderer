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
import scala.math._
import scala.util.matching.Regex

import com.awesome.BoundingBox
import com.awesome.shaders._
import com.awesome.textures._
import com.awesome.vectors._

class RenderSection(
val mtl:Material,
val data:Array[Float],
val positionDataSize:Int,
val normalDataSize:Int,
val texCoordDataSize:Int) {
  var dataBufferId = 0

  val stride = positionDataSize + normalDataSize + texCoordDataSize
  val vertexDataSize = 4 * stride
  val normalOffset = 4 * positionDataSize
  val texCoordOffset = normalOffset + 4 * normalDataSize
  val numVerticies = data.length / stride

  //data.grouped(stride) foreach { list =>
  //  val position = "(" + list(0) + ", " + list(1) + ", " + list(2) + ")"
  //  val texCoord = "(" + list(6) + ", " + list(7) + ")"
  //  val color = "(" + list(8) + ", " + list(9) + ", " + list(10) + ", " + list(11) + ")"
  //  Console.println(texCoord)
  //}

  def genBuffers() {
    if (!data.isEmpty) {
      dataBufferId = glGenBuffers
      val dataBuffer = BufferUtils.createFloatBuffer(data.length)
      dataBuffer.put(data)
      dataBuffer.flip
      glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)
      glBufferData(GL_ARRAY_BUFFER, dataBuffer, GL_STATIC_DRAW)
    }
  }

  def checkError() {
    val error = glGetError
    if (error != GL_NO_ERROR) {
      Console.println("OpenGL Error: " + error)
      Console.println(gluErrorString(error))
    }
  }

  def draw() = {
    if (!data.isEmpty) {
      val program = ShaderProgram.getActiveShader

      val aCoordLocation = glGetAttribLocation(program.id, "aCoord")
      val aNormalLocation = glGetAttribLocation(program.id, "aNormal")
      val aTexCoordLocation = glGetAttribLocation(program.id, "aTexCoord")

      glEnableVertexAttribArray(aCoordLocation)
      glEnableVertexAttribArray(aNormalLocation)
      glEnableVertexAttribArray(aTexCoordLocation)

      mtl.bind()

      glBindBuffer(GL_ARRAY_BUFFER, dataBufferId)

      glVertexAttribPointer(aCoordLocation, 3, GL_FLOAT, false, vertexDataSize, 0)
      glVertexAttribPointer(aNormalLocation, 3, GL_FLOAT, false, vertexDataSize, normalOffset)
      glVertexAttribPointer(aTexCoordLocation, 2, GL_FLOAT, false, vertexDataSize, texCoordOffset)

      glDrawArrays(GL_TRIANGLES, 0, numVerticies)

      glDisableVertexAttribArray(aCoordLocation)
      glDisableVertexAttribArray(aNormalLocation)
      glDisableVertexAttribArray(aTexCoordLocation)
    }
  }
}

class Model(var renderSections:List[RenderSection]) {
  def getBoundingBox():BoundingBox = {
    var lower:Vector3 = null
    var upper:Vector3 = null

    renderSections map { s =>
      s.data.grouped(s.stride) foreach { lst =>
        val x = lst(0)
        val y = lst(0)
        val z = lst(0)

        if (lower != null) {
          lower.x = min(lower.x, x)
          lower.y = min(lower.y, y)
          lower.z = min(lower.z, z)
        } else {
          lower = new Vector3(x, y, z)
        }

        if (upper != null) {
          upper.x = max(upper.x, x)
          upper.y = max(upper.y, y)
          upper.z = max(upper.z, z)
        } else {
          upper = new Vector3(x, y, z)
        }
      }
    }

    if (lower == null || upper == null) {
      lower = new Vector3(0)
      upper = new Vector3(0)
    }

    return new BoundingBox(lower, upper)
  }

  def draw() = {
    renderSections map (_.draw)
  }

  def genBuffers() {
    renderSections map (_.genBuffers())
  }
}

object Model {
  def fromFile(fileName:String):Model = {
    var filePathPattern = "(.*/)[^/]*".r
    var fileDirectory =  (filePathPattern findFirstMatchIn fileName) match {
      case Some(m) => m.group(1)
      case None => ""
    }
    var renderSections:List[RenderSection] = List()
    // load from file
    var file = XML.loadFile(new File(fileName))
    // load into an array of RenderSections

    val imageLibrary = (file \\ "library_images")(0)

    var positions:Array[Float] = Array()
    var texCoords:Array[Float] = Array()
    var normals:Array[Float] = Array()

    val geometry = (file \\ "geometry")(0)
    val geometryName:String = (geometry \ "@id").text

    val meshId = geometryName + "-positions-array"
    val normalsId = geometryName + "-normals-array"
    val mapId = geometryName + "-map-0-array"

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
        return Right(Texture.fromImage(fileDirectory + imageFile))
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

    return new Model(renderSections)
  }
}
