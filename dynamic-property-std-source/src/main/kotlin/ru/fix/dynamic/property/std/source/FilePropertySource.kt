package ru.fix.dynamic.property.std.source

import ru.fix.dynamic.property.api.DynamicProperty
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.stdlib.concurrency.threads.ReferenceCleaner
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object PropertiesFileParser {
    fun parsePropertiesFile(filePath: Path): Map<String, String> {
        val javaProps = Properties().apply {
            load(Files.newBufferedReader(filePath, StandardCharsets.UTF_8))
        }
        return javaProps as Map<String, String>
    }
}

class FilePropertySource(
        private val propertiesFile: DynamicProperty<Path>,
        private val propertyParser: (Path) -> Map<String, String> = PropertiesFileParser::parsePropertiesFile,
        marshaller: DynamicPropertyMarshaller,
        referenceCleaner: ReferenceCleaner = ReferenceCleaner.getInstance()) :
        DynamicPropertySource {

    private val inMemorySource = InMemoryPropertySource(
            marshaller,
            referenceCleaner
    )

    private fun updateProperties(newPath: Path) {
        val newProperties = propertyParser(newPath)

        newProperties.forEach { (key, value) ->
            inMemorySource[key] = value
        }
        inMemorySource.propertyNames()
                .filter { !newProperties.contains(it) }
                .forEach { name ->
                    inMemorySource.remove(name)
                }
    }

    init {

        propertiesFile.addAndCallListener { prevPath, newPath ->
            //TODO: add FileWatcher and listen for property change
//            if (newPath != prevPath) {
//                if(prevPath != null){
//                    watcher.unregister(prevPath)
//                }
//                watcher.register(newPath) {
//                        updateProperties(newPath)
//                }
//            }
            updateProperties(newPath)
        }
    }

    override fun <T : Any?> subscribeAndCallListener(
            propertyName: String,
            propertyType: Class<T>,
            defaultValue: OptionalDefaultValue<T>,
            listener: DynamicPropertySource.Listener<T>): DynamicPropertySource.Subscription =
            inMemorySource.subscribeAndCallListener(propertyName, propertyType, defaultValue, listener)

    override fun close() {
        inMemorySource.close()
    }
}