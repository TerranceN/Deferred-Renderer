package com.awesome

import scala.math._

import models._
import vectors._

class SceneGraphNode(var children:Either[List[GameObject], List[SceneGraphNode]], var positionBounds:BoundingBox) {
  def this(objects:List[GameObject], positionBounds:BoundingBox) = this(Left(objects), positionBounds)
  def isLeaf():Boolean = children match { case Right(_) => false case Left(_) => true }
  val bounds:BoundingBox = children match {
    case Right(nodes) => (nodes map (_.bounds) fold positionBounds) ((x, y) => x.combine(y))
    case Left(objects) => if (objects.isEmpty) positionBounds
                            else (objects map (_.getBoundingBox) fold positionBounds) ((x, y) => x.combine(y))
  }
  def draw():Int = {
    return children match {
      case Right(nodes) => nodes.foldLeft(0)((x, y) => y.draw() + x)
      case Left(objects) => objects.foldLeft(0)((x, y) => { y.draw(); 1 + x } )
    }
  }
  def addObject(anObject:GameObject) {
    children match {
      case Right(nodes) => { /* do nothing since this shouldn't happen */ }
      case Left(objects) => {
        children = Left(objects :+ anObject)
      }
    }
  }
}

class SceneGraph(var root:SceneGraphNode, val smallestNodeSize:Float) {
  def this(smallestNodeSize:Float) = this(null, smallestNodeSize)
  def this(root:SceneGraphNode) = this(root, 0) // using this constructor means that the tree should never be empty

  // eventually this will be sent a camera and view-frustum culling will be done
  // for now just draw everything
  def draw() {
    // draw everything
    root.draw()
  }

  def addObject(theObject:GameObject) {
    if (root == null) {
      val lower = theObject.position - new Vector3(smallestNodeSize) * 0.5f;
      val upper = lower + new Vector3(smallestNodeSize)
      root = new SceneGraphNode(List(theObject), new BoundingBox(lower, upper))
    } else {
      if (root.positionBounds.contains(theObject.position)) {
        var currentNode:SceneGraphNode = root
        var objectPlaced = false

        while (!objectPlaced) {
          currentNode.children match {
            case Left(objects) => {
              currentNode.addObject(theObject)
              objectPlaced = true
            } case Right(nodes) => {
              val nextNode = nodes find { node => node.positionBounds.contains(theObject.position) }

              nextNode match {
                case None => {
                  // this should never happen
                  // print error message and exit loop
                  Console.println("ERROR: Error adding object to scene graph")
                  objectPlaced = true
                }
                case Some(node) => {
                  currentNode = node
                }
              }
            }
          }
        }
      } else {
        // Add new root node and subnodes to include anObject's position
      }
    }
  }
}
