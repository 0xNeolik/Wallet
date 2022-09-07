package com.clluc.stockmind.core.user

import java.util.UUID

import io.circe.{Json, JsonObject}

case class LocalDirectoryEntry(userId: UUID, id: LocalDirectoryEntryId, data: LocalDirectoryData)

case class LocalDirectoryEntryId(directoryProvider: String, directoryKey: String)

case class LocalDirectoryData(data: Json)

object LocalDirectoryEntry {

  def apply(userId: UUID, id: LocalDirectoryEntryId): LocalDirectoryEntry =
    LocalDirectoryEntry(userId, id, LocalDirectoryData(Json.fromJsonObject(JsonObject.empty)))

}

object LocalDirectoryData {

  def apply(): LocalDirectoryData = LocalDirectoryData(Json.fromJsonObject(JsonObject.empty))
}
