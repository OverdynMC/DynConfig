# Design Document

## Overview

Данная библиотека представляет собой высокоуровневую обертку над SpongePowered Configurate, предоставляющую декларативное API на основе аннотаций для работы с конфигурационными файлами. Основная цель — минимизировать boilerplate-код при работе с конфигурациями, обеспечить автоматическую генерацию файлов с дефолтными значениями, поддержку многоязычных комментариев и гибкую систему кастомных сериализаторов.

**Ключевые принципы архитектуры:**

1. **Separation of Concerns**: Четкое разделение между backend-слоем (работа с файлами), processing-слоем (обработка аннотаций и маппинг) и API-слоем (пользовательский интерфейс)
2. **Convention over Configuration**: Использование разумных дефолтов (имя поля = ключ в YAML, автоматическая обработка примитивов)
3. **Extensibility**: Возможность регистрации кастомных backends, адаптеров и политик
4. **Type Safety**: Строгая типизация через generics и compile-time проверки
5. **Fail-Fast**: Ранняя валидация и понятные сообщения об ошибках

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Application Layer                       │
│  (User code with @ConfigResource annotated classes)         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                         API Layer                            │
│  ConfigManager: get(), reload(), update(), saveAll()        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Processing Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ConfigProcessor│  │ FieldMapper  │  │CommentResolver│     │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │AdapterRegistry│  │ LangContext  │                         │
│  └──────────────┘  └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Backend Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           ConfigBackend (interface)                   │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │BukkitYamlBackend│ │JsonBackend │  │ HoconBackend │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   SpongePowered Configurate                  │
│              (YAML/JSON/HOCON parsing core)                  │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
User Code
   │
   ├─→ ConfigManager.get(MyConfig.class)
   │      │
   │      ├─→ Check cache
   │      │      │
   │      │      └─→ If cached: return instance
   │      │
   │      └─→ If not cached:
   │             │
   │             ├─→ ConfigProcessor.load(MyConfig.class)
   │             │      │
   │             │      ├─→ Validate @ConfigResource annotation
   │             │      ├─→ Resolve file path (placeholders)
   │             │      ├─→ ConfigBackend.load(path)
   │             │      │      │
   │             │      │      └─→ Configurate: parse YAML → ConfigurationNode tree
   │             │      │
   │             │      ├─→ FieldMapper.mapFields(instance, node)
   │             │      │      │
   │             │      │      ├─→ For each field:
   │             │      │      │   ├─→ Resolve key (@ConfigKey or field name)
   │             │      │      │   ├─→ Get value from node
   │             │      │      │   ├─→ AdapterRegistry.get(type)
   │             │      │      │   ├─→ Deserialize value
   │             │      │      │   └─→ Set field via reflection
   │             │      │      │
   │             │      │      └─→ Handle missing keys (MissingKeyPolicy)
   │             │      │
   │             │      ├─→ CommentResolver.applyComments(node, fields)
   │             │      │      │
   │             │      │      └─→ For each @Comment:
   │             │      │          ├─→ LangContext.get()
   │             │      │          ├─→ Select language entry
   │             │      │          └─→ Set comment on node
   │             │      │
   │             │      └─→ ConfigBackend.save(path, node, comments)
   │             │             │
   │             │             └─→ Configurate: write node tree → YAML file
   │             │
   │             └─→ Cache instance
   │
   └─→ Return instance
```

## Components and Interfaces

### 1. ConfigManager (API Layer)

**Responsibility**: Глобальный entry point для работы с конфигурациями. Управляет жизненным циклом конфигурационных объектов, кэшированием и координацией между компонентами.

**Key Methods**:

```java
public final class ConfigManager {
    // Initialization
    public static void init(Path baseDirectory)
    
    // Backend management
    public static void registerBackend(ConfigFormat format, ConfigBackend backend)
    
    // Config lifecycle
    public static <T> T get(Class<T> configClass)
    public static <T> T reload(Class<T> configClass)
    public static <T> void update(Class<T> configClass, Consumer<T> mutator)
    public static void saveAll()
    
