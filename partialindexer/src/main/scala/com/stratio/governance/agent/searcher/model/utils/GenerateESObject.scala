package com.stratio.governance.agent.searcher.model.utils

import com.stratio.governance.agent.searcher.model.es.{CategoryES, GeneratedES, KeyValuePairES}
import com.stratio.governance.agent.searcher.model.{DatabaseSchema, DatastoreEngine, FileTable, KeyValuePair}

object GenerateESObject {

  def genParentId(generatedId: String): String = {
    val genIdToList = generatedId.split("/")
    genIdToList.take(genIdToList.length - 2).mkString("/")
  }


  def genGeneratedId(datastoreEngine: DatastoreEngine): String = {
    s"/${datastoreEngine.`type`}/${datastoreEngine.id}"
  }

  def genGeneratedId(sType: String, id: Int): String = {
    s"/$sType/$id"
  }

  def genGenerated(datastoreEngine: DatastoreEngine): GeneratedES = {
    val generatedId = GenerateESObject.genGeneratedId(datastoreEngine)
    GeneratedES(DatastoreEngine.entity,
                datastoreEngine.`type`,
                generatedId,
                GenerateESObject.genParentId(generatedId),
                Seq(CategoryES.fromDatastoreEngineGeneratedId(generatedId)),
                KeyValuePairES.fromDatastoreEngine(datastoreEngine))
  }

  def genGenerated(databaseSchema: DatabaseSchema, keyValuePair: KeyValuePair): GeneratedES = {
    val generatedId = GenerateESObject.genGeneratedId("PostgreSQL", databaseSchema.id)
    GeneratedES(DatastoreEngine.entity,
      "PostgreSQL",
      generatedId,
      GenerateESObject.genParentId(generatedId),
      Seq(CategoryES.fromDatastoreEngineGeneratedId(generatedId)),
      KeyValuePairES.fromKeyValuePair(keyValuePair))
  }

  def genGenerated(databaseSchema: DatabaseSchema, databaseSchemaList: List[(DatabaseSchema, KeyValuePair)])
  : GeneratedES = {
    val generatedId = GenerateESObject.genGeneratedId("PostgreSQL", databaseSchema.id)
    GeneratedES(DatastoreEngine.entity,
      "PostgreSQL",
      generatedId,
      GenerateESObject.genParentId(generatedId),
      Seq(CategoryES.fromDatastoreEngineGeneratedId(generatedId)),
      KeyValuePairES.fromDatabaseSchemaList(databaseSchemaList))
  }

  def genGenerated(fileTable: FileTable, fileTableList: List[(FileTable, KeyValuePair)])
  : GeneratedES = {
    val generatedId = GenerateESObject.genGeneratedId("PostgreSQL", fileTable.id)
    GeneratedES(DatastoreEngine.entity,
      "PostgreSQL",
      generatedId,
      GenerateESObject.genParentId(generatedId),
      Seq(CategoryES.fromFileTableGeneratedId(generatedId)),
      KeyValuePairES.fromFileTableList(fileTableList))
  }

}
