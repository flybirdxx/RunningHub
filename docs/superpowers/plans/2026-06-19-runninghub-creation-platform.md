# RunningHub Creation Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the RunningHub creation platform from the approved PRD: unified dark/lime theme, dynamic API model catalog and invocation foundation, first-class generation history, read-only plaza, and integrated navigation.

**Architecture:** Keep new business logic in `shared` and new UI in `composeApp`, following the existing KMP/Ktor/Koin/Voyager patterns. Add small repository boundaries for model catalog, model invocation, plaza, and unified history while reusing existing `QuickCreateRepository` behavior instead of rewriting it.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor Client, kotlinx.serialization, Koin, Voyager ScreenModel, kotlin.test, Gradle.

---

## Source Documents

- PRD: `doc/PRD-RunningHub-Creation-Platform-2026-06-19.md`
- Design: `docs/superpowers/specs/2026-06-19-runninghub-creation-plaza-history-design.md`
- API model capture: `doc/RunningHub-call-api-models-capture-2026-06-18.md`
- Plaza capture: `doc/RunningHub-explore-capture-2026-06-19.md`
- Color capture: `doc/RunningHub-home-color-palette-capture-2026-06-19.md`

## Planned File Structure

### Shared Data And Domain

- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/ApiModel.kt`
  - Domain models for standard API models, LLM catalog entries, field schemas, field options, pricing, and endpoints.
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/ModelInvocation.kt`
  - Domain models for schema-driven request values and async task results.
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/Plaza.kt`
  - Domain models for plaza tags, inspiration cards, detail, comments, short-film categories, and short-film cards.
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/GenerationHistory.kt`
  - Source-labeled generation history item, output, status, and project summary models.
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/ModelCatalogRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/ModelInvocationRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/PlazaRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/GenerationHistoryRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/ApiModelDto.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/PlazaDto.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/ModelCatalogApi.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/PlazaApi.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ModelCatalogRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ModelInvocationRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/PlazaRepositoryImpl.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/GenerationHistoryRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`

### Shared Tests

- Create: `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/ApiModelDtoTest.kt`
- Create: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/ApiModelFieldMapperTest.kt`
- Create: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/ModelInvocationRequestBuilderTest.kt`
- Create: `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/PlazaDtoTest.kt`
- Create: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/GenerationHistoryMapperTest.kt`

### Compose App

- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Color.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/ExtendedColors.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/navigation/MainScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/di/AppModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreenModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/ApiModelFieldUi.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreenModel.kt`
- Replace or retire: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/community/CommunityScreen.kt`
- Replace or retire: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/community/CommunityScreenModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreenModel.kt`

### Compose Tests

- Create: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/create/ApiModelFieldUiModelTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/history/GenerationHistoryUiTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/plaza/PlazaUiStateTest.kt`

---

## Task 1: Theme Token Foundation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Color.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/ExtendedColors.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/theme/Theme.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/theme/RunningHubThemeTokenTest.kt`

- [ ] **Step 1: Write the theme token test**

Create `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/theme/RunningHubThemeTokenTest.kt`:

```kotlin
package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RunningHubThemeTokenTest {
    @Test
    fun `brand lime matches captured web palette`() {
        assertEquals(Color(0xFFCCFF00), BrandLime)
    }

    @Test
    fun `dark surfaces match captured web palette`() {
        assertEquals(Color(0xFF000000), BaseBlack)
        assertEquals(Color(0xFF080808), Surface900)
        assertEquals(Color(0xFF09090B), Surface850)
        assertEquals(Color(0xFF18181B), Surface800)
        assertEquals(Color(0xFF27272A), Surface700)
    }
}
```

- [ ] **Step 2: Run the focused test and confirm failure**

Run:

```powershell
gradle :composeApp:allTests --tests "com.runninghub.app.ui.theme.RunningHubThemeTokenTest"
```

Expected: compile failure because `BrandLime`, `BaseBlack`, `Surface900`, `Surface850`, `Surface800`, and `Surface700` do not exist yet.

- [ ] **Step 3: Add web brand tokens**

In `Color.kt`, add these tokens near the top and keep existing tokens temporarily to reduce blast radius:

```kotlin
val BrandLime = Color(0xFFCCFF00)
val BaseBlack = Color(0xFF000000)
val Surface900 = Color(0xFF080808)
val Surface850 = Color(0xFF09090B)
val Surface800 = Color(0xFF18181B)
val Surface700 = Color(0xFF27272A)

val TextPrimaryDark = Color(0xFFFFFFFF)
val TextDefaultDark = Color(0xFFEFEFEF)
val TextSecondaryDark = Color(0xFFD8D8D8)
val TextMutedDark = Color(0xFF9DA2A8)
val StatusError = Color(0xFFFF4144)
val ControlTeal = Color(0xFF02DBA3)
val ControlTealActive = Color(0xFF01A47A)
```

- [ ] **Step 4: Map Material color schemes to the new brand**

In `Color.kt`, update `LightPrimary`, `DarkPrimary`, dark backgrounds, and dark surfaces:

```kotlin
val LightPrimary = Color(0xFF4E6200)
val LightOnPrimary = Color.Black
val LightPrimaryContainer = BrandLime
val LightOnPrimaryContainer = Color.Black

val DarkPrimary = BrandLime
val DarkOnPrimary = Color.Black
val DarkPrimaryContainer = Color(0xFF334000)
val DarkOnPrimaryContainer = BrandLime

val DarkBackground = BaseBlack
val DarkOnBackground = TextDefaultDark
val DarkSurface = Surface850
val DarkOnSurface = TextDefaultDark
val DarkSurfaceVariant = Surface800
val DarkOnSurfaceVariant = TextMutedDark
val DarkOutline = Color(0xFF494A4C)
val DarkOutlineVariant = Color(0x14FFFFFF)
val DarkError = StatusError
```

- [ ] **Step 5: Update extended colors without purple/cyan gradients**

In `ExtendedColors.kt`, map dark gradient values to dark surfaces and lime:

```kotlin
gradientStart = Surface800
gradientEnd = BrandLime
linkText = BrandLime
newBadge = BrandLime
onNewBadge = Color.Black
```

Keep success, warning, and info semantic colors unless they clash visibly.