    // Internal
    private static <T> T loadInternal(Class<T> configClass)
    private static void save(Class<?> configClass, Object instance)
}
```

**State**:
- `baseDir: Path` — базовая директория для конфигурационных файлов
- `backends: Map<ConfigFormat, ConfigBackend>` — зарегистрированные backends
- `cache: Map<Class<?>, Object>` — кэш загруженных конфигураций

**Behavior**:
- При первом вызове `get()` загружает конфигурацию и кэширует
- При `reload()` сохраняет текущее состояние, перезагружает из файла и обновляет кэш
- При `update()` применяет мутацию и немедленно сохраняет
- При `saveAll()` сохраняет все кэшированные конфигурации

### 2. ConfigProcessor (Processing Layer)

**Responsibility**: Обработка аннотаций, координация маппинга полей и применения комментариев.

**Key Methods**:

```java
public final class ConfigProcessor {
    public static <T> T load(Class<T> configClass, 
                             ConfigBackend backend, 
                             Path filePath)
    
    public static void save(Object instance, 
                           ConfigBackend backend, 
                           Path filePath)
    
    private static void validateAnnotations(Class<?> configClass)
    private static Path resolvePath(ConfigResource annotation, Path baseDir)
    private static void handleMissingKeys(Object instance, 
                                         Map<String, Object> data, 
                                         MissingKeyPolicy policy)
}
```

**Behavior**:
- Валидирует наличие @ConfigResource
- Разрешает placeholders в пути файла
- Координирует загрузку через backend
- Делегирует маппинг полей в FieldMapper
- Делегирует применение комментариев в CommentResolver
- Обрабатывает политику отсутствующих ключей

### 3. FieldMapper (Processing Layer)

**Responsibility**: Маппинг между Java-полями и YAML-узлами, сериализация/десериализация значений.

**Key Methods**:

```java
public final class FieldMapper {
    public static void mapToInstance(Object instance, 
                                    Map<String, Object> data, 
                                    ConfigBackend backend)
    
    public static Map<String, Object> mapFromInstance(Object instance, 
                                                      ConfigBackend backend)
    
    private static Object deserializeValue(Object rawValue, 
                                          Class<?> targetType)
    
    private static Object serializeValue(Object value, 
                                        Class<?> sourceType)
    
    private static String resolveKey(Field field)
    private static boolean isIgnored(Field field)
}
```

**Behavior**:
- Итерирует по полям класса через reflection
- Пропускает поля с @ConfigIgnore и static поля
- Разрешает ключ через @ConfigKey или использует имя поля
- Проверяет наличие адаптера в AdapterRegistry
- Выполняет автоматическую конвертацию для примитивов, enum, коллекций
- Обрабатывает вложенные структуры (List, Map) рекурсивно

### 4. AdapterRegistry (Processing Layer)

**Responsibility**: Реестр кастомных сериализаторов для сложных типов.

**Key Methods**:

```java
public final class AdapterRegistry {
    public static <T> void register(Class<T> type, ConfigAdapter<T> adapter)
    public static <T> ConfigAdapter<T> get(Class<T> type)
}
```

**State**:
- `adapters: Map<Class<?>, ConfigAdapter<?>>` — зарегистрированные адаптеры

**Behavior**:
- Позволяет регистрировать кастомные адаптеры для любых типов
- Возвращает `null`, если адаптер не зарегистрирован
- Используется FieldMapper для проверки наличия кастомной логики сериализации

### 5. ConfigAdapter<T> (Processing Layer)

**Responsibility**: Интерфейс для кастомной сериализации/десериализации типов.

**Interface**:

```java
public interface ConfigAdapter<T> {
    T fromConfig(Object raw) throws IllegalArgumentException;
    Object toConfig(T value);
}
```

**Usage Example**:

```java
// Adapter for UUID
public class UUIDAdapter implements ConfigAdapter<UUID> {
    @Override
    public UUID fromConfig(Object raw) {
        return UUID.fromString(raw.toString());
    }
    
    @Override
    public Object toConfig(UUID value) {
        return value.toString();
    }
}

