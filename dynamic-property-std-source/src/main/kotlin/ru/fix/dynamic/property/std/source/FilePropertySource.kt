package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.PropertySubscription
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.reference.ReferenceCleaner
import ru.fix.stdlib.files.FileWatcher
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object PropertiesFileParser : FilePropertySource.Parser {
    override fun parsePropertiesFile(filePath: Path): Map<String, String> {
        val javaProps = Properties().apply {
            load(Files.newBufferedReader(filePath, StandardCharsets.UTF_8))
        }
        return javaProps as Map<String, String>
    }
}

/**
 * Load properties from file.
 * Watch for file modifications and update properties when content changes.
 */
class FilePropertySource(
        sourceFilePath: DynamicProperty<Path>,
        private val propertyParser: Parser = PropertiesFileParser,
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) :
        DynamicPropertySource {

    @FunctionalInterface
    interface Parser {
        fun parsePropertiesFile(filePath: Path): Map<String, String>
    }

    private val sourceFilePath: PropertySubscription<Path>

    private val inMemorySource = InMemoryPropertySource(
            marshaller,
            referenceCleaner
    )

    private val fileWatcher = FileWatcher()

    init {
        this.sourceFilePath = sourceFilePath.createSubscription()
                .setAndCallListener { prevPath, newPath ->
                    if (newPath != prevPath) {
                        if (prevPath != null) {
                            fileWatcher.unregister(prevPath)
                        }
                        fileWatcher.register(newPath) {
                            updateProperties(newPath)
                        }
                    }
                    updateProperties(newPath)
                }
    }


    private fun updateProperties(newPath: Path) {
        val newProperties = propertyParser.parsePropertiesFile(newPath)

        newProperties.forEach { (key, value) ->
            inMemorySource[key] = value
        }
        inMemorySource.propertyNames()
                .filter { !newProperties.contains(it) }
                .forEach { name ->
                    inMemorySource.remove(name)
                }
    }

    override fun <T : Any?> createSubscription(propertyName: String,
                                               propertyType: Class<T>,
                                               defaultValue: OptionalDefaultValue<T>): DynamicPropertySource.Subscription<T> =
            inMemorySource.createSubscription(propertyName, propertyType, defaultValue)


    override fun close() {
        sourceFilePath.close()
        fileWatcher.close()
        inMemorySource.close()
    }
}