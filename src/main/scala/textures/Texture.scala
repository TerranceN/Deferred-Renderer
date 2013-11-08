package com.awesome.textures

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL21._
import org.lwjgl.opengl.GL30._
import org.lwjgl.BufferUtils

import java.io.File
import java.io.IOException
import java.awt.image._
import javax.imageio.ImageIO

class Texture(image:BufferedImage, deGamma:Boolean) {
  def this(image:BufferedImage) = this(image, false)

  glEnable(GL_TEXTURE_2D)

  val id = glGenTextures
  glActiveTexture(GL_TEXTURE0)
  glBindTexture(GL_TEXTURE_2D, id)

  val numComponents = image.getColorModel.getNumComponents

  val inputType:Int = numComponents match {
    case 4 => GL_BGRA
    case 3 => GL_BGR
    case 2 => GL_RG
    case 1 => GL_RED
    case _ => -1
  }

  val internalType = deGamma match {
    case true => GL_SRGB
    case false => GL_RGB
  }

  val pixels = (image.getRaster.getDataBuffer).asInstanceOf[DataBufferByte].getData
  Console.println("inputType: " + inputType + ", pixel bytes: " + pixels.length)
  val buf = BufferUtils.createByteBuffer(pixels.length)
  buf.put(pixels)
  buf.flip

  glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
  glTexImage2D(GL_TEXTURE_2D, 0, internalType, image.getWidth, image.getHeight, 0, inputType, GL_UNSIGNED_BYTE, buf)

  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR); // Linear Filtering
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR); // Linear Filtering

  glDisable(GL_TEXTURE_2D)

  def bind() = {
    glEnable(GL_TEXTURE_2D)
    glBindTexture(GL_TEXTURE_2D, id)
  }

  def unbind() = {
    glDisable(GL_TEXTURE_2D)
  }
}

object Texture {
  def fromImage(fileName:String):Texture = fromImage(fileName, false)
  def fromImage(fileName:String, deGamma:Boolean):Texture = {
    Console.println("Loading texture: " + fileName)
    Console.println("deGamma: " + deGamma)
    try {
      val image = ImageIO.read(new File(fileName))
      Console.println("image size: " + image.getWidth + ", " + image.getHeight)
      return new Texture(image, deGamma)
    } catch {
      case e:IllegalArgumentException => {
        Console.println("Warning: null passed to Texture.fromImage")
        e.printStackTrace
      }
      case e:Exception => {
        Console.println("Error: Failed to load texture: " + fileName)
      }
    }

    return null
  }
}
