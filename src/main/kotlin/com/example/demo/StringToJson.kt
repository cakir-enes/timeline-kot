package com.example.demo

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException

class StringToJson : JsonSerializer<String?>() {
    private val mapper = ObjectMapper()

    @Throws(IOException::class)
    override fun serialize(s: String?, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        jsonGenerator.writeObject(mapper.readTree(s))
    }
}
