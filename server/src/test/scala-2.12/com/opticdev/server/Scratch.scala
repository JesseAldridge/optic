package com.opticdev.server

import better.files.File
import com.opticdev.core.Fixture.TestBase
import com.opticdev.core.sourcegear.project.Project
import com.opticdev.core.sourcegear.project.config.ProjectFile
import com.opticdev.opm.{TestPackageProviders, TestProvider}
import com.opticdev.parsers.SourceParserManager
import com.opticdev.server.http.Server
import com.opticdev.server.state.ProjectsManager
import net.jcazevedo.moultingyaml.YamlString
import net.jcazevedo.moultingyaml._

import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
object Scratch extends TestBase with TestPackageProviders {

  installProviders
  super.beforeAll()

  val parserPath = Try({
    val contents = File("config.yml").contentAsString
    contents.parseYaml.asYamlObject.fields(YamlString("testParser")).asInstanceOf[YamlString].value
  }).getOrElse(throw new Error("No testParser found in config.yml"))


  SourceParserManager.installParser("parserPath")

  implicit val projectsManager: ProjectsManager = new ProjectsManager()
  implicit val actorCluster = projectsManager.actorCluster


  com.opticdev.core.actorSystem.scheduler.scheduleOnce(5 seconds) {
    //manually adding projects for testing
    val project = Project.fromProjectFile(new ProjectFile(File("test-examples/resources/tmp/test_project/optic.yml"))).get
    projectsManager.loadProject(project)
  }

  def main(args: Array[String]): Unit = {
    Server.start()
  }

}
