package it.sapienza.appinterpreter.model.view_model.form_model

import it.sapienza.appinterpreter.json.JsonNumbers
import kotlinx.serialization.ExperimentalSerializationApi
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

/**
 * A date as the configuration may write it (the schema says "a date string accepted by Jackson or an
 * epoch-millisecond number"), as epoch milliseconds: a number; a string of digits; or ISO 8601, "2026-05-01" (UTC
 * midnight) or "2026-05-01T09:30[:00[.000]][Z|+02:00]" (UTC without an offset). Like Jackson, out-of-range fields
 * roll over ("2026-02-30" is 2 March). Jackson's RFC 1123 form ("Fri, 01 May 2026 09:30:00 GMT") isn't supported.
 */
object JacksonDateSerializer : KSerializer<Long?> {
    override val descriptor = PrimitiveSerialDescriptor("JacksonDate", PrimitiveKind.LONG).nullable

    private val ISO =
        Regex("""(\d{4})-(\d\d)-(\d\d)(?:T(\d\d):(\d\d)(?::(\d\d))?(?:\.(\d+))?(Z|[+-]\d\d(?::?\d\d)?)?)?""")

    override fun deserialize(decoder: Decoder): Long? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: throw SerializationException("A date must be a string or a number")
        if (!primitive.isString) {
            return if (JsonNumbers.isDecimal(
                    primitive.content
                )
            ) {
                primitive.content.toDouble().toLong()
            } else {
                primitive.content.toLong()
            }
        }
        return parse(primitive.content) ?: throw SerializationException(
            "Cannot parse date \"${primitive.content}\": not a timestamp or an ISO 8601 date (yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)"
        )
    }

    fun parse(text: String): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.removePrefix("-").all { it.isDigit() } && trimmed != "-") return trimmed.toLongOrNull()
        val match = ISO.matchEntire(trimmed) ?: return null
        val (year, month, day, hour, minute, second, fraction, zone) = match.destructured
        val days = daysFromCivil(year.toLong(), month.toLong(), 1) + day.toLong() - 1
        val seconds =
            days * 86_400 + (hour.toLongOrNull() ?: 0) * 3_600 + (minute.toLongOrNull() ?: 0) * 60 +
                (second.toLongOrNull() ?: 0)
        val millis = fraction.take(3).padEnd(3, '0').takeIf { fraction.isNotEmpty() }?.toLong() ?: 0
        val offsetMinutes = when {
            zone.isEmpty() || zone == "Z" -> 0

            else -> {
                val digits = zone.substring(1).replace(":", "")
                val minutes = digits.take(2).toInt() * 60 + (digits.drop(2).toIntOrNull() ?: 0)
                if (zone[0] == '-') -minutes else minutes
            }
        }
        return (seconds - offsetMinutes * 60L) * 1000 + millis
    }

    /** Days from 1970-01-01 to the given date; a month past 12 rolls into the next year. */
    private fun daysFromCivil(year: Long, month: Long, day: Long): Long {
        val y0 = year + (month - 1).floorDiv(12L)
        val m0 = (month - 1).mod(12L) + 1
        val y = if (m0 <= 2) y0 - 1 else y0
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val doy = (153 * (if (m0 > 2) m0 - 3 else m0 + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    // Writing a null is still marked experimental in kotlinx.serialization.
    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}
