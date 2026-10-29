package it.sapienza.appinterpreter.utils

class ConfigurationValidationException(val jsonPath: String, val detail: String) :
    IllegalArgumentException("$jsonPath: $detail")
