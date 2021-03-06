package com.opticdev.core.trainer

import akka.actor.ActorSystem
import akka.stream.scaladsl.JavaFlowSupport.Source
import better.files.File
import com.opticdev.common.storage.DataDirectory
import com.opticdev.core.sourcegear.accumulate.FileAccumulator
import com.opticdev.core.sourcegear.{SGConstructor, SGContext, SourceGear}
import com.opticdev.core.sourcegear.actors.ActorCluster
import com.opticdev.core.sourcegear.graph.model.{FlatModelNode, ModelNode, MultiModelNode}
import com.opticdev.core.sourcegear.project.StaticSGProject
import com.opticdev.core.sourcegear.project.config.ProjectFile
import com.opticdev.opm.PackageManager
import com.opticdev.opm.context.{Leaf, Tree}
import com.opticdev.opm.packages.{OpticMDPackage, OpticPackage}
import com.opticdev.opm.providers.ProjectKnowledgeSearchPaths
import com.opticdev.parsers.{AstGraph, ParserBase, SourceParserManager}
import com.opticdev.parsers.graph.CommonAstNode
import com.opticdev.sdk.markdown.MarkdownParser
import com.opticdev.sdk.markdown.MarkdownParser.MDParseOutput
import com.opticdev.sdk.opticmarkdown2.lens.OMLens
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object TestLens {

  implicit lazy val actorCluster = new ActorCluster(ActorSystem("trainer"))

  def testLens(lensConfiguration: JsObject, markdown: String, testInput: String, projectBaseDir: Option[String] = None) : Try[JsObject] = Try {
    val description = descriptionFromString(markdown)

    val lensId = (lensConfiguration \ "id").get.as[JsString]
    val language = (lensConfiguration \ "snippet" \ "language").get.as[JsString].value

    val output = MDParseOutput(description)
    val lensesSeq = MDParseOutput(description).lenses.value.filterNot(i=> (i.as[JsObject] \ "id").get == lensId) :+ lensConfiguration

    val descriptionIncludingLens = description + ("lenses" -> JsArray(lensesSeq))

    val testPackage = OpticMDPackage(descriptionIncludingLens, Map())
    val testPackageRef = testPackage.packageRef

    implicit val projectKnowledgeSearchPaths =
      projectBaseDir.map(i=> new ProjectFile(File(i) / "optic.yml", createIfDoesNotExist = false).projectKnowledgeSearchPaths).getOrElse(ProjectKnowledgeSearchPaths())
    val dependencyTree = PackageManager.collectPackages(testPackage.dependencies).getOrElse(Tree())

    val dependencyTreeResolved = Tree(Leaf(testPackage, dependencyTree))

    val sg = SGConstructor.fromDependencies(dependencyTreeResolved, SourceParserManager.installedParsers.map(_.parserRef)).map(_.inflate)
    sg.onComplete(i=> i.failed.foreach(_.printStackTrace()))
    val sgBuilt = Await.result(sg, 10 seconds)

    implicit val project = new StaticSGProject("trainer_project", DataDirectory.trainerScratch, sgBuilt)

    val parseResults = sgBuilt.parseString(testInput)(project, language).get

    val mn: FlatModelNode = parseResults.modelNodes.minBy {
      case mn: ModelNode => mn.asInstanceOf[ModelNode].resolveInGraph[CommonAstNode](parseResults.astGraph).root.graphDepth(parseResults.astGraph)
      case mmn: MultiModelNode => mmn.modelNodes.head.resolveInGraph[CommonAstNode](parseResults.astGraph).root.graphDepth(parseResults.astGraph)
    }

    implicit val sourceGearContext = SGContext(
      sgBuilt.fileAccumulator,
      parseResults.astGraph,
      parseResults.parser,
      testInput,
      sgBuilt,
      null
    )
    mn.expandedValue(true)
  }

  def descriptionFromString(inMarkdown: String): JsObject = {
    val desc = MarkdownParser.parseMarkdownString(inMarkdown).map(_.description).getOrElse(JsObject.empty)
    if ((desc \ "info").isEmpty) {
      desc ++ MarkdownParser.parseMarkdownString("""<!-- Package {"author": "optictest", "package": "optictest", "version": "0.0.0"} -->""").map(_.description).get
    } else {
      desc
    }
  }
}
