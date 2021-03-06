package com.opticdev.core.sourcegear.helpers

import com.opticdev.core.sourcegear.gears.helpers.{FlattenModelFields, ModelField}
import org.scalatest.FunSpec
import play.api.libs.json._

class FlattenModelFieldsSpec extends FunSpec {

  val flat = Set(
    ModelField(Seq("one"), JsString("value1")),
    ModelField(Seq("two"), JsBoolean(false)),
    ModelField(Seq("three"), JsNumber(3))
  )

  val nested = Set(
    ModelField(Seq("one"), JsString("value1")),
    ModelField(Seq("two"), JsString("value2")),
    ModelField(Seq("three", "one"), JsString("value3-1")),
    ModelField(Seq("three", "two", "one"), JsString("value3-2-1"))
  )

  it("will not change original object if no fields are added") {
    val expected = Json.parse("""
          {
            "one" : "value1",
            "two" : false,
            "three" : 3
          } """)

    assert(FlattenModelFields.flattenFields(Set(), expected.as[JsObject]) == expected)
  }

  it("Can create JsObject from flat fields") {

    val expected = Json.parse("""
          {
            "one" : "value1",
            "two" : false,
            "three" : 3
          } """)

    assert(FlattenModelFields.flattenFields(flat) == expected)
  }

  it("Can can merge onto an existing object") {
    val expected = Json.parse("""
          {
            "existingField" : true,
            "one" : "value1",
            "two" : false,
            "three" : 3
          } """)

    assert(FlattenModelFields.flattenFields(flat, JsObject(Seq("existingField"-> JsTrue))) == expected)
  }

  it("Can create JsObject from nested fields") {

    val expected = Json.parse("""
          {
            "one" : "value1",
            "two" : "value2",
            "three" : {
              "one": "value3-1",
              "two" : {
                "one": "value3-2-1"
              }
            }
          } """)

    assert(FlattenModelFields.flattenFields(nested) == expected)
  }

}