// Registration
AdapterRegistry.register(UUID.class, new UUIDAdapter());
```

### 6. CommentResolver (Processing Layer)

**Responsibility**: Выбор языка комментариев и применение их к YAML-узлам.

**Key Methods**:

```java
public final class CommentResolver {
    public static Map<String, List<String>> resolveComments(Class<?> configClass)
    
    private static List<String> selectLanguage(Comment annotation, 
                                              String currentLang)
}
```

**Behavior**:
- Итерирует по полям с @Comment
- Получает текущий язык из LangContext
- Ищет соответствующую языковую запись
- Применяет fallback на "en" или "default", если текущий язык не найден
- Возвращает пустой список, если комментарий отсутствует

### 7. LangContext (Processing Layer)

**Responsibility**: Глобальный контекст текущего языка для многоязычных комментариев.

**Key Methods**:

```java
public final class LangContext {
    public static void set(String languageCode)
    public static String get()
}
```

**State**:
- `current: volatile String` — текущий код языка (thread-safe)

**Behavior**:
- Хранит глобальное состояние языка
- Используется CommentResolver для выбора комментариев
- Используется ConfigProcessor для разрешения {lang} placeholder

### 8. ConfigBackend (Backend Layer)

**Responsibility**: Абстракция над конкретной библиотекой сериализации (Configurate).

**Interface**:

```java
public interface ConfigBackend {
    Map<String, Object> load(Path path) throws IOException;
    
    void save(Path path, 
             Map<String, Object> data, 
             Map<String, List<String>> comments) throws IOException;
    
    default Object get(Map<String, Object> data, String key);
    default void set(Map<String, Object> data, String key, Object value);
}
```

**Implementations**:

#### BukkitYamlBackend (Current Implementation)

**Responsibility**: YAML backend с поддержкой вложенных секций и комментариев.

**Key Features**:
- Использует Bukkit's YamlConfiguration для парсинга
- Поддерживает dotted keys (e.g., "database.host")
- Сохраняет структуру с правильными отступами
- Применяет комментарии к узлам
- Форматирует скаляры с учетом типа (числа без кавычек, строки в кавычках)

**Internal Structure**:
```java
private static class Node {
    Map<String, Node> children;  // Вложенные узлы
    List<String> comments;       // Комментарии для этого узла
    Object value;                // Значение (для листовых узлов)
}
```

#### ConfigurateYamlBackend (Recommended Implementation)

**Responsibility**: YAML backend на основе SpongePowered Configurate.

**Key Features**:
- Использует `YAMLConfigurationLoader` из Configurate
- Работает с `ConfigurationNode` API
- Поддерживает комментарии через `node.comment()`
- Автоматическая обработка типов через Configurate's type system

**Implementation Sketch**:

```java
public class ConfigurateYamlBackend implements ConfigBackend {
    @Override
    public Map<String, Object> load(Path path) throws IOException {
        YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
            .path(path)
            .build();
        
        ConfigurationNode root = loader.load();
        return flattenNode(root);
    }
    
    @Override
    public void save(Path path, 
                    Map<String, Object> data, 
                    Map<String, List<String>> comments) throws IOException {
        YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
            .path(path)
            .build();
        
        ConfigurationNode root = loader.createNode();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String[] path = entry.getKey().split("\\.");
            ConfigurationNode node = root.node((Object[]) path);
            node.set(entry.getValue());
            
            List<String> comment = comments.get(entry.getKey());
            if (comment != null) {
                node.comment(String.join("\n", comment));
            }
        }
        
