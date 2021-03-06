package com.opticdev.opm.context

import com.opticdev.common.PackageRef
import com.opticdev.sdk.descriptions.PackageExportable

import scala.util.Try

case class PackageContext(leaf: Leaf) extends Context {

  def getPackageContext(packageId: String): Option[PackageContext] = leaf.tree.leafs.find(_.opticPackage.packageId == packageId).collect {
    case l: Leaf => PackageContext(l)
  }

  def getDependencyProperty(fullPath: String): Option[PackageExportable] = {
    val split = fullPath.split("/")
    if (split.size != 2) {
      None
    } else {
      val packageId = PackageRef.fromString(split.head).get.packageId
      val property = split.last

      val packageOption = getPackageContext(packageId)
      if (packageOption.isDefined) {
        val a = packageOption.get.getProperty(fullPath)
        val b = packageOption.get.getProperty(property)
        if (a.isDefined) a else if (b.isDefined) b else None
      } else None
    }
  }

  def getProperty(propertyKey: String) : Option[PackageExportable] = {
    val schemasOption = leaf.opticPackage.schemas.find(i=> i.schemaRef.full == propertyKey || i.schemaRef.id == propertyKey)
    val lensOption = leaf.opticPackage.lenses.find(_.id == propertyKey)
    val transformationsOption = leaf.opticPackage.transformations.find(_.id == propertyKey)

    if (schemasOption.isDefined) {
      schemasOption.asInstanceOf[Option[PackageExportable]]
    } else if (lensOption.isDefined) {
      lensOption.asInstanceOf[Option[PackageExportable]]
    } else if (transformationsOption.isDefined) {
      transformationsOption.asInstanceOf[Option[PackageExportable]]
    } else {
      None
    }

  }
}
