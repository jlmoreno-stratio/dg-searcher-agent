package com.stratio.governance.agent.searcher.model

case class DatastoreEngine(id: Int,
                           name: String,
                           `type`: String,
                           credentials: Option[String],
                           agent_version: String,
                           server_version: String,
                           operation_command_type: String,
                           created_at: String,
                           updated_at: String,
                           modification_time: Option[Long],
                           access_time: Option[Long],
                           metadata_path: String) extends EntityRow

object DatastoreEngine{
  val entity = "datastore"
}
