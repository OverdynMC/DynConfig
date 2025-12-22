# Implementation Plan

- [ ] 1. Setup project dependencies and structure
  - Add SpongePowered Configurate dependencies to build.gradle.kts (configurate-yaml, configurate-core)
  - Remove Bukkit YAML dependency
  - Update package structure if needed
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 2. Implement ConfigurateYamlBackend
  - [ ] 2.1 Create ConfigurateYamlBackend class implementing ConfigBackend
    - Implement load() method using YAMLConfigurationLoader
    - Implement save() method with ConfigurationNode tree
    - Implement flattenNode() helper for converting ConfigurationNode to Map
    - Implement buildNode() helper for converting Map to ConfigurationNode
    - _Requirements: 8.1, 8.2, 8.5_

  - [ ] 2.2 Implement comment support in ConfigurateYamlBackend
    - Use ConfigurationNode.comment() API to apply comments
    - Preserve comments during save operations
    - _Requirements: 3.4, 8.3_

  - [ ]* 2.3 Write property test for ConfigurateYamlBackend
    - **Property 29: Nested key creation**
    - **Validates: Requirements 8.5**

  - [ ]* 2.4 Write property test for comment persistence
    - **Property 12: Comment persistence round-trip**
    - **Validates: Requirements 3.4**

- [ ] 3. Refactor ConfigManager to use new backend
  - [ ] 3.1 Update loadInternal() to work with ConfigurationNode
    - Replace Map<String, Object> with ConfigurationNode handling
    - Update backend.load() calls
    - _Requirements: 1.1, 6.2_

  - [ ] 3.2 Update save() to work with ConfigurationNode
    - Replace Map<String, Object> with ConfigurationNode handling
    - Update backend.save() calls
    - _Requirements: 6.4_

  - [ ] 3.3 Update backend registration to use ConfigurateYamlBackend by default
    - Replace BukkitYamlBackend with ConfigurateYamlBackend in static initializer
    - _Requirements: 10.1_

  - [ ]* 3.4 Write property test for caching behavior
    - **Property 19: Config instance caching**
    - **Validates: Requirements 6.1**

  - [ ]* 3.5 Write property test for reload behavior
    - **Property 21: Reload saves and updates cache**
    - **Validates: Requirements 6.3**

- [ ] 4. Refactor FieldMapper for ConfigurationNode
  - [ ] 4.1 Update deserializeValue() to handle ConfigurationNode values
    - Work with ConfigurationNode instead of raw Object
    - Use Configurate's type system for conversions
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ] 4.2 Update serializeValue() to produce ConfigurationNode-compatible values
    - Ensure values are compatible with Configurate's serialization
    - Handle nested structures (List, Map) properly
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ] 4.3 Implement nested key support using ConfigurationNode paths
    - Use node.node(Object... path) for dotted keys
    - Create intermediate nodes automatically
    - _Requirements: 8.5_

  - [ ]* 4.4 Write property test for key mapping
    - **Property 2: Custom key mapping**
    - **Validates: Requirements 1.2**

  - [ ]* 4.5 Write property test for default key mapping
    - **Property 3: Default key mapping**
    - **Validates: Requirements 1.3**

  - [ ]* 4.6 Write property test for nested key creation
    - **Property 29: Nested key creation**
    - **Validates: Requirements 8.5**

- [ ] 5. Implement CommentResolver with language selection
  - [ ] 5.1 Create CommentResolver class
    - Implement resolveComments() to extract comments from fields
    - Implement selectLanguage() with fallback logic
    - _Requirements: 3.1, 3.2, 3.3_

  - [ ] 5.2 Integrate CommentResolver into ConfigManager
    - Call CommentResolver during load and save operations
    - Pass resolved comments to backend
    - _Requirements: 3.4, 3.5_

  - [ ]* 5.3 Write property test for language selection
    - **Property 10: Language-specific comment selection**
    - **Validates: Requirements 3.1**

  - [ ]* 5.4 Write property test for fallback logic
    - **Property 11: Comment fallback to default language**
    - **Validates: Requirements 3.2**

  - [ ]* 5.5 Write property test for dynamic language switching
    - **Property 13: Dynamic comment language switching**
    - **Validates: Requirements 3.5**

- [ ] 6. Implement placeholder resolution in ConfigProcessor
  - [ ] 6.1 Create ConfigProcessor class
    - Implement resolvePath() with placeholder replacement
    - Support {lang} and other placeholders
    - _Requirements: 4.1, 4.2_

  - [ ] 6.2 Add error handling for unresolvable placeholders
    - Throw exception with clear message
    - _Requirements: 4.3, 9.1_

  - [ ]* 6.3 Write property test for placeholder resolution
    - **Property 14: Placeholder resolution in paths**
    - **Validates: Requirements 4.1**

  - [ ]* 6.4 Write property test for multiple placeholders
    - **Property 15: Multiple placeholder resolution**
    - **Validates: Requirements 4.2**

  - [ ]* 6.5 Write property test for dynamic path resolution
    - **Property 16: Dynamic path resolution on language change**
    - **Validates: Requirements 4.4**

