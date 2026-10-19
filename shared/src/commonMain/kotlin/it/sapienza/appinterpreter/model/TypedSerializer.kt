package it.sapienza.appinterpreter.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * A family of classes told apart by their "type" property, as the configuration writes them: each type's own
 * generated serializer reads the object without "type". An object without one is read with [untyped], when given.
 * Written explicitly rather than with kotlinx's polymorphism, so it needs no reflection on any platform and the model
 * keeps its packages.
 */
open class TypedSerializer<T : Any>(
    name: String,
    private val types: List<Type<out T>>,
    private val untyped: KSerializer<out T>? = null
) : KSerializer<T> {

    class Type<T : Any>(val name: String, val serializer: KSerializer<T>, val owns: (Any) -> Boolean)

    @OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(name, PolymorphicKind.OPEN)

    override fun deserialize(decoder: Decoder): T {
        val json = decoder as JsonDecoder
        val element = json.decodeJsonElement() as? JsonObject
            ?: throw SerializationException("Expected an object for $descriptor")
        val type = element["type"] ?: return untyped?.let { json.json.decodeFromJsonElement(it, element) }
            ?: throw SerializationException("Field 'type' is required for ${descriptor.serialName}")
        val name = (type as? JsonPrimitive)?.takeIf { it.isString }?.content
        val match = types.firstOrNull { it.name == name }
            ?: throw SerializationException("Unknown ${descriptor.serialName} type ${type.jsonPrimitive}")
        return json.json.decodeFromJsonElement(match.serializer, JsonObject(element - "type"))
    }

    override fun serialize(encoder: Encoder, value: T) {
        val json = encoder as JsonEncoder
        val type = types.firstOrNull { it.owns(value) }
        if (type == null) {
            @Suppress("UNCHECKED_CAST")
            val fallback = untyped as? KSerializer<T> ?: throw SerializationException("No type for $value")
            json.encodeSerializableValue(fallback, value)
            return
        }
        @Suppress("UNCHECKED_CAST")
        val fields = json.json.encodeToJsonElement(type.serializer as KSerializer<T>, value) as JsonObject
        json.encodeJsonElement(JsonObject(mapOf("type" to JsonPrimitive(type.name)) + fields))
    }
}
