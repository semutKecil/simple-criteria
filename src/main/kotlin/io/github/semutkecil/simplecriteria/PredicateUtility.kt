package io.github.semutkecil.simplecriteria

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Predicate
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class PredicateUtility {
    companion object {
        //        private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private fun localDatetimePredicate(fd: FilterData, root: From<*, *>, cb: CriteriaBuilder): Predicate? {

            /**
             *  Expression<Integer> year = cb.function("year", Integer.class, root.get<LocalDateTime>(fd.fi));
            Expression<Integer> month = cb.function("month", Integer.class, root.get<LocalDateTime>(fd.fi));
            Expression<Integer> day = cb.function("day", Integer.class, root.get<LocalDateTime>(fd.fi));
            // Create expressions that extract time parts:
            Expression<Integer> hour = cb.function("hour", Integer.class, time);
            Expression<Integer> minute = cb.function("minute", Integer.class, time);
            Expression<Integer> second = cb.function("second", Integer.class, ts);
             */


            return when (fd.o!!) {
                FilterData.FILTEROP.EQ -> cb.equal(
                    root.get<LocalDateTime>(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.NEQ -> cb.notEqual(
                    root.get<LocalDateTime>(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GT -> cb.greaterThan(
                    root.get(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LT -> cb.lessThan(
                    root.get(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                    root.get(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                    root.get(fd.fi),
                    LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.EQD,
                FilterData.FILTEROP.LED,
                FilterData.FILTEROP.GED,
                FilterData.FILTEROP.LTD,
                FilterData.FILTEROP.GTD,
                FilterData.FILTEROP.NEQD -> {
                    val intDateExp = cb.sum(
                        cb.sum(
                            cb.prod(cb.function("year", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 10000),
                            cb.prod(cb.function("month", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 100)
                        ), cb.function("day", Integer::class.java, root.get<LocalDateTime>(fd.fi))
                    )
                    numberPredicate(
                        intDateExp, fd.o!!, fd.v!!.replace("-", "").toInt(), cb
                    )
                }

                FilterData.FILTEROP.EQT,
                FilterData.FILTEROP.LET,
                FilterData.FILTEROP.GET,
                FilterData.FILTEROP.LTT,
                FilterData.FILTEROP.GTT,
                FilterData.FILTEROP.NEQT -> {
                    val intTimeExp = cb.sum(
                        cb.sum(
                            cb.prod(cb.function("hour", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 3600),
                            cb.prod(cb.function("minute", Integer::class.java, root.get<LocalDateTime>(fd.fi)), 60)
                        ),
                        cb.function("second", Integer::class.java, root.get<LocalDateTime>(fd.fi))
                    )

                    val stv = fd.v!!.split(':').map { it.toInt() }
                    val tVal = (stv[0] * 3600) + (stv[1] * 60) + stv[2]
                    numberPredicate(intTimeExp, fd.o!!, tVal, cb)
                }

                FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalDateTime>(fd.fi))
                FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalDateTime>(fd.fi))
                else -> null
            }
        }

        private fun localDatePredicate(fd: FilterData, root: From<*, *>, cb: CriteriaBuilder): Predicate? {
            return when (fd.o!!) {
                FilterData.FILTEROP.EQ -> cb.equal(
                    root.get<LocalDate>(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.NEQ -> cb.notEqual(
                    root.get<LocalDate>(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GT -> cb.greaterThan(
                    root.get(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LT -> cb.lessThan(
                    root.get(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                    root.get(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                    root.get(fd.fi),
                    LocalDate.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.EQD,
                FilterData.FILTEROP.LED,
                FilterData.FILTEROP.GED,
                FilterData.FILTEROP.LTD,
                FilterData.FILTEROP.GTD,
                FilterData.FILTEROP.NEQD -> {
                    val intDateExp = cb.sum(
                        cb.sum(
                            cb.prod(cb.function("year", Integer::class.java, root.get<LocalDate>(fd.fi)), 10000),
                            cb.prod(cb.function("month", Integer::class.java, root.get<LocalDate>(fd.fi)), 100)
                        ), cb.function("day", Integer::class.java, root.get<LocalDate>(fd.fi))
                    )
                    numberPredicate(
                        intDateExp, fd.o!!, fd.v!!.replace("-", "").toInt(), cb
                    )
                }

                FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalDate>(fd.fi))
                FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalDate>(fd.fi))
                else -> null
            }
        }

        private fun localTimePredicate(fd: FilterData, root: From<*, *>, cb: CriteriaBuilder): Predicate? {
            return when (fd.o!!) {
                FilterData.FILTEROP.EQ -> cb.equal(
                    root.get<LocalTime>(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.NEQ -> cb.notEqual(
                    root.get<LocalTime>(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GT -> cb.greaterThan(
                    root.get(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LT -> cb.lessThan(
                    root.get(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.GE -> cb.greaterThanOrEqualTo(
                    root.get(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.LE -> cb.lessThanOrEqualTo(
                    root.get(fd.fi),
                    LocalTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                )

                FilterData.FILTEROP.EQT,
                FilterData.FILTEROP.LET,
                FilterData.FILTEROP.GET,
                FilterData.FILTEROP.LTT,
                FilterData.FILTEROP.GTT,
                FilterData.FILTEROP.NEQT -> {
                    val intTimeExp = cb.sum(
                        cb.sum(
                            cb.prod(cb.function("hour", Integer::class.java, root.get<LocalTime>(fd.fi)), 3600),
                            cb.prod(cb.function("minute", Integer::class.java, root.get<LocalTime>(fd.fi)), 60)
                        ),
                        cb.function("second", Integer::class.java, root.get<LocalTime>(fd.fi))
                    )

                    val stv = fd.v!!.split(':').map { it.toInt() }
                    val tVal = (stv[0] * 3600) + (stv[1] * 60) + stv[2]
                    numberPredicate(intTimeExp, fd.o!!, tVal, cb)
                }

                FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<LocalTime>(fd.fi))
                FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<LocalTime>(fd.fi))
                else -> null
            }
        }

        private fun <T : Number> numberPredicate(
            field: Expression<T>,
            operator: FilterData.FILTEROP,
            v: T,
            cb: CriteriaBuilder
        ): Predicate? {
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

        private fun stringPredicate(
            field: Expression<String>,
            operator: FilterData.FILTEROP,
            v: String,
            cb: CriteriaBuilder
        ): Predicate? {
            return when (operator) {
                FilterData.FILTEROP.GT, FilterData.FILTEROP.GTT, FilterData.FILTEROP.GTD -> cb.greaterThan(field, v)
                FilterData.FILTEROP.LT, FilterData.FILTEROP.LTD, FilterData.FILTEROP.LTT -> cb.lessThan(field, v)
                FilterData.FILTEROP.GE, FilterData.FILTEROP.GED, FilterData.FILTEROP.GET -> cb.greaterThanOrEqualTo(
                    field,
                    v
                )

                FilterData.FILTEROP.LE, FilterData.FILTEROP.LED, FilterData.FILTEROP.LET -> cb.lessThanOrEqualTo(
                    field,
                    v
                )

                else -> null
            }
        }

        fun generatePredicate(
            root: From<*, *>, cb: CriteriaBuilder,
            fd: FilterData,
            field: Field,
        ): Predicate? {
            return when (fd.o) {
                FilterData.FILTEROP.EQ -> {
                    if (field.type.isEnum) {
                        cb.equal(
                            root.get<Enum<*>>(fd.fName),
                            field.type.enumConstants.first { any -> any.toString() == fd.v!! })
                    } else {
                        when (field.type) {
                            Boolean::class.java -> cb.equal(root.get<Boolean>(fd.fName), fd.v!!.toBoolean())
                            LocalDateTime::class.java -> cb.equal(
                                root.get<LocalDateTime>(fd.fName),
                                LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                            )

                            String::class.java -> cb.equal(
                                cb.lower(root.get(fd.fName)),
                                fd.v!!.lowercase(Locale.getDefault())
                            )

                            else -> cb.equal(root.get<Any>(fd.fName), fd.v)
                        }
                    }
                }

                FilterData.FILTEROP.NEQ -> {
                    if (field.type.isEnum) {
                        cb.notEqual(
                            root.get<Enum<*>>(fd.fName),
                            field.type.enumConstants.first { any -> any.toString() == fd.v!! })
                    } else {
                        when (field.type) {
                            Boolean::class.java -> cb.notEqual(root.get<Boolean>(fd.fName), fd.v!!.toBoolean())
                            LocalDateTime::class.java -> cb.notEqual(
                                root.get<LocalDateTime>(fd.fName),
                                LocalDateTime.parse(fd.v!!, FilterData.dateTimeFormatter)
                            )

                            String::class.java -> cb.notEqual(
                                cb.lower(root.get(fd.fName)),
                                fd.v!!.lowercase(Locale.getDefault())
                            )

                            else -> cb.notEqual(root.get<Any>(fd.fName), fd.v)
                        }
                    }
                }

                FilterData.FILTEROP.LIKE -> cb.like(
                    cb.lower(root.get<String>(fd.fName).`as`(String::class.java)),
                    fd.v!!.lowercase(Locale.getDefault())
                )

                FilterData.FILTEROP.ISNULL -> cb.isNull(root.get<Any>(fd.fName))
                FilterData.FILTEROP.ISNOTNULL -> cb.isNotNull(root.get<Any>(fd.fName))
                FilterData.FILTEROP.IN -> {
                    fd.vAr?.let {
                        root.get<String>(fd.fName).`in`(it.toMutableList())
                    }

                }

                FilterData.FILTEROP.NOTIN -> {
                    fd.vAr?.let {
                        val predicate = root.get<String>(fd.fName).`in`(it.toMutableList())
                        cb.not(predicate)
                    }

                }

                else -> {
                    when (field.type) {
                        String::class.java -> stringPredicate(
                            root.get(fd.fi!!),
                            fd.o!!,
                            fd.v!!.toString(),
                            cb
                        )

                        Int::class.java, Integer::class.java -> numberPredicate(
                            root.get(fd.fName!!),
                            fd.o!!,
                            fd.v!!.toInt(),
                            cb
                        )

                        Float::class.java, java.lang.Float::class.java -> numberPredicate(
                            root.get(fd.fName!!),
                            fd.o!!,
                            fd.v!!.toFloat(),
                            cb
                        )

                        Long::class.java, java.lang.Long::class.java -> numberPredicate(
                            root.get(fd.fName!!),
                            fd.o!!,
                            fd.v!!.toLong(),
                            cb
                        )

                        Double::class.java, java.lang.Double::class.java -> numberPredicate(
                            root.get(fd.fName!!),
                            fd.o!!,
                            fd.v!!.toDouble(),
                            cb
                        )

                        LocalDateTime::class.java -> localDatetimePredicate(fd, root, cb)
                        LocalDate::class.java -> localDatePredicate(fd, root, cb)
                        LocalTime::class.java -> localTimePredicate(fd, root, cb)
                        else -> null
                    }
                }
            }
        }

    }


}