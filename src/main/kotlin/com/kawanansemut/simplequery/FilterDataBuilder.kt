package com.kawanansemut.simplequery

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

    private fun buildLocalDatetimePredicate(fd: FilterData, root: Root<T>, cb: CriteriaBuilder): Predicate? {

        /**
         *  Expression<Integer> year = cb.function("year", Integer.class, root.get<LocalDateTime>(fd.fi));
        Expression<Integer> month = cb.function("month", Integer.class, root.get<LocalDateTime>(fd.fi));
        Expression<Integer> day = cb.function("day", Integer.class, root.get<LocalDateTime>(fd.fi));
        // Create expressions that extract time parts:
        Expression<Integer> hour = cb.function("hour", Integer.class, time);
        Expression<Integer> minute = cb.function("minute", Integer.class, time);
        Expression<Integer> second = cb.function("second", Integer.class, ts);
         */

        val intDateExp = cb.sum(
            cb.sum(
                cb.prod(cb.function("year", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 10000),
                cb.prod(cb.function("month", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 100)
            ), cb.function("day", Integer::class.java, root.get<LocalDateTime>(fd.fi))
        )
        val intTimeExp = cb.sum(
            cb.sum(
                cb.prod(cb.function("hour", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 3600),
                cb.prod(cb.function("minute", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 60)
            ),
            cb.function("second", Integer::class.java, root.get<LocalDateTime>(fd.fi))
        )

        return when (fd.o!!) {
            FilterData.FILTEROP.EQ -> cb.equal(root.get<LocalDateTime>(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.NEQ -> cb.notEqual(root.get<LocalDateTime>(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GT -> cb.greaterThan(
                root.get(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LT -> cb.lessThan(root.get(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                root.get(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                root.get(fd.fi),
                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.EQD, FilterData.FILTEROP.LED, FilterData.FILTEROP.GED, FilterData.FILTEROP.LTD, FilterData.FILTEROP.GTD, FilterData.FILTEROP.NEQD -> PredicateNumber(
                intDateExp, fd.o!!, fd.v!!.replace("-", "").toInt(), cb
            ).build()

            FilterData.FILTEROP.EQT, FilterData.FILTEROP.LET, FilterData.FILTEROP.GET, FilterData.FILTEROP.LTT, FilterData.FILTEROP.GTT, FilterData.FILTEROP.NEQT -> {
                val stv = fd.v!!.split(':').map { it.toInt() }
                val tVal = (stv[0] * 3600) + (stv[1] * 60) + stv[2]
                PredicateNumber(intTimeExp, fd.o!!, tVal, cb).build()
            }
            FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalDateTime>(fd.fi))
            FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalDateTime>(fd.fi))
            else -> null
        }
    }

    private fun buildLocalDatePredicate(fd: FilterData, root: Root<T>, cb: CriteriaBuilder): Predicate? {
        val intDateExp = cb.sum(
            cb.sum(
                cb.prod(cb.function("year", Integer::class.java, root.get<LocalDate>(fd.fi)), 10000),
                cb.prod(cb.function("month", Integer::class.java, root.get<LocalDate>(fd.fi)), 100)
            ), cb.function("day", Integer::class.java, root.get<LocalDate>(fd.fi))
        )

        return when (fd.o!!) {
            FilterData.FILTEROP.EQ -> cb.equal(root.get<LocalDate>(fd.fi), LocalDate.parse(fd.v!!, dateTimeFormatter))
            FilterData.FILTEROP.NEQ -> cb.notEqual(root.get<LocalDate>(fd.fi),
                LocalDate.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GT -> cb.greaterThan(
                root.get(fd.fi),
                LocalDate.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LT -> cb.lessThan(root.get(fd.fi),
                LocalDate.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                root.get(fd.fi),
                LocalDate.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                root.get(fd.fi),
                LocalDate.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.EQD, FilterData.FILTEROP.LED, FilterData.FILTEROP.GED, FilterData.FILTEROP.LTD, FilterData.FILTEROP.GTD, FilterData.FILTEROP.NEQD -> PredicateNumber(
                intDateExp, fd.o!!, fd.v!!.replace("-", "").toInt(), cb
            ).build()

            FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalDate>(fd.fi))
            FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalDate>(fd.fi))
            else -> null
        }
    }

    private fun buildLocalTimePredicate(fd: FilterData, root: Root<T>, cb: CriteriaBuilder): Predicate? {
        val intTimeExp = cb.sum(
            cb.sum(
                cb.prod(cb.function("hour", Integer::class.java, root.get<LocalTime>(fd.fi)), 3600),
                cb.prod(cb.function("minute", Integer::class.java, root.get<LocalTime>(fd.fi)), 60)
            ),
            cb.function("second", Integer::class.java, root.get<LocalTime>(fd.fi))
        )

        return when (fd.o!!) {
            FilterData.FILTEROP.EQ -> cb.equal(root.get<LocalTime>(fd.fi), LocalTime.parse(fd.v!!, dateTimeFormatter))
            FilterData.FILTEROP.NEQ -> cb.notEqual(root.get<LocalTime>(fd.fi),
                LocalTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GT -> cb.greaterThan(
                root.get(fd.fi),
                LocalTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LT -> cb.lessThan(root.get(fd.fi),
                LocalTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                root.get(fd.fi),
                LocalTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                root.get(fd.fi),
                LocalTime.parse(fd.v!!, dateTimeFormatter)
            )
            FilterData.FILTEROP.EQT, FilterData.FILTEROP.LET, FilterData.FILTEROP.GET, FilterData.FILTEROP.LTT, FilterData.FILTEROP.GTT, FilterData.FILTEROP.NEQT -> {
                val stv = fd.v!!.split(':').map { it.toInt() }
                val tVal = (stv[0] * 3600) + (stv[1] * 60) + stv[2]
                PredicateNumber(intTimeExp, fd.o!!, tVal, cb).build()
            }

            FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalTime>(fd.fi))
            FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalTime>(fd.fi))
            else -> null
        }
    }

    fun buildPredicate(root: Root<T>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {

        val fields = cob.declaredFields.toMutableList()
        fields.addAll(cob.superclass.declaredFields.toMutableList())

        return if (fd.fi != null && fd.o != null && fd.v != null && fields.any { it.name == fd.fi }) {
            val field = fields.first { it.name == fd.fi }

            when (fd.o) {
                FilterData.FILTEROP.EQ -> {
                    if (field.type.isEnum) {
                        cb.equal(
                            root.get<Enum<*>>(fd.fi),
                            field.type.enumConstants.first { any -> any.toString() == fd.v!! })
                    } else {
                        when (field.type) {
                            Boolean::class.java -> cb.equal(root.get<Boolean>(fd.fi), fd.v!!.toBoolean())
                            LocalDateTime::class.java -> cb.equal(
                                root.get<LocalDateTime>(fd.fi),
                                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
                            )
                            String::class.java -> cb.equal(cb.lower(root.get(fd.fi)),
                                fd.v!!.lowercase(Locale.getDefault())
                            )
                            else -> cb.equal(root.get<Any>(fd.fi), fd.v)
                        }
                    }
                }
                FilterData.FILTEROP.NEQ -> {
                    if (field.type.isEnum) {
                        cb.notEqual(
                            root.get<Enum<*>>(fd.fi),
                            field.type.enumConstants.first { any -> any.toString() == fd.v!! })
                    } else {
                        when (field.type) {
                            Boolean::class.java -> cb.notEqual(root.get<Boolean>(fd.fi), fd.v!!.toBoolean())
                            LocalDateTime::class.java -> cb.notEqual(
                                root.get<LocalDateTime>(fd.fi),
                                LocalDateTime.parse(fd.v!!, dateTimeFormatter)
                            )
                            String::class.java -> cb.notEqual(cb.lower(root.get(fd.fi)),
                                fd.v!!.lowercase(Locale.getDefault())
                            )
                            else -> cb.notEqual(root.get<Any>(fd.fi), fd.v)
                        }
                    }
                }
                FilterData.FILTEROP.LIKE -> cb.like(
                    cb.lower(root.get<String>(fd.fi).`as`(String::class.java)),
                    fd.v!!.lowercase(Locale.getDefault())
                )
                FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<Any>(fd.fi))
                FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<Any>(fd.fi))
                FilterData.FILTEROP.IN -> {
                    fd.vAr?.let {
                        root.get<String>(fd.fi).`in`(it.toMutableList())
                    }

                }
                FilterData.FILTEROP.NOTIN -> {
                    fd.vAr?.let {
                        val predicate = root.get<String>(fd.fi).`in`(it.toMutableList())
                        cb.not(predicate)
                    }

                }
                else -> {
                    when (field.type) {
                        Int::class.java -> PredicateNumber(
                            root.get(fd.fi!!),
                            fd.o!!,
                            fd.v!!.toInt(),
                            cb

                        ).build()
                        Float::class.java -> PredicateNumber(
                            root.get(fd.fi!!),
                            fd.o!!,
                            fd.v!!.toFloat(),
                            cb
                        ).build()
                        Long::class.java -> PredicateNumber(
                            root.get(fd.fi!!),
                            fd.o!!,
                            fd.v!!.toLong(),
                            cb
                        ).build()
                        Double::class.java -> PredicateNumber(
                            root.get(fd.fi!!),
                            fd.o!!,
                            fd.v!!.toDouble(),
                            cb
                        ).build()
                        LocalDateTime::class.java -> buildLocalDatetimePredicate(fd, root, cb)
                        LocalDate::class.java -> buildLocalDatePredicate(fd, root, cb)
                        LocalTime::class.java -> buildLocalTimePredicate(fd, root, cb)
                        else -> null
                    }
                }
            }
        } else if (fd.and != null && fd.and!!.isNotEmpty()) {
            cb.and(*this.fd.and!!.mapNotNull { FilterDataBuilder(it, cob).buildPredicate(root, cq, cb) }.toTypedArray())
        } else if (fd.or != null && fd.or!!.isNotEmpty()) {
            cb.or(*this.fd.or!!.mapNotNull { FilterDataBuilder(it, cob).buildPredicate(root, cq, cb) }.toTypedArray())
        } else {
            null
        }
    }
}