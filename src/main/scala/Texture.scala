package com.awesome

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.BufferUtils

import java.io.File
import java.io.IOException
import java.awt.image._
import javax.imageio.ImageIO

class Texture(image:BufferedImage) {
  val id = glGenTextures
  glActiveTexture(GL_TEXTURE0)
  glBindTexture(GL_TEXTURE_2D, id)

  val inputType:Int = image.getColorModel.getNumComponents match {
    case 4 => GL_RGBA
    case 3 => GL_RGB
    case _ => -1
  }

  val pixels = (image.getRaster.getDataBuffer).asInstanceOf[DataBufferByte].getData
  val buf = BufferUtils.createByteBuffer(pixels.length)
  buf.put(pixels)
  buf.flip

  glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
  glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, image.getWidth, image.getHeight, 0, inputType, GL_UNSIGNED_BYTE, buf)
}

object Texture {
  def fromImage(fileName:String):Texture = {
    try {
      val image = ImageIO.read(new File(fileName))
      return new Texture(image)
    } catch {
      case e:IllegalArgumentException => {
        Console.println("Warning: null passed to Texture.fromImage")
      }
      case e:IOException => {
        Console.println("Error: Failed to load texture: " + fileName)
      }
    }

    return null
  }
}
