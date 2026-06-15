package me.utsob.booxrichannotation

import io.pebbletemplates.pebble.extension.AbstractExtension
import io.pebbletemplates.pebble.extension.Filter
import io.pebbletemplates.pebble.template.EvaluationContext
import io.pebbletemplates.pebble.template.PebbleTemplate
import kotlin.math.pow
import kotlin.math.round

class CustomPebbleExtension : AbstractExtension() {
    override fun getFilters(): Map<String, Filter> {
        return mapOf("round" to RoundFilter())
    }
}

class RoundFilter : Filter {
    override fun apply(
        input: Any?,
        args: MutableMap<String, Any>?,
        self: PebbleTemplate?,
        context: EvaluationContext?,
        lineNumber: Int
    ): Any? {
        if (input == null) return null
        
        val number = when (input) {
            is Number -> input.toDouble()
            is String -> input.toDoubleOrNull() ?: return input
            else -> return input
        }
        
        val decimals = args?.get("0")?.toString()?.toIntOrNull() ?: 0
        
        return if (decimals <= 0) {
            round(number).toInt()
        } else {
            val multiplier = 10.0.pow(decimals)
            round(number * multiplier) / multiplier
        }
    }
    
    override fun getArgumentNames(): List<String> {
        return listOf("decimals")
    }
}
