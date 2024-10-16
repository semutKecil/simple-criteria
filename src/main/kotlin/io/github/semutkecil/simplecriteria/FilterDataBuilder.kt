package io.github.semutkecil.simplecriteria

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

class FilterDataBuilder<T>(private val fd: FilterData, private val cob: Class<T>) {
    fun buildPredicate(root: Root<T>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
        val fields = SimpleQuery.collectEntityField(cob)
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