        loader.save(root);
    }
    
    private Map<String, Object> flattenNode(ConfigurationNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenNode(node, "", result);
        return result;
    }
    
    private void flattenNode(ConfigurationNode node, 
                            String prefix, 
                            Map<String, Object> result) {
        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : 
                 node.childrenMap().entrySet()) {
                String key = prefix.isEmpty() 
                    ? entry.getKey().toString() 
                    : prefix + "." + entry.getKey();
                flattenNode(entry.getValue(), key, result);
            }
        } else {
            result.put(prefix, node.raw());
        }
    }
}
```

### 9. Annotation System

#### @ConfigResource

**Purpose**: Маркирует класс как конфигурационный ресурс.

**Attributes**:
- `path: String` — относительный путь к файлу (поддержка placeholders)
- `format: ConfigFormat` — формат файла (YAML, JSON, TOML)
- `missingKeyPolicy: MissingKeyPolicy` — политика обработки отсутствующих ключей

**Example**:
```java
@ConfigResource(
    path = "config_{lang}.yml",
    format = ConfigFormat.YAML,
    missingKeyPolicy = MissingKeyPolicy.WRITE_DEFAULT
)
public class MyConfig {
    // fields
}
```

#### @ConfigKey

**Purpose**: Переопределяет имя ключа в YAML.

**Attributes**:
- `value: String` — путь ключа (поддержка dotted notation)

**Example**:
```java
@ConfigKey("database.connection.host")
private String dbHost = "localhost";
```

#### @Comment

**Purpose**: Добавляет многоязычные комментарии к полю.

**Attributes**:
- `value: Entry[]` — массив языковых записей

**Nested Annotation**:
```java
@interface Entry {
    String lang();      // Код языка ("en", "ru", "de")
    String[] lines();   // Строки комментария
}
```

**Example**:
```java
@Comment({
    @Comment.Entry(
        lang = "en",
        lines = {"Database host address", "Default: localhost"}
    ),
    @Comment.Entry(
        lang = "ru",
        lines = {"Адрес хоста базы данных", "По умолчанию: localhost"}
    )
})
private String host = "localhost";
```

#### @ConfigIgnore

**Purpose**: Исключает поле из сериализации/десериализации.

**Example**:
```java
@ConfigIgnore
private transient Connection connection;
```

#### @Validate (Proposed)

**Purpose**: Валидация значений после десериализации.

**Attributes**:
- `min: double` — минимальное значение (для чисел)
- `max: double` — максимальное значение (для чисел)
- `pattern: String` — regex паттерн (для строк)
- `required: boolean` — обязательное поле

**Example**:
```java
@Validate(min = 1, max = 65535)
private int port = 3306;

@Validate(pattern = "^[a-zA-Z0-9_]+$", required = true)
private String username;
```

#### @Default (Proposed)

**Purpose**: Явное указание дефолтного значения (альтернатива field initializer).

**Attributes**:
- `value: String` — строковое представление дефолтного значения

**Example**:
```java
@Default("[]")
private List<String> whitelist;
```

#### @Migrate (Proposed)

**Purpose**: Миграция старых ключей в новые.

**Attributes**:
- `from: String` — старый ключ
- `to: String` — новый ключ (опционально, если отличается от текущего)

**Example**:
```java
@ConfigKey("database.host")
@Migrate(from = "db-host")
private String host;
```

## Data Models

### Configuration Node Tree

Внутреннее представление конфигурации в виде дерева узлов:

```
Root Node
├── database (Section Node)
│   ├── host (Scalar Node: "localhost")
│   ├── port (Scalar Node: 3306)
│   └── credentials (Section Node)
│       ├── username (Scalar Node: "admin")
│       └── password (Scalar Node: "secret")
├── features (List Node)
│   ├── [0] (Scalar Node: "feature1")
│   ├── [1] (Scalar Node: "feature2")
│   └── [2] (Scalar Node: "feature3")
└── settings (Map Node)
    ├── "key1" → "value1"
    └── "key2" → "value2"
