package com.stratio.governance.agent.searcher.model.es

case class CategoryES(id: String,
                      name: String)

object CategoryES{

  def fromDatastoreEngineGeneratedId(generatedID: String): CategoryES = {
    CategoryES(generatedID.split("/").take(1).mkString("/"),
               generatedID.split("/").take(1).mkString("/"))

  }

  def fromFileTableGeneratedId(generatedID: String): CategoryES = {
    CategoryES(generatedID.split("/").take(1).mkString("/"),
      generatedID.split("/").take(1).mkString("/"))

  }


}