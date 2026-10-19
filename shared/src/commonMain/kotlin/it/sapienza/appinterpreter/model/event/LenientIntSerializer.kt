package it.sapienza.appinterpreter.model.event

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/** A whole number that may be written 20.0, which the schema calls an integer and Jackson read as 20. */
object LenientIntSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT).nullable

    override fun deserialize(decoder: Decoder): Int? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        if (element is JsonNull) return null
        val content = (element as? JsonPrimitive)?.takeUnless { it.isString }?.content
            ?: throw SerializationException("Expected a whole number but found $element")
        return content.toIntOrNull() ?: content.toDoubleOrNull()?.let { number ->
            if (number % 1.0 == 0.0 && number >= Int.MIN_VALUE && number <= Int.MAX_VALUE) number.toInt() else null
        } ?: throw SerializationException("Expected a whole number but found $content")
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}