```

### Type Mapping

| Java Type | YAML Representation | Notes |
|-----------|---------------------|-------|
| `int`, `Integer` | Scalar (number) | No quotes |
| `long`, `Long` | Scalar (number) | No quotes |
| `double`, `Double` | Scalar (number) | No quotes |
| `boolean`, `Boolean` | Scalar (true/false) | No quotes |
| `String` | Scalar (quoted) | Single or double quotes |
| `Enum` | Scalar (name) | Enum constant name |
| `List<T>` | Sequence | Recursive serialization |
| `Map<K, V>` | Mapping | Keys converted to strings |
| `T[]` | Sequence | Converted to List |
| Custom with adapter | Adapter-defined | User-defined format |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. 
Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Annotation recognition
*For any* class annotated with @ConfigResource, calling ConfigManager.get() should successfully load and return an instance without throwing an exception
**Validates: Requirements 1.1**

### Property 2: Custom key mapping
*For any* field annotated with @ConfigKey, the YAML file should contain the specified key name rather than the field name
**Validates: Requirements 1.2**

### Property 3: Default key mapping
*For any* field without @ConfigKey annotation, the YAML file should contain a key matching the field name exactly
**Validates: Requirements 1.3**

### Property 4: Ignored field exclusion
*For any* field annotated with @ConfigIgnore, the field should never appear in the saved YAML file and should never be modified during loading
**Validates: Requirements 1.4**

### Property 5: Static field exclusion
*For any* static field in a config class, the field should never appear in the saved YAML file
**Validates: Requirements 1.5**

### Property 6: File generation on first load
*For any* config class, if the file does not exist when loading, a new file should be created containing all field default values
**Validates: Requirements 2.1**

### Property 7: Missing key handling with WRITE_DEFAULT
*For any* config with MissingKeyPolicy.WRITE_DEFAULT, when a key is missing from the file, the file should be updated to include that key with the field's default value
**Validates: Requirements 2.2, 2.3**

### Property 8: Missing key handling with USE_FIELD_DEFAULT
*For any* config with MissingKeyPolicy.USE_FIELD_DEFAULT, when a key is missing from the file, the field should have its default value but the file should remain unchanged
**Validates: Requirements 2.4**

### Property 9: Missing key handling with ERROR
*For any* config with MissingKeyPolicy.ERROR, when a key is missing from the file, loading should throw an IllegalStateException
**Validates: Requirements 2.5**

### Property 10: Language-specific comment selection
*For any* field with @Comment containing multiple language entries, the saved YAML should contain the comment matching the current LangContext value
**Validates: Requirements 3.1**

### Property 11: Comment fallback to default language
*For any* field with @Comment that lacks an entry for the current language, the saved YAML should contain the "en" or "default" language comment
**Validates: Requirements 3.2**

### Property 12: Comment persistence round-trip
*For any* config with comments, saving and then loading the file should preserve all comments in the file content
**Validates: Requirements 3.4**

### Property 13: Dynamic comment language switching
*For any* config with multi-language comments, changing LangContext and reloading should result in comments in the new language
**Validates: Requirements 3.5**

### Property 14: Placeholder resolution in paths
*For any* @ConfigResource path containing {lang}, the resolved file path should contain the current LangContext value instead of the placeholder
**Validates: Requirements 4.1**

### Property 15: Multiple placeholder resolution
*For any* path containing multiple placeholders, all placeholders should be resolved before file access
**Validates: Requirements 4.2**

### Property 16: Dynamic path resolution on language change
*For any* config with {lang} placeholder, changing LangContext should cause subsequent operations to use a different file path
**Validates: Requirements 4.4**

### Property 17: Adapter usage for registered types
*For any* type with a registered ConfigAdapter, serialization and deserialization should use the adapter's toConfig and fromConfig methods
**Validates: Requirements 5.1**

### Property 18: Default serialization fallback
*For any* type without a registered adapter, the system should successfully serialize and deserialize using default type inspection
**Validates: Requirements 5.4**

### Property 19: Config instance caching
*For any* config class, calling ConfigManager.get() multiple times should return the same instance
**Validates: Requirements 6.1**

### Property 20: Initial load and cache
*For any* config class not in cache, calling ConfigManager.get() should load from file and cache the instance
**Validates: Requirements 6.2**

### Property 21: Reload saves and updates cache
*For any* cached config, calling ConfigManager.reload() should save current state, reload from file, and update the cache with the new instance
**Validates: Requirements 6.3**

### Property 22: Atomic update and save
*For any* config, calling ConfigManager.update() with a mutator should apply the mutation and immediately save to file
**Validates: Requirements 6.4**

### Property 23: Batch save all configs
*For any* set of cached configs, calling ConfigManager.saveAll() should save all instances to their respective files
**Validates: Requirements 6.5**

### Property 24: List serialization format
*For any* List field, the saved YAML should contain a valid YAML sequence (list) structure
**Validates: Requirements 7.1**

### Property 25: Map serialization format
*For any* Map field, the saved YAML should contain a valid YAML mapping (map) structure
**Validates: Requirements 7.2**

### Property 26: Recursive nested serialization
*For any* List or Map containing complex objects, all nested levels should be serialized recursively
**Validates: Requirements 7.3**

### Property 27: Single value to list normalization
*For any* List field, if the YAML contains a single scalar value, deserialization should wrap it in a list
**Validates: Requirements 7.4**

### Property 28: Map structure preservation round-trip
*For any* Map field, serializing and then deserializing should preserve the key-value structure
**Validates: Requirements 7.5**

### Property 29: Nested key creation
*For any* dotted key (e.g., "database.host"), the backend should create all intermediate nodes in the YAML structure
**Validates: Requirements 8.5**

### Property 30: Backend routing by format
*For any* config with a specified ConfigFormat, the ConfigManager should use the backend registered for that format
**Validates: Requirements 10.1**

### Property 31: Backend registration and usage
*For any* newly registered backend, subsequent config loads with that format should use the new backend
**Validates: Requirements 10.3**

### Property 32: Multi-format support
*For any* set of configs with different formats, each should be correctly routed to its corresponding backend
**Validates: Requirements 10.4**

### Property 33: Backend replacement
*For any* format, registering a new backend should cause all subsequent operations to use the new implementation
**Validates: Requirements 10.5**

### Property 34: Primitive type conversion
*For any* primitive field (int, long, boolean, double), YAML values should be automatically converted to the correct type
**Validates: Requirements 11.1**

### Property 35: Boxed primitive null handling
*For any* boxed primitive field (Integer, Long, Boolean, Double), null values in YAML should be correctly handled
**Validates: Requirements 11.2**

### Property 36: String conversion
*For any* String field, YAML values should be converted to String regardless of their original type
**Validates: Requirements 11.3**

### Property 37: Enum deserialization by name
*For any* enum field, YAML values should be deserialized by matching the enum constant name
**Validates: Requirements 11.4**

### Property 38: Validation invocation
*For any* field annotated with @Validate, validation logic should be invoked after deserialization
**Validates: Requirements 12.1**

### Property 39: Range validation
*For any* numeric field with min/max constraints, values outside the range should be rejected
**Validates: Requirements 12.3**

### Property 40: Pattern validation
*For any* String field with a pattern constraint, values not matching the regex should be rejected
**Validates: Requirements 12.4**

## Error Handling

### Error Categories

1. **Configuration Errors** (IllegalStateException)
   - Missing @ConfigResource annotation
   - ConfigManager.init() not called
   - No backend registered for format
   - Missing required keys (with ERROR policy)

2. **I/O Errors** (RuntimeException wrapping IOException)
   - Cannot read config file
   - Cannot write config file
   - Cannot create parent directories

3. **Reflection Errors** (RuntimeException)
   - Cannot instantiate config class (no no-args constructor)
   - Cannot access field (private, final)
   - Cannot set field value

4. **Type Conversion Errors** (RuntimeException)
   - Cannot convert YAML value to field type
   - Invalid enum constant name
   - Adapter throws exception

5. **Validation Errors** (ValidationException)
   - Value outside min/max range
   - String doesn't match pattern
   - Required field is null

### Error Message Format

All error messages should follow this format:
```
[Component] Error description: details
Context: field=<fieldName>, class=<className>, file=<filePath>
Caused by: <original exception>
```

Example:
```
[ConfigManager] Cannot load config file: config.yml
Context: class=xyz.example.MyConfig, file=/plugins/MyPlugin/config.yml
Caused by: java.io.FileNotFoundException: /plugins/MyPlugin/config.yml (Access denied)
```

## Testing Strategy

### Unit Testing

Unit tests will cover:
- Annotation processing logic (resolveKey, isIgnored)
- Type conversion (primitives, enums, collections)
- Comment resolution (language selection, fallback)
- Placeholder resolution ({lang} replacement)
- Error message formatting

### Property-Based Testing

Property-based tests will verify universal properties using a PBT library (e.g., jqwik for Java):

**Test Configuration**:
- Minimum 100 iterations per property
- Random generation of config classes, field values, and YAML structures
- Edge cases: empty collections, null values, special characters

**Property Test Examples**:

1. **Round-trip serialization**: For any config instance, serialize → deserialize should produce an equivalent instance
2. **Key mapping consistency**: For any field, the key in YAML should match either @ConfigKey value or field name
3. **Cache consistency**: For any config class, multiple get() calls should return the same instance
4. **Comment language selection**: For any language code, comments should match that language or fallback to default

**Property Test Tagging**:
Each property-based test must include a comment tag:
```java
// Feature: configurate-wrapper-architecture, Property 1: Annotation recognition
@Property
void testAnnotationRecognition(@ForAll ConfigClass config) {
    // test implementation
}
```

### Integration Testing

Integration tests will verify:
- End-to-end config loading and saving with real files
- Configurate integration (actual YAML parsing)
- Multi-config scenarios with different formats
- Language switching and file path changes

## Lifecycle

### Initialization Phase

```
1. Application starts
2. ConfigManager.init(baseDirectory) is called
3. Backends are registered (YAML, JSON, HOCON)
4. Adapters are registered for custom types
5. LangContext.set(languageCode) is called
```

### Config Loading Phase

```
1. User calls ConfigManager.get(MyConfig.class)
2. ConfigManager checks cache
   ├─→ If cached: return instance
   └─→ If not cached:
       3. Validate @ConfigResource annotation
       4. Resolve file path (replace placeholders)
       5. Check if file exists
          ├─→ If exists: load via backend
          └─→ If not exists: create empty node
       6. Create config instance via reflection
       7. For each field:
          ├─→ Resolve key (@ConfigKey or field name)
          ├─→ Get value from node
          ├─→ Check for adapter
          ├─→ Deserialize value
          ├─→ Set field via reflection
          └─→ Handle missing keys (policy)
       8. Resolve comments for current language
       9. Save to file (with comments)
       10. Cache instance
       11. Return instance
