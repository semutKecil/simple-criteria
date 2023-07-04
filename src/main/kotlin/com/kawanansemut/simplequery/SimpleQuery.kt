package com.kawanansemut.simplequery

import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.*
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SimpleQuery<T> private constructor(
    private val entityManager: EntityManager, private val clazz: Class<T>,
    private val select: List<String>,
    private val joins: Map<String, JoinData<*>>,
    private val filterDataList: MutableList<FilterData>,
    private val orderList: MutableList<QueryOrder>
) {
    private val cb: CriteriaBuilder = entityManager.criteriaBuilder
    private val fields = clazz.declaredFields.toMutableList()
    private val mapSelect = mutableMapOf<String, Path<out Any>>()

    enum class DIR { ASC, DESC }

    init {
        fields.addAll(clazz.superclass.declaredFields.toMutableList())
    }

    fun createQuery(): TypedQuery<Tuple> {
        return entityManager.createQuery(applyQuery())
    }

    fun count(): Long {
        val cb = entityManager.criteriaBuilder
        val q = cb.createQuery(Long::class.java)
        val r = q.from(clazz)
        val mapJoinObj = mutableMapOf<String, JoinObj<*>>()
        q.select(cb.count(r))
        val prList = filterDataList.mapNotNull { buildPredicateFromFilterData(r, it, mapJoinObj) }
        if (prList.isNotEmpty()) {
            q.where(cb.and(*prList.toTypedArray()))
        }

        return entityManager.createQuery(q).singleResult
    }

    fun resultListMap(limit: Int? = null, offset: Int = 0): List<Map<String, *>> {
        val query = entityManager.createQuery(applyQuery())
        query.firstResult = offset
        if (limit != null) {
            query.maxResults = limit
        }

        val slc = select.filter { !it.contains(".") }.toMutableList()
        slc.addAll(select.filter { it.contains(".") })

        return query.resultList.map {
            slc.mapIndexed { idx, str ->
                Pair(str, it.get(idx))
            }.toMap()
        }
    }

    private fun applyQuery(): CriteriaQuery<Tuple> {
        val q = cb.createTupleQuery()
        val r = q.from(clazz)
        val mapJoinObj = mutableMapOf<String, JoinObj<*>>()
        mapSelect.putAll(select.filter { !it.contains(".") }.associateWith {
            val field = fields.first { fl -> fl.name == it }
            getPredicate(it, r, field.type)
        }.toMutableMap())
        select.filter { it.contains(".") }.forEach {
            val cSpl = it.split(".")
            val cn = cSpl.last()
            val cJoin = cSpl.take(cSpl.size - 1).joinToString(".")

            val j = getOrCreateJoin(r, cJoin, mapJoinObj)
            val field = j.fields.first { fl -> fl.name == cn }
            mapSelect[it] = getPredicate(cn, j.join, field.type)
        }

        q.multiselect(*mapSelect.values.toTypedArray())

        val prList = filterDataList.mapNotNull { buildPredicateFromFilterData(r, it, mapJoinObj) }
        if (prList.isNotEmpty()) {
            q.where(cb.and(*prList.toTypedArray()))
        }

        val ord = generateOrder(r, mapJoinObj)
        if (ord.isNotEmpty()) {
            q.orderBy(*ord.toTypedArray())
        }

        return q
    }

    private fun generateOrder(
        r: Root<*>,
        mapJoinObj: MutableMap<String, JoinObj<*>>
    ): List<Order> {
        return orderList.mapNotNull {
            val cSpl = it.fieldName.split(".")
            val cn = cSpl.last()
            val cJoin = cSpl.take(cSpl.size - 1).joinToString(".")
            val root: From<*, *>
            val field: Field
            if (cSpl.size > 1) {
                val j = getOrCreateJoin(r, cJoin, mapJoinObj)
                field = j.fields.first { fl -> fl.name == cn }
                root = j.join
            } else {
                root = r
                field = this.fields.first { fl -> fl.name == cn }
            }
            when (it.dir) {
                DIR.ASC -> cb.asc(getPredicate(cn, root, field.type))
                else -> cb.desc(getPredicate(cn, root, field.type))
            }
        }
    }

    private fun getOrCreateJoin(
        r: Root<*>,
        key: String,
        mapJoinObj: MutableMap<String, JoinObj<*>>
    ): JoinObj<out Any?> {
        val join = if (mapJoinObj.containsKey(key)) {
            mapJoinObj[key]
        } else {

            if (key.contains(".")) {
                val ks = key.split(".")
                val rJoin = ks.take(ks.size - 1).joinToString(".")
                getOrCreateJoin(r, rJoin, mapJoinObj)
            }

            joins[key]?.let {
                mapJoinObj[key] = JoinObj(it.clazz, it.joinFrom(r, mapJoinObj))
                mapJoinObj[key]
            }
        }
        return join ?: throw Exception("unregistered join $key. register join with addJoin")
    }

    private fun buildPredicateFromFilterData(
        r: Root<*>,
        fd: FilterData,
        mapJoinObj: MutableMap<String, JoinObj<*>>
    ): Predicate? {
        return if (fd.and?.isNotEmpty() == true) {
            fd.and?.map { d -> buildPredicateFromFilterData(r, d, mapJoinObj) }?.let { p ->
                cb.and(*p.toTypedArray())
            }
        } else if (fd.or?.isNotEmpty() == true) {
            fd.or?.map { d -> buildPredicateFromFilterData(r, d, mapJoinObj) }?.let { p ->
                cb.or(*p.toTypedArray())
            }
        } else if (fd.fi != null && fd.o != null && fd.v != null) {
            val cSpl = fd.fi!!.split(".")
            val root: From<*, *>?
            val field: Field?
            if (cSpl.size > 1) {
                val cn = cSpl.last()
                val cJoin = cSpl.take(cSpl.size - 1).joinToString(".")
                val j = getOrCreateJoin(r, cJoin, mapJoinObj)
                field = j.fields.firstOrNull { fl -> fl.name == cn }
                root = j.join
            } else {
                field = this.fields.firstOrNull { fl -> fl.name == fd.fName }
                root = r
            }

            if (field == null) {
                if (fd.o == FilterData.FILTEROP.EQ && fd.fName == fd.v) {
                    null
                } else {
                    throw Exception("field not found ${fd.fi}")
                }
            } else {
                PredicateUtility.generatePredicate(root, cb, fd, field)
            }


        } else null
    }

    private fun getPredicate(name: String, root: From<*, *>, type: Class<*>): Path<out Any> {
        return if (type.isEnum) {
            root.get<Enum<*>>(name)
        } else {
            when (type) {
                String::class.java -> root.get<String>(name)
                Boolean::class.java -> root.get<Boolean>(name)
                Int::class.java, Integer::class.java -> root.get<Int>(name)
                Float::class.java, java.lang.Float::class.java -> root.get<Float>(name)
                Long::class.java, java.lang.Long::class.java -> root.get<Long>(name)
                Double::class.java, java.lang.Double::class.java -> root.get<Double>(name)
                LocalDateTime::class.java -> root.get<LocalDateTime>(name)
                LocalDate::class.java -> root.get<LocalDate>(name)
                LocalTime::class.java -> root.get<LocalDate>(name)
                else -> throw Exception("current data type ${type.name} with name $name is not exist")
            }
        }
    }

    data class Builder<T>(val em: EntityManager, val clazz: Class<T>) {
        private val select = mutableListOf<String>()
        private val joins = mutableMapOf<String, JoinData<*>>()
        private val filterDataList = mutableListOf<FilterData>()
        private val orderList = mutableListOf<QueryOrder>()

        fun select(vararg column: String) = apply {
            this.select.addAll(column.filter { select.any { s -> s != it } })
        }

        fun <X> addJoin(
            name: String,
            clazz: Class<X>,
            join: ((r: From<*, *>, mj: MutableMap<String, JoinObj<*>>) -> From<*, *>)
        ) = apply {
            joins[name] = JoinData(clazz, join)
        }

        fun addSpringUrlSort(sort: Array<String>) {
            sort.forEachIndexed { idx, v ->
                if (v.contains(",")) {
                    val vs = v.split(",")
                    if (vs[1].lowercase() == "desc") {
                        addOrder(vs[0], DIR.DESC)
                    } else {
                        addOrder(vs[0], DIR.ASC)
                    }
                } else if (!(v.lowercase() == "desc" || v.lowercase() == "asc")) {
                    if (sort.size >= idx + 2 && (sort[idx + 1].lowercase() == "asc" || sort[idx + 1].lowercase() == "desc")) {
                        if (sort[idx + 1].lowercase() == "desc") {
                            addOrder(v, DIR.DESC)
                        } else {
                            addOrder(v, DIR.ASC)
                        }
                    } else {
                        addOrder(v, DIR.ASC)
                    }
                }
            }
        }

        fun addOrder(
            fieldName: String,
            dir: DIR
        ) = apply {
            orderList.add(QueryOrder(fieldName, dir))
        }

        fun andFilterData(filterData: FilterData) = apply {
            filterDataList.add(filterData)
        }

        fun andFilterDataJson(filterData: String) = apply {
            filterDataList.add(FilterData.fromJson(filterData))
        }

        fun build(): SimpleQuery<T> {
            return SimpleQuery(em, clazz, select, joins, filterDataList, orderList)
        }
    }
}