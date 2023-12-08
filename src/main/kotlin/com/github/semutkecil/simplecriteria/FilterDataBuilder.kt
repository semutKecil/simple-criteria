package com.github.semutkecil.simplecriteria

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class FilterDataBuilder<T>(private val fd: FilterData, private val cob: Class<T>) {


    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun buildPredicate(root: Root<T>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {

        val fields = cob.declaredFields.toMutableList()
        fields.addAll(cob.superclass.declaredFields.toMutableList())

        return if (fd.fi != null && fd.o != null && fd.v != null && fields.any { it.name == fd.fi }) {
            val field = fields.first { it.name == fd.fi }
            PredicateUtility.generatePredicate(root, cb, fd, field)
        } else if (fd.and != null && fd.and!!.isNotEmpty()) {
            cb.and(*this.fd.and!!.mapNotNull { FilterDataBuilder(it, cob).buildPredicate(root, cq, cb) }.toTypedArray())
        } else if (fd.or != null && fd.or!!.isNotEmpty()) {
            cb.or(*this.fd.or!!.mapNotNull { FilterDataBuilder(it, cob).buildPredicate(root, cq, cb) }.toTypedArray())
        } else {
            null
        }
    }


}