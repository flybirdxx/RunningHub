package com.runninghub.shared.db

import kotlin.Long
import kotlin.String

public data class CategoryEntity(
  public val id: String,
  public val name: String,
  public val parentId: String?,
  public val sortOrder: Long,
)
