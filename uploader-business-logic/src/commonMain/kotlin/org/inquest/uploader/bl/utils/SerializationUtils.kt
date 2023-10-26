package org.inquest.uploader.bl.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readLines
import kotlin.io.path.writeLines
import kotlin.reflect.KClass

/**
 * Everything related to serialization.
 */
@OptIn(ExperimentalSerializationApi::class)
object SerializationUtils {
    private val module: SerializersModule = SerializersModule {
        polymorphicDefaultSerializer(Path::class) {
            PathSerializer()
        }
    }

    /**
     * Property serializer.
     */
    val PROPERTIES: Properties = Properties(this.module)

    /**
     * Bytearray serializer.
     */
    val PROTOBUF: ProtoBuf = ProtoBuf {
        this.serializersModule = this@SerializationUtils.module
    }

    /**
     * Deserializes properties from [propPath] into an object of type [clazz].
     */
    @OptIn(InternalSerializationApi::class)
    fun <T: Any> deserializeFromProps(
        propPath: Path,
        clazz: KClass<T>
    ): T = PROPERTIES.decodeFromStringMap(
        clazz.serializer(),
        propPath.readLines().associate {
            val split: List<String> = it.split('=')
            split[0] to split[1]
        }
    )

    /**
     * Deserializes properties from [propPath] into an object of type [T].
     */
    inline fun <reified T: Any> deserializeFromProps(propPath: Path): T = deserializeFromProps(propPath, T::class)

    /**
     * Serializes [this] to properties at [propPath].
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T: Any> T.serializeToProps(propPath: Path) {
        PROPERTIES.encodeToStringMap(T::class.serializer(), this). map {
            "${it.key}=${it.value}"
        }.apply(propPath::writeLines)
    }

    /**
     * Deserializes [byteArray] into an object of type [T].
     */
    inline fun <reified T: Any> deserializeFromByteArray(byteArray: ByteArray): T =
        if(T::class == ByteArray::class) byteArray as T else deserializeFromByteArray(byteArray, T::class)

    /**
     * Deserializes [byteArray] into an object of type [clazz].
     */
    @OptIn(InternalSerializationApi::class)
    fun <T: Any> deserializeFromByteArray(
        byteArray: ByteArray,
        clazz: KClass<T>
    ): T = if(clazz == ByteArray::class) byteArray as T else PROTOBUF.decodeFromByteArray(clazz.serializer(), byteArray)

    /**
     * Serializes [this] of type [clazz] to a [ByteArray].
     */
    @OptIn(InternalSerializationApi::class)
    fun <T: Any> T.serializeToByteArray(clazz: KClass<T>): ByteArray = if (clazz == ByteArray::class)
        this as ByteArray
    else
        PROTOBUF.encodeToByteArray(clazz.serializer(), this)

    /**
     * Serializes [this] of type [T] to a [ByteArray].
     */
    inline fun <reified T: Any> T.serializeToByteArray(): ByteArray = if(T::class == ByteArray::class)
        this as ByteArray
    else
        PROTOBUF.encodeToByteArray(this)
}

/**
 * (De-)serializes [Path]s by using [Path.absolutePathString].
 */
private class PathSerializer: KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.nio.Path", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Path) = encoder.encodeString(value.absolutePathString())
    override fun deserialize(decoder: Decoder): Path = Path(decoder.decodeString())
}