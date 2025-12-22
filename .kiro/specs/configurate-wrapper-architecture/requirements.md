# Requirements Document

## Introduction

Данная спецификация описывает архитектуру Java-библиотеки конфигурации, которая работает поверх SpongePowered Configurate (YAML backend). Библиотека предоставляет удобное декларативное API на основе аннотаций для работы с конфигурационными файлами, автоматическую генерацию недостающих ключей, поддержку многоязычных комментариев, кастомных сериализаторов и гибкую политику обработки отсутствующих данных.

## Glossary

- **ConfigManager**: Глобальный менеджер конфигураций, предоставляющий API для загрузки, получения, перезагрузки и сохранения конфигурационных объектов.
- **ConfigBackend**: Абстракция над конкретной библиотекой сериализации (например, SpongePowered Configurate), отвечающая за чтение/запись YAML-файлов.
- **ConfigResource**: Класс, помеченный аннотацией @ConfigResource, представляющий конфигурационный файл.
- **ConfigProcessor**: Компонент, обрабатывающий аннотации и выполняющий маппинг между Java-полями и YAML-узлами.
- **FieldMapper**: Компонент, отвечающий за преобразование Java-полей в YAML-узлы и обратно.
- **TypeRegistry/AdapterRegistry**: Реестр кастомных сериализаторов для сложных типов данных.
- **CommentResolver**: Компонент, выбирающий язык комментариев и применяющий их к YAML-узлам.
- **LangContext**: Контекст текущего языка для многоязычных комментариев.
- **MissingKeyPolicy**: Политика обработки отсутствующих ключей в конфигурационном файле.
- **ConfigAdapter**: Интерфейс для кастомной сериализации/десериализации типов данных.
- **Placeholder**: Динамическая подстановка в пути файла (например, {lang}).

## Requirements

### Requirement 1

**User Story:** Как разработчик плагина, я хочу объявлять конфигурационные классы с помощью аннотаций, чтобы автоматически связывать Java-поля с YAML-ключами без написания boilerplate-кода.

#### Acceptance Criteria

1. WHEN a class is annotated with @ConfigResource THEN the ConfigManager SHALL recognize it as a configuration resource and enable automatic loading
2. WHEN a field is annotated with @ConfigKey THEN the ConfigManager SHALL map that field to the specified YAML path instead of using the field name
3. WHEN a field has no @ConfigKey annotation THEN the ConfigManager SHALL use the field name as the YAML key by default
4. WHEN a field is annotated with @ConfigIgnore THEN the ConfigManager SHALL exclude that field from serialization and deserialization
5. WHEN a static field exists in a config class THEN the ConfigManager SHALL automatically ignore it during processing

### Requirement 2

**User Story:** Как разработчик плагина, я хочу автоматически генерировать конфигурационные файлы с дефолтными значениями, чтобы пользователи получали готовый к редактированию файл при первом запуске.

#### Acceptance Criteria

1. WHEN a config file does not exist and a config class is loaded THEN the ConfigManager SHALL create the file with default field values
2. WHEN a config file exists but lacks some keys THEN the ConfigManager SHALL add missing keys with default values according to the MissingKeyPolicy
3. WHEN MissingKeyPolicy is WRITE_DEFAULT and a key is missing THEN the ConfigManager SHALL write the field's default value to the file
4. WHEN MissingKeyPolicy is USE_FIELD_DEFAULT and a key is missing THEN the ConfigManager SHALL use the field's default value without writing to file
5. WHEN MissingKeyPolicy is ERROR and a key is missing THEN the ConfigManager SHALL throw an exception

### Requirement 3

**User Story:** Как разработчик плагина, я хочу добавлять многоязычные комментарии к конфигурационным полям, чтобы пользователи на разных языках понимали назначение каждого параметра.

#### Acceptance Criteria

1. WHEN a field is annotated with @Comment containing multiple language entries THEN the ConfigManager SHALL select the comment matching the current LangContext
2. WHEN the current language has no matching comment entry THEN the ConfigManager SHALL fall back to the "en" or "default" language entry
3. WHEN no fallback comment exists THEN the ConfigManager SHALL write the field without comments
4. WHEN a comment is applied to a YAML node THEN the ConfigBackend SHALL preserve the comment in the output file
5. WHEN the LangContext changes and config is reloaded THEN the ConfigManager SHALL apply comments in the new language

### Requirement 4

**User Story:** Как разработчик плагина, я хочу использовать placeholders в путях конфигурационных файлов, чтобы поддерживать динамические имена файлов (например, для разных языков).

#### Acceptance Criteria

1. WHEN @ConfigResource path contains {lang} placeholder THEN the ConfigManager SHALL replace it with the current LangContext value
2. WHEN multiple placeholders exist in the path THEN the ConfigManager SHALL resolve all of them before accessing the file
3. WHEN a placeholder cannot be resolved THEN the ConfigManager SHALL throw an exception with a clear error message
4. WHEN the LangContext changes THEN subsequent config operations SHALL use the new resolved path

### Requirement 5

**User Story:** Как разработчик плагина, я хочу регистрировать кастомные сериализаторы для сложных типов, чтобы корректно сохранять и загружать нестандартные объекты.

#### Acceptance Criteria

1. WHEN a ConfigAdapter is registered for a type THEN the ConfigManager SHALL use it for serialization and deserialization of that type
2. WHEN serializing a value with a registered adapter THEN the ConfigManager SHALL call the adapter's toConfig method
3. WHEN deserializing a value with a registered adapter THEN the ConfigManager SHALL call the adapter's fromConfig method
4. WHEN no adapter is registered for a type THEN the ConfigManager SHALL attempt default serialization based on type inspection
5. WHEN an adapter throws an exception THEN the ConfigManager SHALL propagate it with context about the field and config class

