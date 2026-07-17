# Native Image Metadata Tracing

When collecting production native-image metadata from Ballerina tests, include
`access-filter.json` in the tracing-agent options:

```text
-agentlib:native-image-agent=access-filter-file=<repository-root>/build-config/graalvm/access-filter.json,config-output-dir=<output-directory>
```

The filter excludes the Ballerina test package and Testerina classes. Before
packing the output, run `:solace-native:verifyNativeImageMetadata`; it rejects
test entries and JDK-version-specific ICU resource paths.
