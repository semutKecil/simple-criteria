package com.kawanansemut.simplequery

import jakarta.persistence.criteria.From


class JoinData<T>(val clazz: Class<T>, val joinFrom: (r: From<*, *>, mj: MutableMap<String, JoinObj<*>>) -> From<*, *>)