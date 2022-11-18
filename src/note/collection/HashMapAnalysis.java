public class HashMapAnalysis{

    interface Entry<K,V> {
        K getKey();

        V getValue();

        V setValue(V value);

        boolean equals(Object o);

        int hashCode();
    }

    /**
     * JDK 1.6
     */
    public static class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
        
        // hashmap 中 table 数组的默认容量为 16
        static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
        // table 数组的最大容量
        static final int MAXIMUM_CAPACITY = 1 << 30;
        // hashmap 的加载因子，默认为 0.75，当 hashmap 中元素的数量超过 容量*加载因子 的时候，HashMap 就会进行扩容
        static final float DEFAULT_LOAD_FACTOR = 0.75f;
        // hashmap 使用拉链法管理其中的每一个节点
        transient Entry[] table;
        // size 是 hashmap 中包含的键值对的数目
        transient int size;
        // threshold 是 table 数组中扩容的标准，达到这个值就会进行扩容，默认的值为：容量 * 加载因子
        int threshold;

        final float loadFactor;
        // modCount 记录 HashMap 被修改的次数。包括插入删除等操作。这里要注意，只有在操作修改了 hashmap 的结构时，才会
        // 更改 modCount 的值，当 put 元素时，如果只是单纯的对原有的元素进行替换，那么就不会更改 modCount
        transient volatile int modCount;

        /**
         * Entry其实就是封装了key和value，也就是我们put方法参数的key和value会被封装成Entry，然后放到table这个Entry数组中。
         * 但值得注意的是，它有一个类型为Entry的next，它是用于指向下一个Entry的引用，所以table中存储的是Entry的单向链表。
         */
        static class Entry<K,V> implements Map.Entry<K,V> {
            final K key;
            V value;
            Entry<K,V> next;
            final int hash;
    
            /**
             * Creates new entry.
             */
            Entry(int h, K k, V v, Entry<K,V> n) {
                value = v;
                next = n;
                key = k;
                hash = h;
            }
    
            public final K getKey() {
                return key;
            }
    
            public final V getValue() {
                return value;
            }
    
            public final V setValue(V newValue) {
            V oldValue = value;
                value = newValue;
                return oldValue;
            }
    
            public final boolean equals(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry e = (Map.Entry)o;
                Object k1 = getKey();
                Object k2 = e.getKey();
                if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                    Object v1 = getValue();
                    Object v2 = e.getValue();
                    if (v1 == v2 || (v1 != null && v1.equals(v2)))
                        return true;
                }
                return false;
            }
    
            public final int hashCode() {
                return (key==null   ? 0 : key.hashCode()) ^
                       (value==null ? 0 : value.hashCode());
            }
    
            public final String toString() {
                return getKey() + "=" + getValue();
            }
        }

        public HashMap() {
            this.loadFactor = DEFAULT_LOAD_FACTOR;
            threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
            table = new Entry[DEFAULT_INITIAL_CAPACITY];
            init();
        }

        public HashMap(int initialCapacity) {
            this(initialCapacity, DEFAULT_LOAD_FACTOR);
        }

        public HashMap(int initialCapacity, float loadFactor) {
            // 判断 initialCapacity 是否合法
            if (initialCapacity < 0)
                throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
            if (initialCapacity > MAXIMUM_CAPACITY)
                initialCapacity = MAXIMUM_CAPACITY;
            // 判断 loadFactor 是否合法
            if (loadFactor <= 0 || Float.isNaN(loadFactor))
                throw new IllegalArgumentException("Illegal load factor: " + loadFactor);

            // 找到第一个大于等于 initialCapacity 的 2 的平方的数
            int capacity = 1;
            while (capacity < initialCapacity)
                capacity <<= 1;

            this.loadFactor = loadFactor;
            // HashMap 扩容的阀值，值为 HashMap 的当前容量 * 负载因子，默认为 12 = 16 * 0.75
            threshold = (int)(capacity * loadFactor);
            // 初始化 table 数组，这是 HashMap 真实的存储容器
            table = new Entry[capacity];
            // 该方法为空实现，主要是给子类去实现
            init();
        }

        public V put(K key, V value) {
            // 对值为 null 的 key 进行处理
            if (key == null)
                return putForNullKey(value);
            // 根据 key 算出 hash 值
            int hash = hash(key.hashCode());
            // 根据 hash 值和 HashMap 容量算出在 table 中应该存储的下标
            // 等价于 hash % length = hash & (length - 1)
            int i = indexFor(hash, table.length);
            // 遍历位于下标 i 处的链表
            for (Entry<K,V> e = table[i]; e != null; e = e.next) {
                Object k;
                // 先判断 hash 值是否一样，如果一样，再判断 key 是否一样
                // key 和 hash 值都相同的话，说明要存储的 key 已经存在于 HashMap 中，这时需要替换已经存在 Entry 的 value 值即可
                // 为什么比较了 hash 值还需要比较 key，因为不同的对象 hash 值可能一样
                if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                    V oldValue = e.value;
                    e.value = value;
                    e.recordAccess(this);
                    // 替换掉旧值时，不会对 modCount 进行更改
                    return oldValue;
                }
            }

            modCount++;
            addEntry(hash, key, value, i);
            return null;
        }

        static int indexFor(int h, int length) {
            // 取余运算符为 % 。但在以前，CPU采用如下方法计算余数（注意，该方法只对2的N次方数系有效）：X & (2^N - 1)
            // 所以，这里也就是解释了前面为什么需要 table 的长度 length 为 2 的整数次幂
            return h & (length-1);
        }

        // bucketIndex 表示这个 entry 在 table 中的下标
        void addEntry(int hash, K key, V value, int bucketIndex) {
            Entry<K,V> e = table[bucketIndex];
            // 头插法，也就是 table[bucketIndex] 的头结点会变成新创建的 Entry 结点
            // 这也说明了，当 hash 冲突时，采用的拉链法来解决 hash 冲突的，并且是把新元素是插入到单边表的表头
            table[bucketIndex] = new Entry<K,V>(hash, key, value, e);
            if (size++ >= threshold)
                resize(2 * table.length);
        }

        void resize(int newCapacity) {
            Entry[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }

            Entry[] newTable = new Entry[newCapacity];
            // 将旧的 table 上的 entry 转移到新的 table 上去
            transfer(newTable);
            table = newTable;
            threshold = (int)(newCapacity * loadFactor);
        }

        /**
         * Transfers all entries from current table to newTable.
         * 将旧 table 中的所有 entry 转移到新 table 中
         */
        void transfer(Entry[] newTable) {
            Entry[] src = table;
            int newCapacity = newTable.length;
            for (int j = 0; j < src.length; j++) {
                Entry<K, V> e = src[j];
                if (e != null) {
                    // 旧的 table 释放对 entry 链表的引用
                    src[j] = null;
                    // 下面的循环，就是将旧的 table 中的 entry 转移到新的 table 中，这里需要注意的是，旧的 table 
                    // 中 hash 值不相同的 entry 可能在新的 table 中相同
                    do {
                        Entry<K, V> next = e.next;
                        int i = indexFor(e.hash, newCapacity);
                        e.next = newTable[i];
                        newTable[i] = e;
                        e = next;
                    } while (e != null);
                }
            }
        }

        /**
         * Offloaded version of put for null keys
         */
        private V putForNullKey(V value) {
            // 默认取 table 数组中 index 为 0 的位置的链表，如果发现 key 为 null 的 entry，那么就替换掉 value
            // 也就是 key 为 null 的 entry 在 hashmap 中只能保存一个
            for (Entry<K, V> e = table[0]; e != null; e = e.next) {
                if (e.key == null) {
                    V oldValue = e.value;
                    e.value = value;
                    e.recordAccess(this);
                    return oldValue;
                }
            }

            // 如果遍历完之后都没有匹配，就 modCount 自增，调用 addEntry 方法
            modCount++;
            addEntry(0, null, value, 0);
            return null;
        }

        // 从 hashmap 中查找键为 key 的元素 
        public V get(Object key) {
            if (key == null)
                return getForNullKey();
            int hash = hash(key.hashCode());
            for (Entry<K,V> e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
                Object k;
                if (e.hash == hash && ((k = e.key) == key || key.equals(k)))
                    return e.value;
            }
            return null;
        }

        // 从 hashmap 中查找键值为 null 的元素
        private V getForNullKey() {
            for (Entry<K,V> e = table[0]; e != null; e = e.next) {
                if (e.key == null)
                    return e.value;
            }
            return null;
        }

        public V remove(Object key) {
            Entry<K,V> e = removeEntryForKey(key);
            return (e == null ? null : e.value);
        }

        final Entry<K,V> removeEntryForKey(Object key) {
            int hash = (key == null) ? 0 : hash(key.hashCode());
            int i = indexFor(hash, table.length);
            Entry<K,V> prev = table[i];
            Entry<K,V> e = prev;
    
            // 不断遍历 table[i] 处的链表，查找符合条件的 entry 进行删除
            while (e != null) {
                Entry<K,V> next = e.next;
                Object k;
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                    modCount++;
                    size--;
                    // prev == e 说明 e 是链表中的第一个元素，因此把 table[i] 设置成 next（因为 e 要被删除）
                    if (prev == e)
                        table[i] = next;
                    else
                        prev.next = next;
                    e.recordRemoval(this);
                    return e;
                }
                prev = e;
                e = next;
            }
    
            return e;
        }

        public Set<Map.Entry<K,V>> entrySet() {
            return entrySet0();
        }

        private Set<Map.Entry<K,V>> entrySet0() {
            Set<Map.Entry<K,V>> es = entrySet;
            return es != null ? es : (entrySet = new EntrySet());
        }

        private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {

            public Iterator<Map.Entry<K,V>> iterator() {
                return newEntryIterator();
            }

            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<K,V> e = (Map.Entry<K,V>) o;
                Entry<K,V> candidate = getEntry(e.getKey());
                return candidate != null && candidate.equals(e);
            }

            public boolean remove(Object o) {
                return removeMapping(o) != null;
            }

            public int size() {
                return size;
            }

            public void clear() {
                HashMap.this.clear();
            }

        }

        Iterator<Map.Entry<K,V>> newEntryIterator()   {
            return new EntryIterator();
        }

        private final class EntryIterator extends HashIterator<Map.Entry<K,V>> {
            public Map.Entry<K,V> next() {
                return nextEntry();
            }
        }

        private abstract class HashIterator<E> implements Iterator<E> {
            Entry<K,V> next;	// next entry to return
            int expectedModCount;	// For fast-fail
            int index;		// current slot
            Entry<K,V> current;	// current entry
    
            HashIterator() {
                // 设置 expectedModCount 等于 modCount，用于判断迭代器迭代时，是否同时对 hashmap 进行了修改
                expectedModCount = modCount;
                // size > 0 表示当前 hashmap 中 entry 的数目大于 0
                if (size > 0) { // advance to first entry
                    Entry[] t = table;
                    // 这里其实就是遍历 table，找到第一个返回的 Entry next
                    // 该值是 table 数组的第一个有值的 Entry，所以也肯定是单向链表的表头
                    while (index < t.length && (next = t[index++]) == null) ;
                }
            }
    
            public final boolean hasNext() {
                // 如果下一个返回的 Entry 不为 null，则返回 true
                return next != null;
            }
    
            final Entry<K,V> nextEntry() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                // 保存下一个需要返回的 entry，作为返回结果
                Entry<K,V> e = next;
                if (e == null)
                    throw new NoSuchElementException();
                // 如果遍历到 table 上单向链表的最后一个元素时
                if ((next = e.next) == null) {
                    Entry[] t = table;
                    // 继续往下寻找 table 上有元素的下标
                    // 并且把下一个 table 上有单向链表的表头，作为下一个返回的 entry next
                    while (index < t.length && (next = t[index++]) == null)
                        ;
                }
                current = e;
                return e;
            }
    
            public void remove() {
                if (current == null)
                    throw new IllegalStateException();
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                Object k = current.key;
                current = null;
                HashMap.this.removeEntryForKey(k);
                expectedModCount = modCount;
            }
    
        }

    }

}