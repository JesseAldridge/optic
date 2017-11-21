package com.opticdev.core.sourcegear

import better.files.File
import com.opticdev.core.sourcegear.project.config.ProjectFile
import com.opticdev.sdk.descriptions.{Schema, SchemaColdStorage}
import org.scalatest.FunSpec
import play.api.libs.json.{JsObject, Json}
import boopickle.Default._
import com.opticdev.core.Fixture.TestBase
import com.opticdev.core.sourcegear.gears.RuleProvider
import com.opticdev.core.sourcegear.gears.generating.GenerateGear
import com.opticdev.core.sourcegear.gears.parsing.ParseAsModel
import com.opticdev.core.sourcegear.serialization.PickleImplicits._
import com.opticdev.core.sourcegear.storage.GearStorage
import com.opticdev.opm.TestPackageProviders
import com.opticdev.parsers.graph.AstType
import scala.concurrent.duration._
import scala.concurrent.Await
class SGConfigSpec extends TestBase with TestPackageProviders {

  describe("SG Config") {

    lazy val sgConfig = {
      val future = SGConstructor.fromProjectFile(new ProjectFile(File("test-examples/resources/example_packages/express/optic.yaml")))
      Await.result(future, 5 seconds)
    }

    it("can be pickled") {

      val picked = Pickle.intoBytes(sgConfig)
      val unpickled = Unpickle[SGConfig].fromBytes(picked)

      assert(unpickled == sgConfig)

    }

    it("can generate a hexadecimal from hash") {
      assert(sgConfig.hashString == "c315b225")
    }

    it("can inflate to a sourcegear instance") {
      val sourceGear = sgConfig.inflate
      assert(sourceGear.gearSet.size == sgConfig.gears.size)
      assert(sourceGear.schemas.size == sgConfig.schemas.size)
      assert(sourceGear.parsers.size == sgConfig.parserIds.size)
    }

  }

}