```

### Config Update Phase

```
1. User calls ConfigManager.update(MyConfig.class, mutator)
2. Get cached instance
3. Apply mutator
4. For each field:
   ├─→ Resolve key
   ├─→ Get field value
   ├─→ Check for adapter
   ├─→ Serialize value
   └─→ Set value in node
5. Resolve comments for current language
6. Save to file via backend
```

### Config Reload Phase

```
1. User calls ConfigManager.reload(MyConfig.class)
2. Get cached instance
3. Save current state to file
4. Load fresh instance from file (same as loading phase)
5. Update cache with new instance
6. Return new instance
```

## API Examples

### Basic Usage

```java
// 1. Define config class
@ConfigResource(path = "config.yml")
public class MyConfig {
    @Comment({
        @Comment.Entry(lang = "en", lines = {"Server port"}),
        @Comment.Entry(lang = "ru", lines = {"Порт сервера"})
    })
    private int port = 8080;
    
    @ConfigKey("database.host")
    private String dbHost = "localhost";
    
    @ConfigIgnore
    private transient Connection connection;
}

// 2. Initialize
ConfigManager.init(Paths.get("./config"));
LangContext.set("en");

// 3. Load config
MyConfig config = ConfigManager.get(MyConfig.class);
System.out.println(config.port); // 8080

