package com.kawanansemut.simplequery

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

class PredicateNumber<T : Number>(
    private val field: Expression<T>,
    private val operator: FilterData.FILTEROP,
    private val v: T,
    private val cb: CriteriaBuilder,
//    private val root: Root<X>
) {

    fun build(): Predicate? {
        return when (operator) {
            FilterData.FILTEROP.GT, FilterData.FILTEROP.GTT, FilterData.FILTEROP.GTD -> cb.gt(field, v)
            FilterData.FILTEROP.LT, FilterData.FILTEROP.LTD, FilterData.FILTEROP.LTT -> cb.lt(field, v)
            FilterData.FILTEROP.GE, FilterData.FILTEROP.GED, FilterData.FILTEROP.GET -> cb.ge(field, v)
            FilterData.FILTEROP.LE, FilterData.FILTEROP.LED, FilterData.FILTEROP.LET -> cb.le(field, v)
            FilterData.FILTEROP.EQ, FilterData.FILTEROP.EQD, FilterData.FILTEROP.EQT -> cb.equal(field, v)
            FilterData.FILTEROP.NEQ, FilterData.FILTEROP.NEQD, FilterData.FILTEROP.NEQT -> cb.notEqual(field, v)
            FilterData.FILTEROP.ISNULL -> cb.isNull(field)
            FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(field)
            else -> null
        }
    }
}