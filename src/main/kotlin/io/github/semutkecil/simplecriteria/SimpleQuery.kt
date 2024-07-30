package io.github.semutkecil.simplecriteria

import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.*
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SimpleQuery<T> private constructor(
    private val entityManager: EntityManager,
    private val clazz: Class<T>,
    private val select: List<String>,
    private val joins: Map<String, JoinData<*>>,
    private val filterDataList: MutableList<FilterData>,
    private val orderList: MutableList<QueryOrder>
) {
    private val cb: CriteriaBuilder = entityManager.criteriaBuilder
    private val fields = collectEntityField(clazz)
    private val mapSelect = mutableMapOf<String, Path<out Any>>()

    private val cacheKey: CacheKey = CacheKey(
        clazz.name,
        select,
        joins.map { it.key }.toList(),
        FilterData.and(*filterDataList.toTypedArray()).toJson(),
        orderList
    )

    enum class DIR { ASC, DESC }

    fun createQuery(): TypedQuery<Tuple> {
        return entityManager.createQuery(applyQuery())
    }

    private fun collectEntityField(entityClass: Class<T>): MutableList<Field> {
        val listFields = mutableListOf<Field>()
        listFields.addAll(entityClass.declaredFields.toList())
        var parent: Class<in T> = entityClass
        var hasChild = true;
        while (hasChild) {
            try {
                parent = parent.superclass
                listFields.addAll(parent.declaredFields.toList())
            } catch (e: Exception) {
                hasChild = false
            }
        }

        return listFields;
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

    fun resultOneMap(): Map<String, *>? {
        return resultListMap(limit = 1).firstOrNull()
    }

    fun resultListMap(limit: Int? = null, offset: Int = 0): List<Map<String, *>> {
        cacheKey.limit = limit
        cacheKey.offset = offset
        val stringCacheKey = cacheKey.toString()
        if (isValidCache(cacheKey)) {
//            println("return cached data")
            return cacheData[stringCacheKey]!!.data;
        }

        val executor = if (executorMap.contains(cacheKey.toString())) {
            executorMap[cacheKey.toString()]
        } else {
            executorMap.put(cacheKey.toString(), Executors.newSingleThreadExecutor())
            executorMap[cacheKey.toString()]
        }
        val cv = CompletableFuture<List<Map<String, *>>>()
        executor!!.execute {
            try {
//                println("execute query data")
                if (isValidCache(cacheKey)) {
//                    println("return cached data")
                    cv.complete(cacheData[stringCacheKey]!!.data)
                    return@execute
                }
                val query = entityManager.createQuery(applyQuery())
                query.firstResult = offset
                if (limit != null) {
                    query.maxResults = limit
                }
                val slc = select.filter { !it.contains(".") }.toMutableList()
                slc.addAll(select.filter { it.contains(".") })
                val res = query.resultList.map {
                    slc.mapIndexed { idx, str ->
                        Pair(str, it.get(idx))
                    }.toMap()
                }
                cacheData[stringCacheKey] = DataCache(data = res, created = Instant.now())
                cv.complete(res)
            } catch (e: Exception) {
                throw e
            }
        }
        return cv.get()
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

    private fun isValidCache(cacheKey: CacheKey): Boolean {
        val stringCacheKey = cacheKey.toString()
        val res = if (cacheData.containsKey(stringCacheKey)) {
//            println("matching")
            val data = cacheData[stringCacheKey]
            if (cacheUpdate.containsKey(cacheKey.className)) {
//                println("expired check")
                data!!.created.isAfter(cacheUpdate[cacheKey.className])
            }
            true
        } else {
            false
        }

        if (res && executorMap[stringCacheKey]?.isShutdown == false) {
            executorMap[stringCacheKey]?.shutdown()
            executorMap.remove(stringCacheKey)
        }

        return res
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

    companion object {
        //        val singleThreadedExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val executorMap: ConcurrentHashMap<String, ExecutorService> = ConcurrentHashMap()
        val cacheData: ConcurrentHashMap<String, DataCache> = ConcurrentHashMap()
        private val cacheUpdate = mutableMapOf<String, Instant>()
        fun <T> reloadCache(clazz: Class<T>) {

            cacheUpdate[clazz.name] = Instant.now()
        }
    }

    data class Builder<T>(val em: EntityManager, val clazz: Class<T>) {
        private val select = mutableListOf<String>()
        private val joins = mutableMapOf<String, JoinData<*>>()
        private val filterDataList = mutableListOf<FilterData>()
        private val orderList = mutableListOf<QueryOrder>()

        fun select(vararg column: String) = apply {
            this.select.addAll(column.filter { !select.contains(it) })
        }

        fun <X> addJoin(
            name: String,
            clazz: Class<X>,
            join: ((r: From<*, *>, mj: MutableMap<String, JoinObj<*>>) -> From<*, *>)
        ) = apply {
            joins[name] = JoinData(clazz, join)
        }

        private fun <X, Y> addJoinLv1(
            name: String,
            clazzParent: Class<X>,
            clazzJoin: Class<Y>
        ) = apply {
            addJoin(name, clazzJoin) { r, _ ->
                r.join<X, Y>(name, JoinType.LEFT)
            }
        }

        private fun <X, Y> addJoinLvUp(
            name: String,
            clazzParent: Class<X>,
            clazzJoin: Class<Y>
        ) = apply {
            addJoin(name, clazzJoin) { _, m ->
                val nameSplit = name.split(".")
                m[nameSplit.take(nameSplit.size - 1).joinToString(".")]?.join?.join<X, Y>(
                    nameSplit.last(),
                    JoinType.LEFT
                )
                    ?: throw Exception("join not found")
            }
        }

        private fun addJoin(splName: List<String>) {
            var parentClass: Class<*> = clazz
            var joinName = ""
            splName.forEachIndexed { i, fn ->
                if (joinName == "") {
                    joinName = fn
                } else {
                    joinName += ".$fn"
                }

                var childClass = parentClass.getDeclaredField(fn).type
                if (MutableCollection::class.java.isAssignableFrom(childClass)) {
                    val fieldType = parentClass.getDeclaredField(fn).genericType as ParameterizedType
                    childClass = fieldType.actualTypeArguments[0] as Class<*>
                }

                if (joins[joinName] == null) {
                    if (i == 0) {
                        addJoinLv1(joinName, parentClass, childClass)
                    } else {
                        addJoinLvUp(joinName, parentClass, childClass)
                    }
                }
                parentClass = childClass
            }
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
            select.filter { it.contains(".") }.forEach {
                val jSplit = it.split(".")
                addJoin(jSplit.take(jSplit.size - 1))
            }

            orderList.map { it.fieldName }.filter { it.contains(".") }.forEach {
                val jSplit = it.split(".")
                addJoin(jSplit.take(jSplit.size - 1))
            }

            filterDataList.forEach { f ->
                val flattenF = f.flattenField()
                flattenF.filter { it.contains(".") }.forEach {
                    val jSplit = it.split(".")
                    addJoin(jSplit.take(jSplit.size - 1))
                }
            }

            return SimpleQuery(em, clazz, select, joins, filterDataList, orderList)
        }
    }
}