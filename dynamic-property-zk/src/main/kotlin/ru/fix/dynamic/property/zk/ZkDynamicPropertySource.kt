package ru.fix.dynamic.property.zk

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.TreeCache
import org.apache.curator.framework.recipes.cache.TreeCacheEvent
import org.apache.curator.framework.recipes.cache.TreeCacheListener
import org.apache.logging.log4j.kotlin.Logging
import ru.fix.dynamic.property.api.marshaller.DynamicPropertyMarshaller
import ru.fix.dynamic.property.api.source.DynamicPropertySource
import ru.fix.dynamic.property.api.source.OptionalDefaultValue
import ru.fix.dynamic.property.std.source.PropertySourceAccessor
import ru.fix.dynamic.property.std.source.PropertySourcePublisher
import ru.fix.stdlib.reference.ReferenceCleaner
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Implementation of [DynamicPropertySource] that uses Zookeeper [TreeCache]
 * and provides subscriptions to property change events.
 *
 * During initialization constuctor will block and wait until all properties will be loaded from zookeeper to local cache.
 *
 * Use [CuratorFrameworkFactory] in order to get curatorFramework instance.
 * ```
 * CuratorFrameworkFactory.newClient("zk-host1:port1,zk-host2:port2...", ExponentialBackoffRetry(1000, 10))
 * ```
 *
 * @param curatorFramework Ready to use curator framework
 * @param zookeeperConfigPath Root path where ZkDynamicPropertySource will store properties.
 *                            E.g. '/my-application/config'
 * @param initializationTimeout how log to wait until all property values will be loaded
 */
class ZkDynamicPropertySource(
    private val curatorFramework: CuratorFramework,
    zookeeperConfigPath: String,
    marshaller: DynamicPropertyMarshaller,
    initializationTimeout: Duration
) : DynamicPropertySource {

    companion object : Logging

    private val readTreeAndProcessNotificationLock = ReentrantLock()

    private val propertySourcePublisher = PropertySourcePublisher(
        propertySourceAccessor = object : PropertySourceAccessor {
            override fun accessPropertyUnderLock(propertyName: String, accessor: (String?) -> Unit) {
                readTreeAndProcessNotificationLock.withLock {
                    val path = getAbsolutePathForProperty(propertyName)
                    val currentData = treeCache.getCurrentData(path)
                    val currentValue = if (currentData == null)
                        null
                    else
                        zkDataToStringOrNull(currentData.data, currentData.toString())
                    accessor(currentValue)
                }
            }
        },
        marshaller = marshaller,
        referenceCleaner = ReferenceCleaner.getInstance()
    )

    private val treeCache: TreeCache

    private val rootPath: String =
        if (zookeeperConfigPath.endsWith('/')) {
            zookeeperConfigPath.dropLast(1)
        } else {
            zookeeperConfigPath
        }

    private val rootPathPrefix = "$rootPath/"

    init {

        if (curatorFramework.state == CuratorFrameworkState.LATENT) {
            curatorFramework.start()
        }
        treeCache = TreeCache(this.curatorFramework, this.rootPath)
        val treeCacheInitialized = CountDownLatch(1)

        treeCache.listenable.addListener(TreeCacheListener { _, treeCacheEvent ->

            logger.trace { "Received TreeCache event: $treeCacheEvent" }

            when (treeCacheEvent.type!!) {
                TreeCacheEvent.Type.NODE_ADDED,
                TreeCacheEvent.Type.NODE_UPDATED -> {
                    try {
                        onZkTreeChanged(
                            treeCacheEvent, zkDataToStringOrNull(
                                treeCacheEvent.data.data,
                                treeCacheEvent.toString()
                            )
                        )
                    } catch (exc: Exception) {
                        logger.error("Zk property updating error for event $treeCacheEvent", exc)
                    }
                }
                TreeCacheEvent.Type.NODE_REMOVED -> {
                    onZkTreeChanged(treeCacheEvent, null)
                }
                TreeCacheEvent.Type.INITIALIZED -> {
                    treeCacheInitialized.countDown()
                }
                TreeCacheEvent.Type.CONNECTION_LOST,
                TreeCacheEvent.Type.CONNECTION_SUSPENDED,
                TreeCacheEvent.Type.CONNECTION_RECONNECTED -> {
                    //do nothing
                }
            }
        })

        treeCache.start()

        if (!treeCacheInitialized.await(initializationTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw ZkDynamicPropertySourceFailedToInitializeOnTime(initializationTimeout)
        }
    }

    /**
     * Works through curator directly to load latest data
     */
    fun readAllProperties(): Map<String, Any> {
        val allProperties = ConcurrentHashMap<String, Any>()

        val exist = curatorFramework.checkExists().forPath(rootPath)
        if (exist != null) {
            val children = curatorFramework.children.forPath(rootPath)
            if (children.isNotEmpty()) {
                val latch = CountDownLatch(children.size)
                for (child in children) {
                    curatorFramework.data.watched().inBackground { _, event ->
                        allProperties[child] = String(event.data, StandardCharsets.UTF_8)
                        latch.countDown()
                    }.forPath(getAbsolutePathForProperty(child))
                }
                if (!latch.await(120, TimeUnit.SECONDS)) {
                    throw TimeoutException("Failed to extract zk properties data")
                }
            }
        }
        return allProperties
    }


    private fun zkDataToStringOrNull(data: ByteArray, logDetails: String): String? =
        try {
            String(data, StandardCharsets.UTF_8)
        } catch (exc: Exception) {
            logger.error("Failed to read string value from zk node. $logDetails", exc)
            null
        }


    private fun onZkTreeChanged(treeCacheEvent: TreeCacheEvent, newValue: String?) {
        val absolutePath = treeCacheEvent.data.path
        if (!absolutePath.startsWith(rootPathPrefix)) {
            return
        }

        val propertyName = getPropertyNameFromAbsolutePath(absolutePath)

        logger.info {
            "Zk property change: type: ${treeCacheEvent.type}, node: $absolutePath. New value is '$newValue'"
        }
        readTreeAndProcessNotificationLock.withLock {
            propertySourcePublisher.notifyAboutPropertyChange(propertyName, newValue)
        }
    }


    private fun getAbsolutePathForProperty(propertyName: String): String {
        Objects.requireNonNull(propertyName)
        return "$rootPath/$propertyName"
    }

    private fun getPropertyNameFromAbsolutePath(absolutePath: String): String {
        return absolutePath.substring(rootPath.length + 1)
    }

    override fun <T : Any?> createSubscription(
        propertyName: String,
        propertyType: Class<T>,
        defaultValue: OptionalDefaultValue<T>
    ): DynamicPropertySource.Subscription<T> {
        return propertySourcePublisher.createSubscription(propertyName, propertyType, defaultValue)
    }

    override fun close() {
        propertySourcePublisher.close()
        treeCache.close()
    }


}

class ZkDynamicPropertySourceFailedToInitializeOnTime(
    timeout: Duration
) : java.lang.Exception(
    "ZkDynamicPropertySource failed to initialize on time. " +
            "Given initialization timeout: $timeout"
)