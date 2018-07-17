package com.stratio.governance.agent.searcher.model.es

import com.stratio.governance.agent.searcher.model.{DatabaseSchema, DatastoreEngine, FileTable, KeyValuePair}

import scala.util.Try

case class KeyValuePairES(gdp: Boolean,
                          quality: Float,
                          owner: String)

object KeyValuePairES {

  val GDP = "gdp"
  val QUALITY = "quality"
  val OWNER = "owner"

  def fromDatabaseSchemaList(databaseSchemaList: List[(DatabaseSchema, KeyValuePair)])
  : KeyValuePairES = {

    databaseSchemaList.head._2 match {
      case null => null
      case _ =>     KeyValuePairES(
        Try(databaseSchemaList.filter(_._2.key == GDP).head._2.value.toBoolean).getOrElse(false),
        Try(databaseSchemaList.filter(_._2.key == QUALITY).head._2.value.toFloat).getOrElse(0),
        Try(databaseSchemaList.filter(_._2.key == OWNER).head._2.value.toString).getOrElse("")
      )

    }
  }

  def fromFileTableList(fileTable: List[(FileTable, KeyValuePair)])
  : KeyValuePairES = {

    fileTable.head._2 match {
      case null => null
      case _ =>     KeyValuePairES(
        Try(fileTable.filter(_._2.key == GDP).head._2.value.toBoolean).getOrElse(false),
        Try(fileTable.filter(_._2.key == QUALITY).head._2.value.toFloat).getOrElse(0),
        Try(fileTable.filter(_._2.key == OWNER).head._2.value.toString).getOrElse("")
      )

    }
  }


  def fromKeyValuePair(keyValuePair: KeyValuePair): KeyValuePairES = {
    KeyValuePairES(true, "1.0".toFloat, "owner")
  }


  def fromDatastoreEngine(datastoreEngine: DatastoreEngine) = {
    //TODO these values must be recovered from database
    KeyValuePairES(true, "1.0".toFloat, "owner")
  }
}