- [ ] **Step 6: Run theme test**

Run:

```powershell
gradle :composeApp:allTests --tests "com.runninghub.app.ui.theme.RunningHubThemeTokenTest"
```

Expected: PASS.

- [ ] **Step 7: Compile Compose app**

Run:

```powershell
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

---

## Task 2: Standard API Model DTOs And Domain Models

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/ApiModel.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/ApiModelDto.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/ApiModelDtoTest.kt`

- [ ] **Step 1: Write DTO parsing tests**

Create `ApiModelDtoTest.kt`:

```kotlin
package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiModelDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `sku list response parses model card fields`() {
        val response = json.decodeFromString<BaseResponseDto<SkuListPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": "2046514150500524034",
                    "name": "全能图片G-2-文生图-官方稳定版",
                    "type": "text-to-image",
                    "source": "rh-ai",
                    "price": "0.06 CNY/次"
                  }
                ],
                "total": 1
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).records.single()
        assertEquals("2046514150500524034", record.id)
        assertEquals("全能图片G-2-文生图-官方稳定版", record.name)
        assertEquals("text-to-image", record.type)
        assertEquals("rh-ai", record.source)
        assertEquals("0.06 CNY/次", record.price)
    }

    @Test
    fun `sku detail response keeps endpoint and input config json`() {
        val response = json.decodeFromString<BaseResponseDto<SkuDetailDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "id": "2027192837726294017",
                "name": "全能图片V2-文生图-低价渠道版",
                "rhEndpoint": "/rhart-image-n-g31-flash/text-to-image",
                "inputConfigJson": "[{\"fieldKey\":\"prompt\",\"type\":\"STRING\",\"required\":true}]",
                "queueSize": 1000,
                "concurrencyLimit": 500
              }
            }
            """.trimIndent()
        )

        val detail = assertNotNull(response.data)
        assertEquals("/rhart-image-n-g31-flash/text-to-image", detail.rhEndpoint)
        assertEquals(1000, detail.queueSize)
        assertEquals(500, detail.concurrencyLimit)
        assertEquals(true, detail.inputConfigJson.contains("prompt"))
    }
}
```

- [ ] **Step 2: Run DTO tests and confirm failure**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.remote.dto.ApiModelDtoTest"
```

Expected: compile failure because DTO classes do not exist.

- [ ] **Step 3: Add domain models**

Create `ApiModel.kt`:

```kotlin
package com.runninghub.shared.domain.model

data class ApiModelSummary(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val endpoint: String? = null,
    val priceSummary: String? = null,
    val requiredFields: List<String> = emptyList(),
    val optionalFields: List<String> = emptyList(),
)

data class ApiModelDetail(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val endpoint: String,
    val priceSummary: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
    val fields: List<ApiModelField> = emptyList(),
    val rawInputConfigJson: String? = null,
)

data class ApiModelField(
    val fieldKey: String,
    val paramKey: String,
    val type: ApiModelFieldType,
    val required: Boolean,
    val title: String? = null,
    val description: String? = null,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val options: List<ApiModelFieldOption> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val precision: Int? = null,
    val multipleInputs: Boolean = false,
    val maxInputCount: Int? = null,
    val maxUploadCount: Int? = null,
    val maxUploadSizeBytes: Long? = null,
    val acceptFormats: List<String> = emptyList(),
    val visible: Boolean = true,
)

data class ApiModelFieldOption(
    val label: String,
    val value: String,
)

enum class ApiModelFieldType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    LIST,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    MODEL,
    UNKNOWN,
}