### Requirement 6

**User Story:** Как разработчик плагина, я хочу получать конфигурационные объекты через простой API, чтобы минимизировать код доступа к настройкам.

#### Acceptance Criteria

1. WHEN ConfigManager.get(Class) is called THEN the ConfigManager SHALL return a cached instance if available
2. WHEN no cached instance exists THEN the ConfigManager SHALL load the config from file and cache it
3. WHEN ConfigManager.reload(Class) is called THEN the ConfigManager SHALL save current state, reload from file, and update the cache
4. WHEN ConfigManager.update(Class, Consumer) is called THEN the ConfigManager SHALL apply the mutation and immediately save the config
5. WHEN ConfigManager.saveAll() is called THEN the ConfigManager SHALL save all cached config instances to their respective files

### Requirement 7

**User Story:** Как разработчик плагины, я хочу, чтобы библиотека корректно обрабатывала коллекции и вложенные структуры, чтобы сохранять сложные конфигурации.

#### Acceptance Criteria

1. WHEN a field is of type List THEN the ConfigManager SHALL serialize it as a YAML list
2. WHEN a field is of type Map THEN the ConfigManager SHALL serialize it as a YAML map
3. WHEN a List or Map contains complex objects THEN the ConfigManager SHALL recursively serialize nested elements
4. WHEN deserializing a List field and the YAML contains a single value THEN the ConfigManager SHALL wrap it in a list
5. WHEN deserializing a Map field THEN the ConfigManager SHALL preserve key-value structure

### Requirement 8

**User Story:** Как разработчик плагина, я хочу, чтобы библиотека использовала SpongePowered Configurate как backend, чтобы не писать собственную YAML-систему и использовать проверенное решение.

#### Acceptance Criteria

1. WHEN ConfigBackend loads a file THEN it SHALL delegate to Configurate's ConfigurationLoader
2. WHEN ConfigBackend saves a file THEN it SHALL use Configurate's node system to write data
3. WHEN ConfigBackend applies comments THEN it SHALL use Configurate's comment API
4. WHEN ConfigBackend retrieves a nested key THEN it SHALL use Configurate's node path resolution
5. WHEN ConfigBackend sets a nested key THEN it SHALL create intermediate nodes using Configurate's API

### Requirement 9

**User Story:** Как разработчик плагина, я хочу, чтобы библиотека обрабатывала ошибки с понятными сообщениями, чтобы быстро находить проблемы в конфигурации.

#### Acceptance Criteria

1. WHEN a config class lacks @ConfigResource annotation THEN the ConfigManager SHALL throw IllegalStateException with the class name
2. WHEN ConfigManager.init() is not called before loading configs THEN the ConfigManager SHALL throw IllegalStateException
3. WHEN a config file cannot be read THEN the ConfigManager SHALL throw RuntimeException with the file path and cause
4. WHEN a config file cannot be written THEN the ConfigManager SHALL throw RuntimeException with the file path and cause
5. WHEN a field cannot be set via reflection THEN the ConfigManager SHALL throw RuntimeException with the field name and cause
6. WHEN a config class has no no-args constructor THEN the ConfigManager SHALL throw RuntimeException during instantiation
7. WHEN type conversion fails THEN the ConfigManager SHALL throw RuntimeException with the field name and expected type

### Requirement 10

**User Story:** Как разработчик плагина, я хочу, чтобы библиотека поддерживала различные форматы конфигурационных файлов, чтобы в будущем можно было добавить JSON, HOCON и другие форматы.

#### Acceptance Criteria

1. WHEN a ConfigFormat is specified in @ConfigResource THEN the ConfigManager SHALL use the corresponding registered backend
2. WHEN no backend is registered for a format THEN the ConfigManager SHALL throw IllegalStateException
3. WHEN a new backend is registered via ConfigManager.registerBackend THEN subsequent config loads SHALL use the new backend
4. WHEN multiple formats are used in different configs THEN the ConfigManager SHALL correctly route each to its backend
5. WHEN a backend is replaced THEN the ConfigManager SHALL use the new implementation for that format

### Requirement 11

**User Story:** Как разработчик плагина, я хочу, чтобы библиотека поддерживала примитивные типы и enum, чтобы не писать адаптеры для базовых типов.

#### Acceptance Criteria

1. WHEN a field is a primitive type (int, long, boolean, double) THEN the ConfigManager SHALL automatically convert YAML values
2. WHEN a field is a boxed primitive (Integer, Long, Boolean, Double) THEN the ConfigManager SHALL handle null values correctly
3. WHEN a field is a String THEN the ConfigManager SHALL convert YAML values to String
4. WHEN a field is an enum THEN the ConfigManager SHALL deserialize by enum name
5. WHEN an enum value in YAML is invalid THEN the ConfigManager SHALL throw an exception with valid enum values

### Requirement 12

**User Story:** Как разработчик плагина, я хочу иметь возможность валидировать конфигурационные значения, чтобы предотвратить некорректные настройки.

#### Acceptance Criteria

1. WHEN a field is annotated with @Validate THEN the ConfigManager SHALL invoke the validation logic after deserialization
2. WHEN validation fails THEN the ConfigManager SHALL throw a ValidationException with the field name and reason
3. WHEN a field has min/max constraints THEN the ConfigManager SHALL verify numeric values are within range
4. WHEN a field has a pattern constraint THEN the ConfigManager SHALL verify String values match the regex
5. WHEN a field is marked as required THEN the ConfigManager SHALL throw an exception if the value is null after loading