- [ ] 7. Implement MissingKeyPolicy handling
  - [ ] 7.1 Update loadInternal() to handle WRITE_DEFAULT policy
    - Write missing keys with default values to file
    - _Requirements: 2.2, 2.3_

  - [ ] 7.2 Update loadInternal() to handle USE_FIELD_DEFAULT policy
    - Use field defaults without modifying file
    - _Requirements: 2.4_

  - [ ] 7.3 Update loadInternal() to handle ERROR policy
    - Throw IllegalStateException for missing keys
    - _Requirements: 2.5_

  - [ ]* 7.4 Write property test for WRITE_DEFAULT policy
    - **Property 7: Missing key handling with WRITE_DEFAULT**
    - **Validates: Requirements 2.2, 2.3**

  - [ ]* 7.5 Write property test for USE_FIELD_DEFAULT policy
    - **Property 8: Missing key handling with USE_FIELD_DEFAULT**
    - **Validates: Requirements 2.4**

  - [ ]* 7.6 Write property test for ERROR policy
    - **Property 9: Missing key handling with ERROR**
    - **Validates: Requirements 2.5**

- [ ] 8. Implement AdapterRegistry and ConfigAdapter system
  - [ ] 8.1 Verify AdapterRegistry implementation
    - Ensure register() and get() methods work correctly
    - _Requirements: 5.1_

  - [ ] 8.2 Integrate adapters into FieldMapper
    - Check for adapters before default serialization
    - Call toConfig() and fromConfig() appropriately
    - _Requirements: 5.2, 5.3, 5.4_

  - [ ] 8.3 Add error handling for adapter exceptions
    - Wrap adapter exceptions with context
    - _Requirements: 5.5_

  - [ ]* 8.4 Write property test for adapter usage
    - **Property 17: Adapter usage for registered types**
    - **Validates: Requirements 5.1**

  - [ ]* 8.5 Write property test for default serialization fallback
    - **Property 18: Default serialization fallback**
    - **Validates: Requirements 5.4**

- [ ] 9. Implement collection and nested structure handling
  - [ ] 9.1 Update serializeValue() for List handling
    - Serialize lists as YAML sequences
    - Handle nested elements recursively
    - _Requirements: 7.1, 7.3_

  - [ ] 9.2 Update serializeValue() for Map handling
    - Serialize maps as YAML mappings
    - Convert keys to strings
    - Handle nested values recursively
    - _Requirements: 7.2, 7.3_

  - [ ] 9.3 Update deserializeValue() for List normalization
    - Wrap single values in lists when field type is List
    - _Requirements: 7.4_

  - [ ] 9.4 Update deserializeValue() for Map preservation
    - Preserve key-value structure during deserialization
    - _Requirements: 7.5_

  - [ ]* 9.5 Write property test for List serialization
    - **Property 24: List serialization format**
    - **Validates: Requirements 7.1**

  - [ ]* 9.6 Write property test for Map serialization
    - **Property 25: Map serialization format**
    - **Validates: Requirements 7.2**

  - [ ]* 9.7 Write property test for recursive nested serialization
    - **Property 26: Recursive nested serialization**
    - **Validates: Requirements 7.3**

  - [ ]* 9.8 Write property test for single value to list normalization
    - **Property 27: Single value to list normalization**
    - **Validates: Requirements 7.4**

  - [ ]* 9.9 Write property test for map structure preservation
    - **Property 28: Map structure preservation round-trip**
    - **Validates: Requirements 7.5**

- [ ] 10. Implement primitive and enum type handling
  - [ ] 10.1 Update convertPrimitive() for all primitive types
    - Handle int, long, double, boolean conversions
    - _Requirements: 11.1_

  - [ ] 10.2 Add null handling for boxed primitives
    - Check for null before conversion
    - _Requirements: 11.2_

  - [ ] 10.3 Add String conversion logic
    - Convert any value to String via toString()
    - _Requirements: 11.3_

  - [ ] 10.4 Add enum deserialization by name
    - Use Enum.valueOf() with proper error handling
    - _Requirements: 11.4_

  - [ ] 10.5 Add error handling for invalid enum values
    - Throw exception with valid enum values listed
    - _Requirements: 11.5_

  - [ ]* 10.6 Write property test for primitive conversion
    - **Property 34: Primitive type conversion**
    - **Validates: Requirements 11.1**

  - [ ]* 10.7 Write property test for boxed primitive null handling
    - **Property 35: Boxed primitive null handling**
    - **Validates: Requirements 11.2**

  - [ ]* 10.8 Write property test for String conversion
    - **Property 36: String conversion**
    - **Validates: Requirements 11.3**

  - [ ]* 10.9 Write property test for enum deserialization
    - **Property 37: Enum deserialization by name**
    - **Validates: Requirements 11.4**

