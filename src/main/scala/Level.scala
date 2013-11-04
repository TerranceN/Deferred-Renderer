package com.awesome

import scala.math._
import scala.collection.mutable.ArrayBuffer

import models._
import vectors._
import geometry._

class Level {
  def toSceneGraph() {}
}

class LevelPiece(val model:Model) extends GameObject {
  var position = new Vector3(0)
  def draw() {
    model.draw()
  }
  def getBoundingBox():BoundingBox = {
    return model.getBoundingBox
  }
}

object Level {
  def fromModel(model:Model, minSize:Float):SceneGraphNode = {

    var triangeCount:Int = 0

    var bounds = model.getBoundingBox
    var boundsSize = bounds.size
    var maxSize = max(boundsSize.x, max(boundsSize.y, boundsSize.z))
    var octreeLevels = ceil(log(maxSize / minSize) / log(2)).toInt
    var boundsMaxSize = pow(2, octreeLevels).toFloat * minSize
    var cubicBounds = new BoundingBox(bounds.lower, bounds.lower + new Vector3(boundsMaxSize))

    var cellSize:Double = maxSize / (pow(2, octreeLevels))

    def splitOnPlane(part:RenderSection, plane:Plane):ArrayBuffer[Option[RenderSection]] = {
      val buffer:ArrayBuffer[Option[RenderSection]] = new ArrayBuffer(2)
      val data:ArrayBuffer[Array[Float]] = new ArrayBuffer(2)
      for (i <- 0 until 2) {
        buffer += None
        data += Array()
      }

      //Console.println("Section data is " + part.data.length + " floats long. Therefore it should be 0 mod 3: " + part.data.length % 3)
      //Console.println("Stride is: " + part.stride + ", NumVerticies is: " + part.numVerticies)
      //Console.println("Plane normal is: " + plane.normal.x + ", " + plane.normal.y + ", " + plane.normal.z)

      // for each triangle
      part.data.grouped(part.stride * 3) foreach { triangleData =>
        var vertexList:List[(Vector3, Boolean, Array[Float])] = List()
        var vert = triangleData.grouped(part.stride).toList
        var verts = (vert zip vert.indices map { case (lst, index) => (new Vector3(lst(0), lst(1), lst(2)), index) }).toList
        var edges = (verts.sliding(2).toList map { case Seq(a, b) => (a, b) }).toList
        edges = (verts(verts.length - 1), verts(0)) :: edges
        var intersections:List[(Vector3, Array[Float])] = List()
        // for each edge
        edges foreach { case ((p1, i1), (p2, i2)) =>
          val p1Data = triangleData.slice(i1 * part.stride, i1 * part.stride + part.stride)
          val p2Data = triangleData.slice(i2 * part.stride, i2 * part.stride + part.stride)
          val p1Side = plane.whichSide(p1)
          val p2Side = plane.whichSide(p2)
          //Console.println(p1.x + ", " + p1.y + ", " + p1.z)
          //Console.println(p2.x + ", " + p2.y + ", " + p2.z)
          vertexList = (p1, p1Side == 0, p1Data) :: vertexList
          // check for intersections
          // only include intersections when neither point is on the plane
          if (p1Side != 0 && p2Side != 0) {
            new LineSegment(p1, p2).intersection(plane) match {
              case Some(i) => {
                // interpolate data if intersections are found
                val t =  (i - p1).length / (p2 - p1).length
                val p1Norm = new Vector3(p1Data(3), p1Data(4), p1Data(5))
                val p1TexCoord = new Vector2(p1Data(6), p1Data(7))
                val p2Norm = new Vector3(p2Data(3), p2Data(4), p2Data(5))
                val p2TexCoord = new Vector2(p2Data(6), p2Data(7))
                val iNorm = p1Norm.lerp(p2Norm, t)
                val iTexCoord = p1TexCoord.lerp(p2TexCoord, t)
                //Console.println("t = " + t)
                //Console.println("p1Tex = " + p1TexCoord.x + ", " + p1TexCoord.y)
                //Console.println("p2Tex = " + p2TexCoord.x + ", " + p2TexCoord.y)
                //Console.println("iTex = " + iTexCoord.x + ", " + iTexCoord.y)
                val intersectionData:Array[Float] = Array(i.x, i.y, i.z, iNorm.x, iNorm.y, iNorm.z, iTexCoord.x, iTexCoord.y)
                //Console.println("Intersection: " + i.x + ", " + i.y + ", " + i.z)
                vertexList = (i, true, intersectionData) :: vertexList
                intersections = (i, intersectionData) :: intersections
              }
              case _ => {}
            }
          }
        }

        vertexList = vertexList.toList.reverse

        // TODO: recreate geometry using vertexList and vertexData
        var numIntersections = (vertexList.length - 3)
        //Console.println(numIntersections + " intersections")

        if (numIntersections > 0) {
          if (numIntersections == 2) {
            triangeCount += 2
          } else {
            triangeCount += 1
          }
          // first side
          var numDataAdded = 0
          var resultIndex:Int = 0
          for (i <- 0 until 2) {
            var foundIntersection = i match {
              case 0 => false
              case 1 => true
            }
            var lastDataAdded:Array[Float] = null
            var numVertsAdded = 0
            var firstVertexData:Array[Float] = null
            var dataBuffer:Array[Float] = Array()

            //Console.println("indicies: ")

            for ((point, isIntersection, vertexData) <- vertexList) {
              if (!foundIntersection || isIntersection) {
                if (!isIntersection) {
                  var side = plane.whichSide(point)
                  resultIndex = if (side == -1) 0 else 1
                }
                if (numVertsAdded > 2 && numVertsAdded % 3 == 0 && lastDataAdded != null) {
                  dataBuffer = dataBuffer ++ lastDataAdded
                  numDataAdded += lastDataAdded.length
                  numVertsAdded += 1
                }
                dataBuffer = dataBuffer ++ vertexData
                lastDataAdded = vertexData
                numDataAdded += vertexData.length
                numVertsAdded += 1
                if (firstVertexData == null) {
                  firstVertexData = vertexData
                }
              }
              if (isIntersection) {
                foundIntersection = !foundIntersection
              }
            }
            if (numVertsAdded % 3 == 2) {
              dataBuffer = dataBuffer ++ firstVertexData
              numDataAdded += firstVertexData.length
            }

            data(resultIndex) = data(resultIndex) ++ dataBuffer
          }
          //Console.println("Due to intersections, replaced triange data with " + numDataAdded + " floats")
        } else {
          triangeCount += 1
          var resultIndex:Int = 0
          for ((point, isIntersection, vertexData) <- vertexList) {
            if (!isIntersection) {
              var side = plane.whichSide(point)
              resultIndex = if (side == -1) 0 else 1
            }
          }
          data(resultIndex) = data(resultIndex) ++ triangleData
        }
      }

      for (i <- 0 until 2) {
        if (!data(i).isEmpty) {
          buffer(i) = Some(new RenderSection(
            part.mtl,
            data(i),
            part.positionDataSize,
            part.normalDataSize,
            part.texCoordDataSize
          ))
        }
      }

      return buffer
    }

    def splitFlatMapFunction(plane:Plane)(elem:Option[RenderSection]):ArrayBuffer[Option[RenderSection]] = elem match {
      case Some(piece) => splitOnPlane(piece, plane)
      case None => {
        var buf:ArrayBuffer[Option[RenderSection]] = new ArrayBuffer(2)
        buf += None
        buf += None
        return buf
      }
    }

    def splitIntoEight(
      part:RenderSection,
      middle:Vector3):ArrayBuffer[Option[RenderSection]] = {
        var xPlane = new Plane(new Vector3(1, 0, 0), middle)
        var yPlane = new Plane(new Vector3(0, 1, 0), middle)
        var zPlane = new Plane(new Vector3(0, 0, 1), middle)

        val sections = splitOnPlane(part, zPlane) flatMap splitFlatMapFunction(yPlane) flatMap splitFlatMapFunction(xPlane)

        return sections
    }

    def generateOctree(m:Model, octreeLevel:Int, lower:Vector3):SceneGraphNode = {
      var sectionSize = (pow(2, octreeLevel)).toFloat * minSize

      if (octreeLevel <= 0) {
        // make node with models
        m.genBuffers
        return new SceneGraphNode(
          List(new LevelPiece(m)),
          new BoundingBox(lower, lower + new Vector3(sectionSize)))
      } else {
        var middle = lower + new Vector3(sectionSize / 2)

        // make node with sub nodes
        var nodes:List[SceneGraphNode] = List()

        // list of render sections for each model
        var sections:ArrayBuffer[List[RenderSection]] = new ArrayBuffer(8)
        for (i <- 0 until 8) {
          sections += List()
        }
        for (part <- m.renderSections) {
          // split triangles that need to be split
          var parts = splitIntoEight(part, middle)
          for (i <- 0 until 8) {
            parts(i) match {
              case Some(section) => {
                sections(i) = section :: sections(i)
              }
              case _ => {}
            }
          }
        }

        var subSectionSize = sectionSize / 2
        for (i <- 0 until 8) {
          var subIndexes = new Vector3(i % 2, i / 2 % 2, i / 4 % 2)
          var bounds = new BoundingBox(
            lower + subIndexes * subSectionSize,
            lower + subIndexes * subSectionSize + new Vector3(subSectionSize))
          // TODO: Change this to a recursive call until sectionSize is small enough
          if (!sections(i).isEmpty) {
            val m = new Model(sections(i))

            nodes = generateOctree(m, octreeLevel - 1, bounds.lower) :: nodes
          } else {
            Console.println("Empty Section!")
            nodes = new SceneGraphNode(Right(List():List[SceneGraphNode]), bounds) :: nodes
          }
        }

        return new SceneGraphNode(Right(nodes), new BoundingBox(lower, lower + new Vector3(sectionSize)))
      }
    }

    val root:SceneGraphNode = generateOctree(model, octreeLevels, cubicBounds.lower)

    return root
  }
}
