package com.opticdev.core.sourcegear.graph

import com.opticdev.core.sourcegear.graph.edges.YieldsModel
import com.opticdev.core.sourcegear.graph.enums.AstPropertyRelationship
import com.opticdev.parsers.graph.CommonAstNode
import com.opticdev.sdk.opticmarkdown2.lens.{Literal, ObjectLiteral, Token, ArrayLiteral}
import com.opticdev.sdk.opticmarkdown2.lens.OMLensComponent

package object model {
  type ModelAstMapping = Map[ModelKey, Set[AstMapping]]

  type ModelAstPair = (YieldsModel, CommonAstNode)

  sealed trait ModelKey

  case class Path(path: Seq[String]) extends ModelKey {
    override def toString: String = path.mkString(".")
  }

  object Path {
    def fromString(string: String) = Path(string.split("\\.").toSeq)
  }

  sealed trait AstMapping {
    val relationship : AstPropertyRelationship.Value
    def supportsComponentMapping(component: OMLensComponent) = {
      component.`type` match {
        case Token if relationship == AstPropertyRelationship.Token => true
        case Literal if relationship == AstPropertyRelationship.Literal => true
        case ObjectLiteral if relationship == AstPropertyRelationship.ObjectLiteral => true
        case ArrayLiteral if relationship == AstPropertyRelationship.ArrayLiteral => true
        case _ => false
      }
    }
  }
  case class NodeMapping(node: CommonAstNode, relationship : AstPropertyRelationship.Value) extends AstMapping
  case class ModelVectorMapping(models: Vector[ModelNode]) extends AstMapping {override val relationship = AstPropertyRelationship.Model}
  case class ContainerMapping(containerRoot: CommonAstNode) extends AstMapping {override val relationship = AstPropertyRelationship.NoRelationship}
  case object NoMapping extends AstMapping {override val relationship = AstPropertyRelationship.NoRelationship}
}