- [ ] 11. Implement @Validate annotation and validation system
  - [ ] 11.1 Create @Validate annotation
    - Add min, max, pattern, required attributes
    - _Requirements: 12.1_

  - [ ] 11.2 Create ValidationException class
    - Include field name and reason in exception
    - _Requirements: 12.2_

  - [ ] 11.3 Create Validator class
    - Implement validateField() method
    - Check min/max for numeric types
    - Check pattern for String types
    - Check required for null values
    - _Requirements: 12.3, 12.4, 12.5_

  - [ ] 11.4 Integrate validation into loadInternal()
    - Call Validator.validateField() after deserialization
    - _Requirements: 12.1_

  - [ ]* 11.5 Write property test for validation invocation
    - **Property 38: Validation invocation**
    - **Validates: Requirements 12.1**

  - [ ]* 11.6 Write property test for range validation
    - **Property 39: Range validation**
    - **Validates: Requirements 12.3**

  - [ ]* 11.7 Write property test for pattern validation
    - **Property 40: Pattern validation**
    - **Validates: Requirements 12.4**

- [ ] 12. Implement multi-format backend support
  - [ ] 12.1 Create ConfigurateJsonBackend
    - Use JSONConfigurationLoader from Configurate
    - Implement load() and save() methods
    - _Requirements: 10.1, 10.3_

  - [ ] 12.2 Create ConfigurateHoconBackend
    - Use HoconConfigurationLoader from Configurate
    - Implement load() and save() methods
    - _Requirements: 10.1, 10.3_

  - [ ] 12.3 Update ConfigManager to support backend routing
    - Ensure correct backend is used based on ConfigFormat
    - _Requirements: 10.1, 10.4_

  - [ ]* 12.4 Write property test for backend routing
    - **Property 30: Backend routing by format**
    - **Validates: Requirements 10.1**

  - [ ]* 12.5 Write property test for multi-format support
    - **Property 32: Multi-format support**
    - **Validates: Requirements 10.4**

  - [ ]* 12.6 Write property test for backend replacement
    - **Property 33: Backend replacement**
    - **Validates: Requirements 10.5**

- [ ] 13. Implement comprehensive error handling
  - [ ] 13.1 Add error handling for missing @ConfigResource
    - Throw IllegalStateException with class name
    - _Requirements: 9.1_

  - [ ] 13.2 Add error handling for uninitialized ConfigManager
    - Throw IllegalStateException if init() not called
    - _Requirements: 9.2_

  - [ ] 13.3 Add error handling for I/O operations
    - Wrap IOException with RuntimeException including file path
    - _Requirements: 9.3, 9.4_

  - [ ] 13.4 Add error handling for reflection operations
    - Wrap reflection exceptions with field and class context
    - _Requirements: 9.5, 9.6_

  - [ ] 13.5 Add error handling for type conversion
    - Include field name and expected type in exception
    - _Requirements: 9.7_

  - [ ]* 13.6 Write unit tests for error messages
    - Test missing annotation error
    - Test uninitialized manager error
    - Test I/O error messages
    - Test reflection error messages
    - Test type conversion error messages
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

- [ ] 14. Implement annotation processing helpers
  - [ ] 14.1 Update resolveKey() to handle @ConfigKey
    - Return annotation value or field name
    - _Requirements: 1.2, 1.3_

  - [ ] 14.2 Update isIgnored() to handle @ConfigIgnore and static fields
    - Check for annotation and static modifier
    - _Requirements: 1.4, 1.5_

  - [ ]* 14.3 Write property test for ignored field exclusion
    - **Property 4: Ignored field exclusion**
    - **Validates: Requirements 1.4**

  - [ ]* 14.4 Write property test for static field exclusion
    - **Property 5: Static field exclusion**
    - **Validates: Requirements 1.5**

- [ ] 15. Implement file generation on first load
  - [ ] 15.1 Update loadInternal() to create file if not exists
    - Check file existence before loading
    - Create file with default values if missing
    - _Requirements: 2.1_

  - [ ]* 15.2 Write property test for file generation
    - **Property 6: File generation on first load**
    - **Validates: Requirements 2.1**

- [ ] 16. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 17. Deprecate BukkitYamlBackend
  - [ ] 17.1 Add @Deprecated annotation to BukkitYamlBackend
    - Add deprecation notice in javadoc
    - Recommend ConfigurateYamlBackend instead

  - [ ] 17.2 Update documentation to reflect deprecation
    - Update README with migration guide
    - Add examples using new backend

- [ ] 18. Create example configs and usage documentation
  - [ ] 18.1 Create example config classes
    - Basic config example
    - Multi-language comment example
    - Custom adapter example
    - Validation example
    - Multi-format example

  - [ ] 18.2 Create usage documentation
    - Getting started guide
    - API reference
    - Migration guide from old implementation
    - Best practices

- [ ] 19. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