// 4. Update config
ConfigManager.update(MyConfig.class, cfg -> {
    cfg.port = 9090;
});

// 5. Reload config
config = ConfigManager.reload(MyConfig.class);
```

### Multi-Language Support

```java
// Switch language
LangContext.set("ru");

// Reload to apply new comments
MyConfig config = ConfigManager.reload(MyConfig.class);
// Comments in config.yml are now in Russian
```

### Custom Adapter

```java
// Define adapter
public class UUIDAdapter implements ConfigAdapter<UUID> {
    @Override
    public UUID fromConfig(Object raw) {
        return UUID.fromString(raw.toString());
    }
    
    @Override
    public Object toConfig(UUID value) {
        return value.toString();
    }
}

// Register adapter
AdapterRegistry.register(UUID.class, new UUIDAdapter());

// Use in config
@ConfigResource(path = "players.yml")
public class PlayerConfig {
    private UUID playerId; // Will use UUIDAdapter
}
```

### Multiple Formats

```java
@ConfigResource(path = "settings.json", format = ConfigFormat.JSON)
public class JsonConfig {
    private String setting = "value";
}

@ConfigResource(path = "data.conf", format = ConfigFormat.HOCON)
public class HoconConfig {
    private int value = 42;
}

// Register backends
ConfigManager.registerBackend(ConfigFormat.JSON, new ConfigurateJsonBackend());
ConfigManager.registerBackend(ConfigFormat.HOCON, new ConfigurateHoconBackend());

