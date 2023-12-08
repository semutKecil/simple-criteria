package com.github.semutkecil.simplecriteria

import jakarta.persistence.criteria.From


class JoinObj<T>(clazz: Class<T>, val join: From<*, *>) {
    val fields = clazz.declaredFields.toMutableList()

    init {
        fields.addAll(clazz.superclass.declaredFields.toMutableList())
    }
}