data class LlmModelSummary(
    val modelKey: String,
    val provider: String,
    val version: String? = null,
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)
```

- [ ] **Step 4: Add DTOs**

Create `ApiModelDto.kt`:

```kotlin
package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkuListRequestDto(
    val categoryType: String = "STANDARD_MODEL",
    val tagIds: List<String> = emptyList(),
    val search: String = "",
    val categoryTagIds: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
    val isCollected: Boolean = false,
    val region: String = "",
    val isWhitelist: Boolean = false,
    val pageNum: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class SkuListPageDto(
    val records: List<SkuSummaryDto> = emptyList(),
    val list: List<SkuSummaryDto> = emptyList(),
    val total: Int = 0,
) {
    val items: List<SkuSummaryDto>
        get() = if (records.isNotEmpty()) records else list
}

@Serializable
data class SkuSummaryDto(
    val id: String,
    val name: String = "",
    val nameEn: String? = null,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val price: String? = null,
    val priceSummary: String? = null,
    val rhEndpoint: String? = null,
)

@Serializable
data class SkuDetailRequestDto(
    val id: String,
)

@Serializable
data class SkuDetailDto(
    val id: String,
    val name: String = "",
    val nameEn: String? = null,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val price: String? = null,
    val priceSummary: String? = null,
    val rhEndpoint: String = "",
    val inputConfigJson: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
)

@Serializable
data class LlmModelDto(
    val modelKey: String,
    val provider: String = "",
    val version: String? = null,
    @SerialName("context")
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)
```

- [ ] **Step 5: Run DTO tests**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.remote.dto.ApiModelDtoTest"
```

Expected: PASS.

---

## Task 3: API Model Field Mapper

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ApiModelFieldMapper.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/ApiModelFieldMapperTest.kt`

- [ ] **Step 1: Write mapper tests**

Create `ApiModelFieldMapperTest.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelFieldType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModelFieldMapperTest {
    private val mapper = ApiModelFieldMapper(Json { ignoreUnknownKeys = true; isLenient = true })

    @Test
    fun `maps string list and image fields from input config json`() {
        val fields = mapper.parse(
            """
            [
              {
                "fieldKey": "prompt",
                "mappedApiParamKey": "prompt",
                "type": "STRING",
                "required": true,
                "title": "Prompt",
                "defaultValue": "a cat",
                "minLength": 1,
                "maxLength": 2000
              },
              {
                "fieldKey": "aspectRatio",
                "type": "LIST",
                "required": false,
                "defaultValue": "16:9",
                "options": [
                  { "label": "16:9", "value": "16:9" },
                  { "label": "1:1", "apiValue": "1:1" }
                ]
              },
              {
                "fieldKey": "imageUrls",
                "type": "IMAGE",
                "required": true,
                "multipleInputs": true,
                "maxUploadCount": 4,
                "maxUploadSize": 52428800,
                "accept": ["JPG", "PNG"]
              }
            ]
            """.trimIndent()
        )

        assertEquals(3, fields.size)
        assertEquals(ApiModelFieldType.STRING, fields[0].type)
        assertEquals("prompt", fields[0].paramKey)
        assertEquals(true, fields[0].required)
        assertEquals(2000, fields[0].maxLength)
        assertEquals(ApiModelFieldType.LIST, fields[1].type)
        assertEquals("1:1", fields[1].options[1].value)
        assertEquals(ApiModelFieldType.IMAGE, fields[2].type)
        assertEquals(true, fields[2].multipleInputs)
        assertEquals(4, fields[2].maxUploadCount)
        assertEquals(listOf("JPG", "PNG"), fields[2].acceptFormats)
    }

    @Test
    fun `unknown or malformed input config returns empty list`() {
        assertEquals(emptyList(), mapper.parse(null))
        assertEquals(emptyList(), mapper.parse(""))
        assertEquals(emptyList(), mapper.parse("{not-json"))
    }
}
```

- [ ] **Step 2: Run mapper tests and confirm failure**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.repository.ApiModelFieldMapperTest"
```

Expected: compile failure because `ApiModelFieldMapper` does not exist.

- [ ] **Step 3: Implement mapper**

Create `ApiModelFieldMapper.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ApiModelFieldOption
import com.runninghub.shared.domain.model.ApiModelFieldType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiModelFieldMapper(private val json: Json) {
    fun parse(inputConfigJson: String?): List<ApiModelField> {
        if (inputConfigJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val root = json.parseToJsonElement(inputConfigJson)
            when (root) {
                is JsonArray -> root.mapNotNull { it.toFieldOrNull() }
                is JsonObject -> root["inputs"]?.jsonArray?.mapNotNull { it.toFieldOrNull() }.orEmpty()
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun JsonElement.toFieldOrNull(): ApiModelField? {
        val obj = this as? JsonObject ?: return null
        val fieldKey = obj.string("fieldKey") ?: obj.string("key") ?: obj.string("name") ?: return null
        val paramKey = obj.string("mappedApiParamKey") ?: obj.string("paramKey") ?: fieldKey
        val type = obj.string("type") ?: obj.string("fieldType")
        return ApiModelField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            type = type.toFieldType(),
            required = obj.boolean("required") ?: false,
            title = obj.string("title") ?: obj.string("name"),
            description = obj.string("paramDesc") ?: obj.string("description"),
            placeholder = obj.string("placeholder"),
            defaultValue = obj["defaultValue"]?.jsonPrimitive?.contentOrNull,
            options = obj.array("options").mapNotNull { it.toOptionOrNull() },
            minLength = obj.int("minLength"),
            maxLength = obj.int("maxLength"),
            min = obj.double("min"),
            max = obj.double("max"),
            step = obj.double("step"),
            precision = obj.int("precision"),
            multipleInputs = obj.boolean("multipleInputs") ?: false,
            maxInputCount = obj.int("maxInputCount") ?: obj.int("maxInpuNum"),
            maxUploadCount = obj.int("maxUploadCount"),
            maxUploadSizeBytes = obj.long("maxUploadSize") ?: obj.long("maxSize"),
            acceptFormats = obj.acceptFormats(),
            visible = obj.boolean("visible") ?: true,
        )
    }

    private fun JsonElement.toOptionOrNull(): ApiModelFieldOption? {
        val obj = this as? JsonObject ?: return null
        val value = obj.string("apiValue") ?: obj.string("value") ?: return null
        val label = obj.string("label") ?: obj.string("name") ?: value
        return ApiModelFieldOption(label = label, value = value)
    }

    private fun String?.toFieldType(): ApiModelFieldType = when (this?.uppercase()) {
        "STRING", "TEXT" -> ApiModelFieldType.STRING
        "NUMBER", "FLOAT", "DOUBLE" -> ApiModelFieldType.NUMBER
        "INTEGER", "INT" -> ApiModelFieldType.INTEGER
        "BOOLEAN", "BOOL" -> ApiModelFieldType.BOOLEAN
        "LIST", "SELECT", "ENUM" -> ApiModelFieldType.LIST
        "IMAGE", "IMAGE_UPLOAD" -> ApiModelFieldType.IMAGE
        "VIDEO", "VIDEO_UPLOAD" -> ApiModelFieldType.VIDEO
        "AUDIO", "AUDIO_UPLOAD" -> ApiModelFieldType.AUDIO
        "UPLOAD", "FILE" -> ApiModelFieldType.FILE
        "MODEL" -> ApiModelFieldType.MODEL
        else -> ApiModelFieldType.UNKNOWN
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull()

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.array(key: String): List<JsonElement> =
        (this[key] as? JsonArray)?.toList().orEmpty()

    private fun JsonObject.acceptFormats(): List<String> =
        when (val accept = this["accept"]) {
            is JsonArray -> accept.mapNotNull { it.jsonPrimitive.contentOrNull }
            else -> emptyList()
        }
}
```

- [ ] **Step 4: Run mapper tests**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.repository.ApiModelFieldMapperTest"
```

Expected: PASS.

---

## Task 4: Model Catalog API And Repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/ModelCatalogApi.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/ModelCatalogRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ModelCatalogRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/ModelCatalogRepositoryMapperTest.kt`

- [ ] **Step 1: Create repository mapper test**

Create `ModelCatalogRepositoryMapperTest.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuSummaryDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelCatalogRepositoryMapperTest {
    private val mapper = ApiModelFieldMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun `maps sku summary to domain summary`() {
        val summary = SkuSummaryDto(
            id = "sku-1",
            name = "全能图片V2",
            type = "text-to-image",
            groupName = "全能图片",
            source = "rh-ai",
            price = "0.16 CNY/次",
            rhEndpoint = "/rhart-image/text-to-image",
        ).toDomain()

        assertEquals("sku-1", summary.id)
        assertEquals("全能图片V2", summary.name)
        assertEquals("text-to-image", summary.type)
        assertEquals("/rhart-image/text-to-image", summary.endpoint)
        assertEquals("0.16 CNY/次", summary.priceSummary)
    }

    @Test
    fun `maps sku detail with parsed fields`() {
        val detail = SkuDetailDto(
            id = "sku-1",
            name = "全能图片V2",
            rhEndpoint = "/rhart-image/text-to-image",
            inputConfigJson = """[{"fieldKey":"prompt","type":"STRING","required":true}]""",
        ).toDomain(mapper)

        assertEquals("sku-1", detail.id)
        assertEquals("/rhart-image/text-to-image", detail.endpoint)
        assertEquals("prompt", detail.fields.single().fieldKey)
    }
}
```

- [ ] **Step 2: Add repository interface**

Create `ModelCatalogRepository.kt`:

```kotlin
package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.ApiModelDetail
import com.runninghub.shared.domain.model.ApiModelSummary
import com.runninghub.shared.domain.model.LlmModelSummary

interface ModelCatalogRepository {
    suspend fun listStandardModels(search: String = "", page: Int = 1, size: Int = 30): Result<List<ApiModelSummary>>
    suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail>
    suspend fun listLlmModels(): Result<List<LlmModelSummary>>
}
```

- [ ] **Step 3: Add API client**

Create `ModelCatalogApi.kt`:

```kotlin
package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.BaseResponseDto
import com.runninghub.shared.data.remote.dto.LlmModelDto
import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuDetailRequestDto
import com.runninghub.shared.data.remote.dto.SkuListPageDto
import com.runninghub.shared.data.remote.dto.SkuListRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ModelCatalogApi(private val client: HttpClient) {
    companion object {
        private const val BASE_URL = "https://www.runninghub.cn"
    }

    suspend fun listStandardModels(request: SkuListRequestDto): BaseResponseDto<SkuListPageDto> =
        client.post("$BASE_URL/api/sku/list") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getStandardModelDetail(id: String): BaseResponseDto<SkuDetailDto> =
        client.post("$BASE_URL/api/sku/detail") {
            contentType(ContentType.Application.Json)
            setBody(SkuDetailRequestDto(id))
        }.body()

    suspend fun listLlmModels(): BaseResponseDto<List<LlmModelDto>> =
        client.get("$BASE_URL/llm/api/models").body()
}
```

- [ ] **Step 4: Add repository implementation and mappers**

Create `ModelCatalogRepositoryImpl.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.ModelCatalogApi
import com.runninghub.shared.data.remote.dto.LlmModelDto
import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuListRequestDto
import com.runninghub.shared.data.remote.dto.SkuSummaryDto
import com.runninghub.shared.domain.model.ApiModelDetail
import com.runninghub.shared.domain.model.ApiModelSummary
import com.runninghub.shared.domain.model.LlmModelSummary
import com.runninghub.shared.domain.repository.ModelCatalogRepository
import kotlinx.serialization.json.Json

class ModelCatalogRepositoryImpl(
    private val api: ModelCatalogApi,
    json: Json,
) : ModelCatalogRepository {
    private val fieldMapper = ApiModelFieldMapper(json)

    override suspend fun listStandardModels(search: String, page: Int, size: Int): Result<List<ApiModelSummary>> =
        runCatching {
            val response = api.listStandardModels(
                SkuListRequestDto(search = search, pageNum = page, pageSize = size)
            )
            check(response.code == 0) { response.msg.ifEmpty { "Model list load failed" } }
            response.data?.items.orEmpty().map { it.toDomain() }
        }

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> =
        runCatching {
            val response = api.getStandardModelDetail(modelId)
            check(response.code == 0) { response.msg.ifEmpty { "Model detail load failed" } }
            checkNotNull(response.data) { "Model detail missing" }.toDomain(fieldMapper)
        }

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        runCatching {
            val response = api.listLlmModels()
            check(response.code == 0) { response.msg.ifEmpty { "LLM model list load failed" } }
            response.data.orEmpty().map { it.toDomain() }
        }
}

fun SkuSummaryDto.toDomain(): ApiModelSummary =
    ApiModelSummary(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        endpoint = rhEndpoint,
        priceSummary = priceSummary ?: price,
    )

fun SkuDetailDto.toDomain(fieldMapper: ApiModelFieldMapper): ApiModelDetail =
    ApiModelDetail(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
        source = source,
        endpoint = rhEndpoint,
        priceSummary = priceSummary ?: price,
        queueSize = queueSize,
        concurrencyLimit = concurrencyLimit,
        fields = fieldMapper.parse(inputConfigJson),
        rawInputConfigJson = inputConfigJson,
    )

fun LlmModelDto.toDomain(): LlmModelSummary =
    LlmModelSummary(
        modelKey = modelKey,
        provider = provider,
        version = version,
        contextLength = contextLength,
        capabilities = capabilities,
        inputPrice = inputPrice,
        outputPrice = outputPrice,
    )
```

- [ ] **Step 5: Register Koin bindings**

Modify `SharedModule.kt`:

```kotlin
import com.runninghub.shared.data.remote.api.ModelCatalogApi
import com.runninghub.shared.data.repository.ModelCatalogRepositoryImpl
import com.runninghub.shared.domain.repository.ModelCatalogRepository
```

Then add:

```kotlin
single { ModelCatalogApi(get()) }
single<ModelCatalogRepository> { ModelCatalogRepositoryImpl(get(), get()) }
```

- [ ] **Step 6: Run mapper test and compile shared module**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.repository.ModelCatalogRepositoryMapperTest"
gradle :shared:compileKotlinAndroid
```

Expected: both commands PASS.

---

## Task 5: Model Invocation Foundation

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/ModelInvocation.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/ModelInvocationRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ModelInvocationRequestBuilder.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/ModelInvocationRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/ModelInvocationRequestBuilderTest.kt`

- [ ] **Step 1: Write request builder tests**

Create `ModelInvocationRequestBuilderTest.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ApiModelFieldType
import com.runninghub.shared.domain.model.ModelFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelInvocationRequestBuilderTest {
    @Test
    fun `builds request body from typed values and visible fields`() {
        val body = ModelInvocationRequestBuilder().build(
            fields = listOf(
                ApiModelField("prompt", "prompt", ApiModelFieldType.STRING, required = true),
                ApiModelField("aspectRatio", "aspectRatio", ApiModelFieldType.LIST, required = false),
                ApiModelField("private", "private", ApiModelFieldType.STRING, required = false, visible = false),
            ),
            values = mapOf(
                "prompt" to ModelFieldValue.Text("a cat"),
                "aspectRatio" to ModelFieldValue.Text("16:9"),
                "private" to ModelFieldValue.Text("internal"),
            ),
            webhookUrl = "https://example.com/hook",
        )

        assertEquals("a cat", body["prompt"])
        assertEquals("16:9", body["aspectRatio"])
        assertEquals("https://example.com/hook", body["webhookUrl"])
        assertEquals(false, body.containsKey("private"))
    }

    @Test
    fun `builds list value for multi media field`() {
        val body = ModelInvocationRequestBuilder().build(
            fields = listOf(
                ApiModelField(
                    fieldKey = "imageUrls",
                    paramKey = "imageUrls",
                    type = ApiModelFieldType.IMAGE,
                    required = true,
                    multipleInputs = true,
                )
            ),
            values = mapOf("imageUrls" to ModelFieldValue.StringList(listOf("https://a.png", "https://b.png"))),
        )

        assertEquals(listOf("https://a.png", "https://b.png"), body["imageUrls"])
    }
}
```

- [ ] **Step 2: Add invocation domain models**

Create `ModelInvocation.kt`:

```kotlin
package com.runninghub.shared.domain.model

sealed interface ModelFieldValue {
    data class Text(val value: String) : ModelFieldValue
    data class NumberValue(val value: Double) : ModelFieldValue
    data class BooleanValue(val value: Boolean) : ModelFieldValue
    data class StringList(val values: List<String>) : ModelFieldValue
}

data class ModelInvocationRequest(
    val modelId: String,
    val endpoint: String,
    val fields: List<ApiModelField>,
    val values: Map<String, ModelFieldValue>,
    val webhookUrl: String? = null,
)

data class ModelInvocationTask(
    val taskId: String,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val resultUrls: List<String> = emptyList(),
)
```

- [ ] **Step 3: Add repository interface**

Create `ModelInvocationRepository.kt`:

```kotlin
package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.ModelInvocationRequest
import com.runninghub.shared.domain.model.ModelInvocationTask

interface ModelInvocationRepository {
    suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask>
    suspend fun queryTask(taskId: String): Result<ModelInvocationTask>
    suspend fun uploadMedia(apiKey: String, fileBytes: ByteArray, fileName: String, contentType: String): Result<String>
}
```

- [ ] **Step 4: Implement request builder**

Create `ModelInvocationRequestBuilder.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ModelFieldValue

class ModelInvocationRequestBuilder {
    fun build(
        fields: List<ApiModelField>,
        values: Map<String, ModelFieldValue>,
        webhookUrl: String? = null,
    ): Map<String, Any> {
        val body = linkedMapOf<String, Any>()
        fields.filter { it.visible }.forEach { field ->
            val value = values[field.paramKey] ?: values[field.fieldKey] ?: return@forEach
            body[field.paramKey] = value.toBodyValue()
        }
        if (!webhookUrl.isNullOrBlank()) {
            body["webhookUrl"] = webhookUrl
        }
        return body
    }

    private fun ModelFieldValue.toBodyValue(): Any = when (this) {
        is ModelFieldValue.Text -> value
        is ModelFieldValue.NumberValue -> value
        is ModelFieldValue.BooleanValue -> value
        is ModelFieldValue.StringList -> values
    }
}
```

- [ ] **Step 5: Implement repository by reusing QuickCreateApi primitives**

Create `ModelInvocationRepositoryImpl.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.domain.model.ModelInvocationRequest
import com.runninghub.shared.domain.model.ModelInvocationTask
import com.runninghub.shared.domain.repository.ModelInvocationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

class ModelInvocationRepositoryImpl(
    private val client: HttpClient,
    private val quickCreateApi: QuickCreateApi,
) : ModelInvocationRepository {
    private val builder = ModelInvocationRequestBuilder()

    override suspend fun submitStandardModel(request: ModelInvocationRequest): Result<ModelInvocationTask> =
        runCatching {
            val endpoint = request.endpoint.ensureOpenApiEndpoint()
            val response = client.post("${QuickCreateApi.BASE_URL}$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(builder.build(request.fields, request.values, request.webhookUrl))
            }.body<JsonObject>()
            val taskId = response["taskId"]?.toString()?.trim('"')
                ?: response["data"]?.toString()?.trim('"')
                ?: ""
            ModelInvocationTask(taskId = taskId, status = response["status"]?.toString()?.trim('"'))
        }

    override suspend fun queryTask(taskId: String): Result<ModelInvocationTask> =
        runCatching {
            val response = quickCreateApi.queryTask(taskId)
            ModelInvocationTask(
                taskId = taskId,
                status = response.status,
                errorCode = response.errorCode,
                errorMessage = response.errorMessage,
                resultUrls = response.results?.mapNotNull { it.url }.orEmpty(),
            )
        }

    override suspend fun uploadMedia(
        apiKey: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
    ): Result<String> =
        runCatching {
            val response = quickCreateApi.uploadMedia(apiKey, fileBytes, fileName, contentType)
            response.url ?: error("Upload response missing URL")
        }

    private fun String.ensureOpenApiEndpoint(): String =
        if (startsWith("/openapi/v2/")) this else "/openapi/v2${if (startsWith("/")) this else "/$this"}"
}
```

- [ ] **Step 6: Register Koin binding**

Modify `SharedModule.kt`:

```kotlin
import com.runninghub.shared.data.repository.ModelInvocationRepositoryImpl
import com.runninghub.shared.domain.repository.ModelInvocationRepository
```

Add:

```kotlin
single<ModelInvocationRepository> { ModelInvocationRepositoryImpl(get(), get()) }
```

- [ ] **Step 7: Run tests and compile**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.repository.ModelInvocationRequestBuilderTest"
gradle :shared:compileKotlinAndroid
```

Expected: both commands PASS.

---

## Task 6: Unified Generation History Repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/GenerationHistory.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/GenerationHistoryRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/GenerationHistoryRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/repository/GenerationHistoryMapperTest.kt`

- [ ] **Step 1: Write normalization tests**

Create `GenerationHistoryMapperTest.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryOutput
import kotlin.test.Test
import kotlin.test.assertEquals

class GenerationHistoryMapperTest {
    @Test
    fun `maps quick creation item to unified history item`() {
        val item = QuickCreationHistoryItem(
            taskId = "task-1",
            status = "SUCCESS",
            skuId = "sku-1",
            taskType = "image",
            cashAmount = 0.16,
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "out-1",
                    url = "https://example.com/a.png",
                    type = "png",
                    width = 1024,
                    height = 1024,
                )
            )
        ).toGenerationHistoryItem()

        assertEquals("task-1", item.taskId)
        assertEquals("QUICK_CREATE", item.source)
        assertEquals("SUCCESS", item.status)
        assertEquals("sku-1", item.modelId)
        assertEquals(0.16, item.cost)
        assertEquals("https://example.com/a.png", item.outputs.single().url)
    }
}
```

- [ ] **Step 2: Add domain models**

Create `GenerationHistory.kt`:

```kotlin
package com.runninghub.shared.domain.model

data class GenerationHistoryPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<GenerationHistoryItem>,
)

data class GenerationHistoryItem(
    val taskId: String,
    val source: String,
    val status: String,
    val modelId: String? = null,
    val modelName: String? = null,
    val taskType: String? = null,
    val cost: Double = 0.0,
    val currency: String? = null,
    val createdAt: String? = null,
    val durationText: String? = null,
    val params: Map<String, String> = emptyMap(),
    val outputs: List<GenerationHistoryOutput> = emptyList(),
)

data class GenerationHistoryOutput(
    val outputId: String,
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val outputName: String? = null,
    val expireTime: String? = null,
)
```

- [ ] **Step 3: Add repository interface**

Create `GenerationHistoryRepository.kt`:

```kotlin
package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.GenerationHistoryItem
import com.runninghub.shared.domain.model.GenerationHistoryPage

interface GenerationHistoryRepository {
    suspend fun listHistory(page: Int = 1, size: Int = 20): Result<GenerationHistoryPage>
    suspend fun listProjectHistory(projectId: String, page: Int = 1, size: Int = 20): Result<GenerationHistoryPage>
    suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem>
    suspend fun cancelTask(taskId: String): Result<Unit>
}
```

- [ ] **Step 4: Implement quick-create facade**

Create `GenerationHistoryRepositoryImpl.kt`:

```kotlin
package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.GenerationHistoryItem
import com.runninghub.shared.domain.model.GenerationHistoryOutput
import com.runninghub.shared.domain.model.GenerationHistoryPage
import com.runninghub.shared.domain.repository.GenerationHistoryRepository
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryOutput

class GenerationHistoryRepositoryImpl(
    private val quickCreateRepository: QuickCreateRepository,
) : GenerationHistoryRepository {
    override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> =
        quickCreateRepository.listQuickCreationHistory(page, size).map { pageData ->
            GenerationHistoryPage(
                page = pageData.page,
                size = pageData.size,
                total = pageData.total,
                items = pageData.items.map { it.toGenerationHistoryItem() },
            )
        }

    override suspend fun listProjectHistory(projectId: String, page: Int, size: Int): Result<GenerationHistoryPage> =
        quickCreateRepository.listQuickCreationProjectTasks(projectId, page, size).map { pageData ->
            GenerationHistoryPage(
                page = pageData.page,
                size = pageData.size,
                total = pageData.total,
                items = pageData.items.map { it.toGenerationHistoryItem() },
            )
        }

    override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> =
        quickCreateRepository.getQuickCreationHistoryDetail(outputId).map { it.toGenerationHistoryItem() }

    override suspend fun cancelTask(taskId: String): Result<Unit> =
        quickCreateRepository.cancelQuickCreationTask(taskId)
}

fun QuickCreationHistoryItem.toGenerationHistoryItem(): GenerationHistoryItem =
    GenerationHistoryItem(
        taskId = taskId,
        source = "QUICK_CREATE",
        status = status,
        modelId = skuId,
        taskType = taskType,
        cost = cashAmount,
        currency = cashCurrency,
        durationText = taskCostTime,
        params = params,
        outputs = outputs.map { it.toGenerationHistoryOutput() },
    )

fun QuickCreationHistoryOutput.toGenerationHistoryOutput(): GenerationHistoryOutput =
    GenerationHistoryOutput(
        outputId = outputId,
        url = url,
        type = type,
        thumbnailUrl = thumbnailUrl,
        width = width,
        height = height,
        outputName = outputName,
        expireTime = expireTime,
    )
```

- [ ] **Step 5: Register Koin binding**

Modify `SharedModule.kt`:

```kotlin
import com.runninghub.shared.data.repository.GenerationHistoryRepositoryImpl
import com.runninghub.shared.domain.repository.GenerationHistoryRepository
```

Add:

```kotlin
single<GenerationHistoryRepository> { GenerationHistoryRepositoryImpl(get()) }
```

- [ ] **Step 6: Run tests and compile**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.repository.GenerationHistoryMapperTest"
gradle :shared:compileKotlinAndroid
```

Expected: both commands PASS.

---

## Task 7: Plaza DTOs, API, Repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/model/Plaza.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/domain/repository/PlazaRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/dto/PlazaDto.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/remote/api/PlazaApi.kt`
- Create: `shared/src/commonMain/kotlin/com/runninghub/shared/data/repository/PlazaRepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/runninghub/shared/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/com/runninghub/shared/data/remote/dto/PlazaDtoTest.kt`

- [ ] **Step 1: Write DTO parsing test**

Create `PlazaDtoTest.kt`:

```kotlin
package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlazaDtoTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `creation list response parses media and owner`() {
        val response = json.decodeFromString<BaseResponseDto<PlazaCreationPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": "2067532558576996354",
                    "intro": "美女",
                    "owner": { "id": "owner-1", "name": "kitten HZ", "avatar": "https://avatar.png" },
                    "statisticsInfo": { "likeCount": "2", "useCount": "76", "collectCount": "1" },
                    "creationShowreelInfo": {
                      "outputId": "out-1",
                      "fileUrl": "https://image.png",
                      "fileType": "png",
                      "imageWidth": 1664,
                      "imageHeight": 2496
                    },
                    "liked": false,
                    "collected": false
                  }
                ],
                "total": 1
              }
            }
            """.trimIndent()
        )

        val card = assertNotNull(response.data).items.single()
        assertEquals("2067532558576996354", card.id)
        assertEquals("kitten HZ", card.owner?.name)
        assertEquals("https://image.png", card.creationShowreelInfo?.fileUrl)
        assertEquals("2", card.statisticsInfo?.likeCount)
    }
}
```

- [ ] **Step 2: Add domain models**

Create `Plaza.kt`:

```kotlin
package com.runninghub.shared.domain.model

data class PlazaTag(val id: String, val name: String, val level: Int = 0, val enable: Boolean = true)

data class PlazaCreationPage(val page: Int, val total: Int, val items: List<PlazaCreationCard>)

data class PlazaCreationCard(
    val id: String,
    val intro: String? = null,
    val publishTime: String? = null,
    val ownerName: String? = null,
    val ownerAvatar: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val likeCount: String? = null,
    val useCount: String? = null,
    val collectCount: String? = null,
    val liked: Boolean = false,
    val collected: Boolean = false,
)

data class PlazaShortCategory(val id: String? = null, val code: String, val name: String)

data class PlazaShortCard(
    val id: String,
    val name: String,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val categoryName: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null,
)
```

- [ ] **Step 3: Add DTOs, API, repository**

Implement DTOs and mappings using fields named in `doc/RunningHub-explore-capture-2026-06-19.md`. API methods:

```kotlin
suspend fun getCreationTags(): BaseResponseDto<List<PlazaTagDto>>
suspend fun listCreations(request: PlazaCreationListRequestDto): BaseResponseDto<PlazaCreationPageDto>
suspend fun listShortCategories(): BaseResponseDto<List<PlazaShortCategoryDto>>
suspend fun listShorts(request: PlazaShortListRequestDto): BaseResponseDto<PlazaShortPageDto>
```

Repository interface:

```kotlin
interface PlazaRepository {
    suspend fun getTags(): Result<List<PlazaTag>>
    suspend fun listCreations(page: Int, size: Int, sort: String, tags: List<String>): Result<PlazaCreationPage>
    suspend fun listShortCategories(): Result<List<PlazaShortCategory>>
    suspend fun listShorts(page: Int, size: Int, categoryCode: String? = null): Result<List<PlazaShortCard>>
}
```

- [ ] **Step 4: Register Koin binding**

Add `PlazaApi` and `PlazaRepositoryImpl` to `SharedModule.kt`.

- [ ] **Step 5: Run tests and compile**

Run:

```powershell
gradle :shared:allTests --tests "com.runninghub.shared.data.remote.dto.PlazaDtoTest"
gradle :shared:compileKotlinAndroid
```

Expected: both commands PASS.

---

## Task 8: Navigation And ScreenModel Wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/navigation/MainScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/di/AppModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreenModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreenModel.kt`

- [ ] **Step 1: Add create and plaza screen models**

Create minimal screen models that load repositories and expose UI state. Start with loading, error, and empty states; add actions in later tasks.

- [ ] **Step 2: Replace tab enum labels**

Update `BottomNavTab` to:

```kotlin
enum class BottomNavTab(...) {
    Discovery("发现", Icons.Filled.Explore, Icons.Outlined.Explore),
    Create("创作", Icons.Filled.Star, Icons.Outlined.Star),
    Plaza("广场", Icons.Filled.Public, Icons.Outlined.Public),
    History("历史", Icons.Filled.History, Icons.Outlined.History),
    Profile("我的", Icons.Filled.Person, Icons.Outlined.Person),
}
```

Use available Material icons already in the dependency. If `Public` or `History` icons are unavailable in this version, use `Explore` for plaza and `Build` for history temporarily, but keep labels correct.

- [ ] **Step 3: Wire tab content**

Map tabs:

```kotlin
Discovery -> DiscoveryVoyagerScreen().Content()
Create -> CreateVoyagerScreen().Content()
Plaza -> PlazaVoyagerScreen().Content()
History -> TaskHistoryVoyagerScreen().Content()
Profile -> ProfileVoyagerScreen().Content()
```

- [ ] **Step 4: Register new screen models**

In `AppModule.kt`, register:

```kotlin
factoryOf(::CreateScreenModel)
factoryOf(::PlazaScreenModel)
```

- [ ] **Step 5: Compile Compose app**

Run:

```powershell
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

---

## Task 9: Unified Create UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/ApiModelFieldUi.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/create/CreateScreenModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/create/ApiModelFieldUiModelTest.kt`

- [ ] **Step 1: Add UI model tests**

Create tests for mapping domain fields to stable UI field types:

```kotlin
package com.runninghub.app.ui.feature.create

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ApiModelFieldType
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModelFieldUiModelTest {
    @Test
    fun `list field maps to option selector`() {
        val ui = ApiModelField("aspectRatio", "aspectRatio", ApiModelFieldType.LIST, required = true).toUiModel()
        assertEquals(ApiModelFieldUiType.Options, ui.type)
    }

    @Test
    fun `image field maps to upload control`() {
        val ui = ApiModelField("imageUrls", "imageUrls", ApiModelFieldType.IMAGE, required = true).toUiModel()
        assertEquals(ApiModelFieldUiType.Upload, ui.type)
    }
}
```

- [ ] **Step 2: Implement UI model mapper**

Create:

```kotlin
enum class ApiModelFieldUiType { Text, Number, Toggle, Options, Upload, Unsupported }

data class ApiModelFieldUiModel(
    val key: String,
    val label: String,
    val type: ApiModelFieldUiType,
    val required: Boolean,
)

fun ApiModelField.toUiModel(): ApiModelFieldUiModel =
    ApiModelFieldUiModel(
        key = paramKey,
        label = title ?: fieldKey,
        type = when (type) {
            ApiModelFieldType.STRING -> ApiModelFieldUiType.Text
            ApiModelFieldType.NUMBER, ApiModelFieldType.INTEGER -> ApiModelFieldUiType.Number
            ApiModelFieldType.BOOLEAN -> ApiModelFieldUiType.Toggle
            ApiModelFieldType.LIST -> ApiModelFieldUiType.Options
            ApiModelFieldType.IMAGE, ApiModelFieldType.VIDEO, ApiModelFieldType.AUDIO, ApiModelFieldType.FILE -> ApiModelFieldUiType.Upload
            else -> ApiModelFieldUiType.Unsupported
        },
        required = required,
    )
```

- [ ] **Step 3: Build the first create screen**

Implement:

- Header with title `创作`.
- Category chips: `图片`, `视频`, `音频`, `LLM`, `全部`.
- Model search/list from `ModelCatalogRepository.listStandardModels`.
- Model detail load on selection.
- Dynamic field rendering for text, number, toggle, options, and upload controls.
- Sticky bottom actions: `预估费用`, `生成`.

- [ ] **Step 4: Submit task through `ModelInvocationRepository`**

When the user taps `生成`:

- Validate required visible fields.
- Build `ModelInvocationRequest`.
- Call `submitStandardModel`.
- Show task ID or error.
- Do not implement LLM submit in this task.

- [ ] **Step 5: Run tests and compile**

Run:

```powershell
gradle :composeApp:allTests --tests "com.runninghub.app.ui.feature.create.ApiModelFieldUiModelTest"
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: both commands PASS.

---

## Task 10: First-Class History UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/history/TaskHistoryScreenModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/history/GenerationHistoryUiTest.kt`

- [ ] **Step 1: Write status mapping tests**

Create `GenerationHistoryUiTest.kt`:

```kotlin
package com.runninghub.app.ui.feature.history

import kotlin.test.Test
import kotlin.test.assertEquals

class GenerationHistoryUiTest {
    @Test
    fun `success status maps to completed filter`() {
        assertEquals(TaskHistoryFilter.COMPLETED, generationHistoryFilterForStatus("SUCCESS"))
    }

    @Test
    fun `running status maps to in progress filter`() {
        assertEquals(TaskHistoryFilter.IN_PROGRESS, generationHistoryFilterForStatus("RUNNING"))
    }
}
```

- [ ] **Step 2: Switch ScreenModel to `GenerationHistoryRepository`**

Replace direct `WebAppRepository` dependency with `GenerationHistoryRepository`.

- [ ] **Step 3: Update UI copy and rows**

Use labels:

- Title: `生成历史`
- Empty: `暂无生成记录`
- Filters: `全部`, `进行中`, `成功`, `失败`
- Row actions: `查看`, `复用参数`, `取消`

Rows should show thumbnail, source, status, cost, task ID, and output count when available.

- [ ] **Step 4: Add running task cancel**

Call `GenerationHistoryRepository.cancelTask(taskId)` for running tasks.

- [ ] **Step 5: Run tests and compile**

Run:

```powershell
gradle :composeApp:allTests --tests "com.runninghub.app.ui.feature.history.GenerationHistoryUiTest"
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: both commands PASS.

---

## Task 11: Read-Only Plaza UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/runninghub/app/ui/feature/plaza/PlazaScreenModel.kt`
- Test: `composeApp/src/commonTest/kotlin/com/runninghub/app/ui/feature/plaza/PlazaUiStateTest.kt`

- [ ] **Step 1: Add UI state tests**

Create `PlazaUiStateTest.kt`:

```kotlin
package com.runninghub.app.ui.feature.plaza

import kotlin.test.Test
import kotlin.test.assertEquals

class PlazaUiStateTest {
    @Test
    fun `default sort is recommend`() {
        assertEquals("RECOMMEND", PlazaUiState().sort)
    }

    @Test
    fun `default tab is inspiration`() {
        assertEquals(PlazaTab.Inspiration, PlazaUiState().selectedTab)
    }
}
```

- [ ] **Step 2: Implement screen model**

Load:

- Tags on first load.
- Inspiration creations with `sort=RECOMMEND`.
- Shorts when user switches to `短片`.

Support:

- Refresh.
- Sort change.
- Tag selection.
- Load more.

- [ ] **Step 3: Implement screen**

UI structure:

- Header `广场`.
- Sort segmented control: `推荐`, `最热`, `最新`.
- Tabs: `灵感`, `短片`.
- Horizontal tag chips.
- Two-column adaptive grid for media cards.
- Loading, empty, and error states.

- [ ] **Step 4: Keep write actions disabled**

Show like/use/comment counts as read-only. Do not add like, collect, comment submit, or follow actions until endpoints are captured.

- [ ] **Step 5: Run tests and compile**

Run:

```powershell
gradle :composeApp:allTests --tests "com.runninghub.app.ui.feature.plaza.PlazaUiStateTest"
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: both commands PASS.

---

## Task 12: Full Verification

**Files:**
- No new files unless earlier tasks require small fixes.

- [ ] **Step 1: Run shared compilation**

Run:

```powershell
gradle :shared:compileKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run compose compilation**

Run:

```powershell
gradle :composeApp:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all common tests**

Run:

```powershell
gradle :shared:allTests :composeApp:allTests
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Build debug app**

Run:

```powershell
gradle :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual verification on Android**

Check:

- Bottom navigation shows `发现 / 创作 / 广场 / 历史 / 我的`.
- `创作` loads model list and a selected model detail.
- Schema-driven fields render for a text-to-image model.
- Required-field validation blocks empty submit.
- Media upload field is visible for image-to-image models.
- Submitting a standard model shows a task ID or clear API error.
- `历史` loads records and filters by status.
- Running history item exposes cancel action.
- `广场` loads inspiration list, sort modes, tags, and short films.
- Theme uses dark surfaces and lime selected/CTA states.

---

## Scope And Sequencing Notes

- Implement Tasks 1-6 first. They create the foundation and can be validated mostly with common tests.
- Implement Tasks 8-10 next to expose the creation and history workflow.
- Implement Task 11 after shared plaza mappings are stable.
- Do not delete existing quick-create code until the new dynamic flow has proven parity for basic image/video generation.
- Do not implement plaza write actions or LLM streaming in this plan.

## Self-Review

- Spec coverage: Theme, unified model catalog, invocation, history, plaza, and navigation are covered by tasks.
- Completion marker scan: no unfinished markers are intentionally left in this plan.
- Type consistency: domain type names are reused consistently across shared and compose tasks.
- Scope split: the plan keeps the full PRD direction but sequences independent subsystems so each task can be verified.