// Load configs
JsonConfig json = ConfigManager.get(JsonConfig.class);
HoconConfig hocon = ConfigManager.get(HoconConfig.class);
```

### Validation (Proposed Feature)

```java
@ConfigResource(path = "server.yml")
public class ServerConfig {
    @Validate(min = 1, max = 65535)
    private int port = 25565;
    
    @Validate(pattern = "^[a-zA-Z0-9_]+$", required = true)
    private String serverName;
    
    @Validate(required = true)
    private String motd = "Welcome!";
}

// Loading will throw ValidationException if constraints are violated
```

## Migration from Current Implementation

### Key Changes

1. **Backend Layer**: Replace BukkitYamlBackend with ConfigurateYamlBackend
2. **Node Handling**: Use ConfigurationNode API instead of custom Node class
3. **Comment System**: Use Configurate's built-in comment support
4. **Type Serialization**: Leverage Configurate's TypeSerializer system
5. **Path Resolution**: Use Configurate's node path API for nested keys

### Migration Steps

1. Add Configurate dependency to build.gradle.kts
2. Create ConfigurateYamlBackend implementing ConfigBackend
3. Refactor ConfigManager to use ConfigurationNode
4. Update FieldMapper to work with ConfigurationNode
5. Implement ConfigurateJsonBackend and ConfigurateHoconBackend
6. Add validation support with @Validate annotation
7. Write comprehensive test suite (unit + property-based)
8. Update documentation and examples

### Backward Compatibility

To maintain backward compatibility during migration:
- Keep BukkitYamlBackend as deprecated option
- Provide migration utility to convert old configs
- Support both backends simultaneously during transition period

## Constraints and Limitations

### Technical Constraints

1. **Reflection Limitations**
   - Config classes must have a public no-args constructor
   - Fields must be accessible (not final)
   - Generic type information is erased at runtime (use adapters for complex generics)

2. **YAML Limitations**
   - Keys must be strings (Map keys are converted to strings)
   - Circular references are not supported
   - Some Java types require custom adapters (e.g., UUID, LocalDateTime)

3. **Performance Considerations**
   - Reflection is slower than direct access
   - Large configs may have noticeable load times
   - Caching mitigates repeated load overhead

### Design Limitations

1. **Single File Per Config**
   - Each @ConfigResource maps to exactly one file
   - No built-in support for config splitting across multiple files

2. **Flat Field Structure**
   - Nested objects require custom adapters
   - No automatic mapping of nested classes to YAML sections

3. **Static Language Context**
   - LangContext is global, not per-config
   - Changing language affects all configs

### Future Enhancements

1. **Nested Object Support**: Automatic mapping of nested classes to YAML sections
2. **Config Inheritance**: Support for extending base configs
3. **Hot Reload**: Watch file system for changes and auto-reload
4. **Config Versioning**: Automatic migration between config versions
5. **Schema Validation**: JSON Schema or similar for config validation
6. **Config Profiles**: Support for dev/staging/prod profiles

