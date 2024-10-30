package io.github.semutkecil.simplecriteria

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.jpa.domain.Specification
import java.io.Serializable
import java.time.format.DateTimeFormatter

class FilterData : Serializable {
    var fi: String? = null
    var v: String? = null
    var vAr: Array<String>? = null
    var o: FILTEROP? = null
    var fName: String? = null
    var f: Array<String>? = null
        set(value) {
            if (value != null && value.size >= 3) {
                this.fi = value[0]
                this.fName = value[0].split(".").last()
                this.o = FILTEROP.valueOf(value[1])
                this.v = value[2]

                this.vAr = value.sliceArray(2 until value.size)
//                this.vAr = when {
//                    value.size == 3 -> {
//                        arrayOf(value[2])
//                    }
//                    value.size > 3 -> {
//                        val sliceArray = value.sliceArray(2 until value.size)
//                        sliceArray
//                    }
//                    else -> {
//                        null
//                    }
//                }
            }
            field = value
        }
    var and: Array<FilterData>? = null
    var or: Array<FilterData>? = null


    fun toJson(): String {
        val jsonMapper = jacksonObjectMapper()
        return jsonMapper.writeValueAsString(this)
    }

    fun flattenField(): List<String> {
        return if (this.fi != null) {
            listOf(fi!!)
        } else if (this.and != null) {
            this.and!!.map { it.flattenField() }.flatten().distinct()
        } else {
            this.or!!.map { it.flattenField() }.flatten().distinct()
        }
    }

    fun <T> toSpec(clazz: Class<T>): Specification<T> {
        return Specification<T> { r, cq, cb ->
            FilterDataBuilder(this, clazz).buildPredicate(r, cq, cb)
        }
    }

    companion object {
        var dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun fromJson(json: String): FilterData {
            val jsonMapper = jacksonObjectMapper()
            return jsonMapper.readValue(json, FilterData::class.java)
        }

        fun filter(field: String, operator: FILTEROP, vararg value: String): FilterData {
            val fd = FilterData()
            fd.f = arrayOf(field, operator.name, *value)
            return fd
        }

        fun and(vararg filterData: FilterData): FilterData {
            val fd = FilterData()
            fd.and = filterData.map { it }.toTypedArray()
            return fd
        }

        fun or(vararg filterData: FilterData): FilterData {
            val fd = FilterData()
            fd.or = filterData.map { it }.toTypedArray()
            return fd
        }


    }

    enum class FILTEROP {
        EQ,
        NEQ,
        GT,
        LT,
        GE,
        LE,
        LIKE,
        EQD,
        NEQD,
        EQT,
        NEQT,
        GTD,
        GTT,
        LTD,
        LTT,
        GED,
        GET,
        LED,
        LET,
        ISNULL,
        ISNOTNULL,
        IN,
        NOTIN,
        LIKEREV,
    }
}