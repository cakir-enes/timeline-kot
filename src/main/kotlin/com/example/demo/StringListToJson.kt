package com.example.demo

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException

class StringListToJson : JsonSerializer<List<String?>>() {
    private val mapper = ObjectMapper()

    @Throws(IOException::class)
    override fun serialize(strings: List<String?>, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
        jsonGenerator.writeStartArray()
        for (string in strings) {
            jsonGenerator.writeObject(mapper.readTree(string))
        }
        jsonGenerator.writeEndArray()
    }
}