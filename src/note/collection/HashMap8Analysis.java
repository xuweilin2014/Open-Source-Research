public class HashMap8Analysis{

    public static class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {

        // HashMap 的默认初始化大小为 16
        static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

        // HashMap 的最大容量
        static final int MAXIMUM_CAPACITY = 1 << 30;

        // 负载因子的大小，一般 HashMap 的扩容的临界点是当前 HashMap 的大小 > DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY 
        static final float DEFAULT_LOAD_FACTOR = 0.75f;

        // 这是 JDK1.8 在底层做的一个优化，当一个 Entry 挂载的节点超过8个，就会将当前 Entry 的链表结构转化为红黑树的数据结构
        static final int TREEIFY_THRESHOLD = 8;

        // 红黑树的最大节点数
        static final int MIN_TREEIFY_CAPACITY = 64;

        // hashmap 使用拉链法来管理其中的每个结点，由 Node 结点组成链表之后，HashMap 定义了一个 Node 数组，记录每个链表中的第一个节点
        transient Node<K,V>[] table;

        static class Node<K,V> implements Map.Entry<K,V> {
            // key 的哈希值
            final int hash;
            // 节点的 key，类型和定义 HashMap 时的 key 相同
            final K key;
            // 节点的 value，类型和定义 HashMap 时的 value 相同
            V value;
            // 该节点的下一节点
            Node<K,V> next;
    
            Node(int hash, K key, V value, Node<K,V> next) {
                this.hash = hash;
                this.key = key;
                this.value = value;
                this.next = next;
            }
    
            public final K getKey()        { return key; }
            public final V getValue()      { return value; }
            public final String toString() { return key + "=" + value; }
    
            public final int hashCode() {
                return Objects.hashCode(key) ^ Objects.hashCode(value);
            }

            public final boolean equals(Object o) {
                if (o == this)
                    return true;
                if (o instanceof Map.Entry) {
                    Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                    if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                        return true;
                }
                return false;
            }
        }

        public HashMap(int initialCapacity) {
            this(initialCapacity, DEFAULT_LOAD_FACTOR);
        }

        public HashMap(int initialCapacity, float loadFactor) {
            if (initialCapacity < 0)
                throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
            if (initialCapacity > MAXIMUM_CAPACITY)
                initialCapacity = MAXIMUM_CAPACITY;
            if (loadFactor <= 0 || Float.isNaN(loadFactor))
                throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
            this.loadFactor = loadFactor;
            this.threshold = tableSizeFor(initialCapacity);
        }

        // 获取到大于或者等于 cap 的最小的且最近的 2 的整数次幂的数，它通过对 cap 中最高位 1 的快速裂变，
        // 来求得 >= cap 的最小最近的 2 的整数次幂。比如，对于 0010 0101 (37) 这个数，求解的过程如下：
        // n |= n >>> 1 ===>  0010 0101 | 0001 0010 = 0011 0111
        // n |= n >>> 2 ===>  0011 0111 | 0000 1101 = 0011 1111
        // n |= n >>> 4 ===>  0011 1111 | 0000 0011 = 0011 1111
        // n |= n >>> 8 ===>  0011 1111 | 0000 0000 = 0011 1111
        // n |= n >>> 16 ===> 0011 1111 | 0000 0000 = 0011 1111
        // 最后，求出的 n = 0011 1111，也就是 n = 63
        static final int tableSizeFor(int cap) {
            int n = cap - 1;
            n |= n >>> 1;
            n |= n >>> 2;
            n |= n >>> 4;
            n |= n >>> 8;
            n |= n >>> 16;
            return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
        }

        // 在 key 调用 hashCode 方法算出的 hash 值上进行扰动，减少碰撞概率
        static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        }

        Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
            return new Node<>(hash, key, value, next);
        }

        public V put(K key, V value) {
            return putVal(hash(key), key, value, false, true);
        }

        final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
            Node<K, V>[] tab;
            Node<K, V> p;
            int n, i;
            // node 表 table 为空则创建
            if ((tab = table) == null || (n = tab.length) == 0)
                n = (tab = resize()).length;
            // i = (n - 1) & hash 计算 hash 值应该处于的下标
            // 如果 tab[i] 为 null，说明没有 hash 冲突，直接创建一个 Node 结点
            if ((p = tab[i = (n - 1) & hash]) == null)
                tab[i] = newNode(hash, key, value, null);
            // 否则，说明发生了 hash 碰撞    
            else {
                Node<K, V> e;
                K k;
                // 由于不同的 hash 值进行取模运算，得出的下标可能是相同的，因此需要比较 hash 值
                // 另外，由于不同的对象所产生的 hash 值可能相同，所以必须比较对象的 key 值
                // 如果存在 hash 值和 key 值都相同的 Node，那么就将其赋值给 e
                if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                    e = p;
                // 判断该链是否为红黑树    
                else if (p instanceof TreeNode)
                    e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
                // 判断该链为链表
                else {
                    // binCount 可以看成是遍历链表的过程中的一个计数器，遍历链表，检查链表中的结点的 hash 和 key 值是否和 node 结点相等，
                    // 如果存在相等的话，直接 break
                    // 如果不存在相等的话，将 node 添加到链表的尾部，如果链表的长度大于 8，则将链表转换为红黑树进行处理
                    for (int binCount = 0;; ++binCount) {
                        // 遍历到了链表的尾部
                        if ((e = p.next) == null) {
                            p.next = newNode(hash, key, value, null);
                            // 链表长度大于 8 转换为红黑树进行处理
                            if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                                treeifyBin(tab, hash);
                            break;
                        }
                        // 比较 e 中的 hash 值以及 key 值与方法中的 hash 值与 key 值，如果都相等的话，要 put 的对象在 hashmap 中存在
                        if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                            break;
                        p = e;
                    }
                }
                // e != null 说明要 put 的对象在 hashmap 中存在，接下来做的事情就是简单地替换值
                if (e != null) { // existing mapping for key
                    V oldValue = e.value;
                    if (!onlyIfAbsent || oldValue == null)
                        e.value = value;
                    afterNodeAccess(e);
                    return oldValue;
                }
            }
            // 运行到这里，说明要 put 的对象在 hashmap 中不存在，所以需要新增对象，size 需要加一
            // 这里需要注意，modCount 表示的是 hashmap 的结构发生的变化次数，如果要 put 的对象在 hashmap 中存在，那么
            // 这个时候只是简单地对旧值进行替换就行了，不需要对 modCount 进行更改
            ++modCount;
            if (++size > threshold)
                resize();
            afterNodeInsertion(evict);
            return null;
        }

        
        final Node<K,V>[] resize() {
            Node<K,V>[] oldTab = table;
            int oldCap = (oldTab == null) ? 0 : oldTab.length;
            int oldThr = threshold;
            int newCap, newThr = 0;

            if (oldCap > 0) {
                // 超过最大值就不再扩充了，就只好随你碰撞去吧
                if (oldCap >= MAXIMUM_CAPACITY) {
                    threshold = Integer.MAX_VALUE;
                    return oldTab;
                // 没超过最大值，就扩充为原来的 2 倍    
                } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY)
                    // 由于新的 capacity 变为原来的两倍，所以直接把 new threshold 变为原来的两倍
                    newThr = oldThr << 1; // double threshold        
            
            // 如果 oldCap 为 0，而 oldThr 不为 0 的话，就把 oldThr 赋值给 newCap 
            } else if (oldThr > 0) 
                newCap = oldThr;

            // 如果 oldCap 和 olThr 都为 0,    
            else {               
                // 初始化 new Capacity 为 DEFAULT_INITIAL_CAPACITY 的值，也就是 16
                newCap = DEFAULT_INITIAL_CAPACITY;
                // new Threshold 为 0.75 * new Capacity
                newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            }

            // 计算新的上限
            if (newThr == 0) {
                float ft = (float)newCap * loadFactor;
                newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ? (int)ft : Integer.MAX_VALUE);
            }

            threshold = newThr;
            @SuppressWarnings({"rawtypes","unchecked"})
            // 使用新的 capacity 大小 newCap，来创建新的 Node 数组 newTab
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
            table = newTab;

            if (oldTab != null) {
                // 把旧的 hashmap 中的每一个元素都保存到新的 hashmap 中去
                for (int j = 0; j < oldCap; ++j) {
                    Node<K,V> e;
                    if ((e = oldTab[j]) != null) {
                        oldTab[j] = null;
                        // 如果 e.next == null，说明在旧的 oldTab 中，下标 j 处只有 e 一个元素，
                        // 并且在 jdk 1.8 中，位于任意某个下标 j 处的元素，rehash 之后，新的下标为 j 或者 j + oldCap，所以旧的 hashmap 中
                        // 只有一个 Node 结点的下标处，在进行 rehash 之后，在新的下标处，也只可能有一个 Node
                        if (e.next == null)
                            newTab[e.hash & (newCap - 1)] = e;
                        else if (e instanceof TreeNode)
                            ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                        else { // preserve order
                            Node<K,V> loHead = null, loTail = null;
                            Node<K,V> hiHead = null, hiTail = null;
                            Node<K,V> next;

                            do {
                                next = e.next;
                                // 原索引
                                if ((e.hash & oldCap) == 0) {
                                    if (loTail == null)
                                        loHead = e;
                                    else
                                        loTail.next = e;
                                    loTail = e;
                                // 原索引 + oldCap    
                                } else {
                                    if (hiTail == null)
                                        hiHead = e;
                                    else
                                        hiTail.next = e;
                                    hiTail = e;
                                }
                            } while ((e = next) != null);

                            // 原索引放到 bucket 里面
                            if (loTail != null) {
                                loTail.next = null;
                                newTab[j] = loHead;
                            }

                            // 原索引 + oldCap 放到 bucket 里面
                            if (hiTail != null) {
                                hiTail.next = null;
                                newTab[j + oldCap] = hiHead;
                            }
                        }
                    }
                }
            }

            return newTab;
        }


        final void treeifyBin(Node<K,V>[] tab, int hash) {
            int n, index; Node<K,V> e;
            // 当因为一个桶中链表的元素，或者说长度大于 8 时，就会尝试将链表转换成红黑树，
            // 但是，如果此时 table 中桶的个数太少（默认小于 64，也就是下面的 MIN_TREEIFY_CAPACITY 被看做是桶太小），那么就会进行扩容，而不是将链表转换成红黑树。
            // 所以，需要同时满足桶中链表元素大于 8 以及桶的数量大于等于 64 这两个条件才会转换成红黑树
            if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
                resize();
            else if ((e = tab[index = (n - 1) & hash]) != null) {
                TreeNode<K,V> hd = null, tl = null;
                do {
                    TreeNode<K,V> p = replacementTreeNode(e, null);
                    if (tl == null)
                        hd = p;
                    else {
                        p.prev = tl;
                        tl.next = p;
                    }
                    tl = p;
                } while ((e = e.next) != null);
                if ((tab[index] = hd) != null)
                    hd.treeify(tab);
            }
        }
    

    }

}