public class ArrayListAnalysis {

    public static class ArrayList<E> {
        // 存储元素的数组
        transient Object[] elementData;

        // 数组中当前元素个数
        private int size;

        // 默认数组长度
        private static final int DEFAULT_CAPACITY = 10;

        private static final Object[] EMPTY_ELEMENTDATA = {};

        private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

        private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    
        /**
         * Constructs an empty list with an initial capacity of ten.
         * 创建一个容量为 10 的 ArrayList，注意，注释是说构造一个容量大小为 10 的空的 list 集合，但构造函数只是给 elementData 赋值了一个空的数组，
         * 其实是在第一次添加元素时容量扩大至 10 的。
         */
        public ArrayList() {
            this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
        }

        public ArrayList(int initialCapacity) {
            if (initialCapacity > 0) {
                this.elementData = new Object[initialCapacity];
            } else if (initialCapacity == 0) {
                // 如果容量为 0，那么都指向同一个共享的空数组，减少内存占用
                this.elementData = EMPTY_ELEMENTDATA;
            } else {
                throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
            }
        }

        // 批量添加
        public boolean addAll(Collection<? extends E> c) {
            // 集合转化为数组
            Object[] a = c.toArray();
            int numNew = a.length;
            // 跟add(E e)中处理方式类似
            ensureCapacityInternal(size + numNew);  // Increments modCount
            // 将集合内的元素复制到 elementData 中，覆盖掉 [size, size + numNew) 的元素
            System.arraycopy(a, 0, elementData, size, numNew);
            size += numNew;
            return numNew != 0;
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
    
            Object[] a = c.toArray();
            int numNew = a.length;
            ensureCapacityInternal(size + numNew);  // Increments modCount
    
            int numMoved = size - index;
            if (numMoved > 0)
                // 将 elementData 中位置为 index 及其以后的元素都向后移动 numNew 个位置
                System.arraycopy(elementData, index, elementData, index + numNew, numMoved);
    
            // 将集合内的元素复制到 elementData 中，覆盖掉 [index, index + numNew) 的元素
            System.arraycopy(a, 0, elementData, index, numNew);
            size += numNew;
            return numNew != 0;
        }

        // 将元素 element 插入到数组中下标为 index 处的位置，并且将数组中下标为 [index, size - 1] 的元素
        // 都向后移动一个位置
        public void add(int index, E element) {
            rangeCheckForAdd(index);
            // 将数组中的元素长度增加 1
            ensureCapacityInternal(size + 1); // Increments modCount!!
            // 使用 System.arraycopy 将 elementData 中 [index, size) 处的元素，拷贝到 elementData 中 [index, size + 1) 处
            System.arraycopy(elementData, index, elementData, index + 1, size - index);
            elementData[index] = element;
            size++;
        }

        public boolean add(E e) {
            // 检查数组容量，当容量不够时，进行扩容
            ensureCapacityInternal(size + 1); // Increments modCount!!
            elementData[size++] = e;
            return true;
        }

        private void ensureCapacityInternal(int minCapacity) {
            // 这里的 minCapacity 为数组的容量 size + 1
            ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
        }

        private static int calculateCapacity(Object[] elementData, int minCapacity) {
            // 先判断当前数组是否为空，也就是判断当前数组中是不是一个元素都没有
            // 如果使用默认的无参构造器，那么实例化的 ArrayList 就为空
            if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
                // 空数组进行扩容，扩容的长度就为 DEFAULT_CAPACITY 与 minCapacity 的较大值
                return Math.max(DEFAULT_CAPACITY, minCapacity);
            }
            return minCapacity;
        }

        private void ensureExplicitCapacity(int minCapacity) {
            modCount++;
            // overflow-conscious code
            // 如果数组的容量不够，就执行扩容
            if (minCapacity - elementData.length > 0)
                grow(minCapacity);
        }

        private void grow(int minCapacity) {
            // overflow-conscious code
            // oldCapacity：当前数组的长度
            // newCapacity：数组扩容之后的长度，等于原始长度的 1.5 倍
            // minCapacity：存储数组至少需要的长度

            int oldCapacity = elementData.length;
            // 先将数组扩展至原来的 1.5 倍
            int newCapacity = oldCapacity + (oldCapacity >> 1);

            // 如果新的容量小于所需容量 将所需容量赋值给新的容量
            // 这种情况会在空数组的时候发生
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            // 如果新的数组长度大于数组的最大长度
            if (newCapacity - MAX_ARRAY_SIZE > 0)
                // 计算新容量
                newCapacity = hugeCapacity(minCapacity);
            // minCapacity is usually close to size, so this is a win:
            elementData = Arrays.copyOf(elementData, newCapacity);
        }

        // 如果所需长度大于数组的最大长度 重新计算数组长度 最大的数组长度可以为 Integer.MAX_VALUE
        private static int hugeCapacity(int minCapacity) {
            if (minCapacity < 0) // overflow
                throw new OutOfMemoryError();

            return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
        }

        E elementData(int index) {
            return (E) elementData[index];
        }

        // 删除 ArrayList 中第一次出现的特定元素
        public boolean remove(Object o) {
            if (o == null) {
                for (int index = 0; index < size; index++)
                    if (elementData[index] == null) {
                        fastRemove(index);
                        return true;
                    }
            } else {
                for (int index = 0; index < size; index++)
                    // 比较对象时依赖 equals 方法
                    // 因此类型变量 E 对应的类注意重写 equlas 方法
                    // 重写时注意遵守规范，具体参考 effective java 第三版的第 10、11 两条规则
                    if (o.equals(elementData[index])) {
                        fastRemove(index);
                        return true;
                    }
            }
            return false;
        }

        private void fastRemove(int index) {
            modCount++;
            // numMoved = size - (index + 1)
            int numMoved = size - index - 1;
            // 将 elemetData 中 index + 1 及其后面的元素都向前移动一个下标
            if (numMoved > 0)
                System.arraycopy(elementData, index+1, elementData, index, numMoved);
            
            // 根据上一步的操作，size - 1 位置的对象向前移动一个下标
            // 如果没有elementData[--size]==null，可能会导致内存泄漏
            // 试想，ArrayList被add了100个对象，然后被remove了100次。按照GC的机制来说，100个对象应该可以被GC掉（假设没有对象对象），但是由于还存在ArrayList的实例引用，对应的100个对象就无法删除
            elementData[--size] = null; 
        }

        // 根据下标删除元素
        // 注意：java5 后引入自动装箱、拆箱的机制，因此产生了一个有趣的问题：
        // 当类型变量为 Integer 的 ArrayList 调用 remove 时，可能调用 remove(Object)，也可能调用 remove(Index)
        // 一定要注意测试是否符合自己的预期
        public E remove(int index) {
            rangeCheck(index);
            modCount++;
            E oldValue = elementData(index);
            int numMoved = size - index - 1;
            // 如果被删除元素不是 ArrayList 的最后一个元素
            if (numMoved > 0)
                // 对应下标之后的元素向前移动一个下标
                System.arraycopy(elementData, index+1, elementData, index, numMoved);
            // 最后一个元素只为 null，方便 GC
            elementData[--size] = null; 
            return oldValue;
        }

        /**
         * Save the state of the ArrayList instance to a stream (that is,
         * serialize it).
         *
         * @serialData The length of the array backing the ArrayList instance
         *             is emitted (int), followed by all of its elements (each an
         *             Object) in the proper order.
         */
        private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
            // Write out element count, and any hidden stuff
            int expectedModCount = modCount;
            // 默认的序列化策略，序列化其它的字段
            s.defaultWriteObject();
            // 采用实际的长度，而不是容量
            // Write out size as capacity for behavioural compatibility with clone()
            s.writeInt(size);

            // Write out all elements in the proper order.
            // 只序列化数组的前 size 个对象
            for (int i = 0; i < size; i++) {
                s.writeObject(elementData[i]);
            }

            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        private class Itr implements Iterator<E> {
            // cursor: 代表下一个要访问的元素下标
            int cursor;       // index of next element to return
            // lastRet: 代表上一个已经访问过的元素下标
            int lastRet = -1; // index of last element returned; -1 if no such
            // modCount： 代表了对 ArrayList 修改的次数，类似于版本号，每修改一次，就会发生变化。而 expectedModCount 相当于一个快照，用来记录某一时刻 ArrayList 被修改的次数
            // 之后如果 expectedModCount 和 modCount 不相等，则说明在迭代器迭代的过程中，ArrayList 的结构发生了变化。
            int expectedModCount = modCount;

            Itr() {}

            public boolean hasNext() {
                return cursor != size;
            }

            @SuppressWarnings("unchecked")
            public E next() {
                checkForComodification();
                int i = cursor;
                if (i >= size)
                    throw new NoSuchElementException();
                Object[] elementData = ArrayList.this.elementData;
                // i >= elementData.length 说明 ArrayList 的长度结构发生了变化，因此是迭代器遍历的同时有修改
                if (i >= elementData.length)
                    throw new ConcurrentModificationException();
                cursor = i + 1;
                // 这里 cursor 和 lastRet 都增加一
                return (E) elementData[lastRet = i];
            }

            public void remove() {
                // lastRet 如果小于 0，说明在调用 remove 方法之前，没有调用 next
                if (lastRet < 0)
                    throw new IllegalStateException();
                checkForComodification();

                try {
                    // 使用 ArrayList 中的 remove 方法来移除指定下标的元素
                    ArrayList.this.remove(lastRet);
                    // 因为移除了 lastRet 处的元素之后，上面 remove 方法会把 lastRet 之后的元素移动到前面。所以 cursor 会继续指向 lastRet
                    cursor = lastRet;
                    // lastRet 被赋值指向 -1，表示要调用 remove 方法之前必须要调用 next 方法，否则，就会抛出异常
                    lastRet = -1;
                    expectedModCount = modCount;
                } catch (IndexOutOfBoundsException ex) {
                    throw new ConcurrentModificationException();
                }
            }

            // 调用 remove 和 next 方法之前，先必须比较 modCount 和 expectedModCount 大小
            final void checkForComodification() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }
    }

    public static class LinkedList{

        private static class Node<E> {
            E item;
            Node<E> next;
            Node<E> prev;
    
            Node(Node<E> prev, E element, Node<E> next) {
                this.item = element;
                this.next = next;
                this.prev = prev;
            }
        }

        transient int size = 0;

        /**
         * Pointer to first node. Invariant: (first == null && last == null) ||
         * (first.prev == null && first.item != null)
         */
        transient Node<E> first;

        /**
         * Pointer to last node. Invariant: (first == null && last == null) ||
         * (last.next == null && last.item != null)
         */
        transient Node<E> last;

        // LinkedList() 仅仅是一个空构造方法
        public LinkedList() {
        }

public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}

        // 将新元素 e 添加到链表的尾部
        public boolean add(E e) {
            linkLast(e);
            return true;
        }

        // 将元素添加到链表的尾部
        void linkLast(E e) {
            final Node<E> l = last;
            // 新增一个结点 Node，并且设置其前后引用
            final Node<E> newNode = new Node<>(l, e, null);
            // 设置链表的最后一个结点 last 为 newNode
            last = newNode;
            // 如果 newNode 为第一个结点，那么就将 first 也设置成 newNode
            if (l == null)
                first = newNode;
            // 设置之前的最后的结点的 next 指针指向 newNode    
            else
                l.next = newNode;
            size++;
            modCount++;
        }

        public void add(int index, E element) {
            // 判断 index >= 0 && index <= size
            checkPositionIndex(index);
            // 如果 index = size，就说明要在原来链表的最后附加上元素
            if (index == size)
                linkLast(element);
            // 将 element 插入到链表第 index 个节点的位置   
            else
                linkBefore(element, node(index));
        }

        void linkBefore(E e, Node<E> succ) {
            // succ 即为链表中在 index 下标处的结点
            final Node<E> pred = succ.prev;
            // 创建一个新的结点，其结点值为 e，前后指向分别是 pred 和 succ，相当于将节点 newNode
            // 插入到 succ 结点之前
            final Node<E> newNode = new Node<>(pred, e, succ);
            succ.prev = newNode;
            // pred == null 就说明 succ 这个节点是链表第一个结点
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            size++;
            modCount++;
        }

        public E get(int index) {
            checkElementIndex(index);
            return node(index).item;
        }

        // 返回特定下标的链表结点
        Node<E> node(int index) {
            // 如果 index 位于链表的前半段，那么就从 first 结点开始往后遍历；
            // 如果 index 位于链表的后半段，那么就从 last 结点开始往前遍历
            if (index < (size >> 1)) {
                Node<E> x = first;
                for (int i = 0; i < index; i++)
                    x = x.next;
                return x;
            } else {
                Node<E> x = last;
                for (int i = size - 1; i > index; i--)
                    x = x.prev;
                return x;
            }
        }

        public E remove(int index) {
            checkElementIndex(index);
            // node(index) 返回链表中位于 index 索引处的 node
            return unlink(node(index));
        }

        E unlink(Node<E> x) {
            // assert x != null;
            final E element = x.item;
            final Node<E> next = x.next;
            final Node<E> prev = x.prev;

            // prev == null，说明这个 x 元素是链表中的第一个元素
            if (prev == null) {
                first = next;
            } else {
                prev.next = next;
                // 释放结点前的一个结点
                x.prev = null;
            }

            // next == null，说明这个 x 元素是链表中的最后一个元素
            if (next == null) {
                last = prev;
            } else {
                next.prev = prev;
                // 释放结点前的后一个结点
                x.next = null;
            }

            x.item = null;
            size--;
            modCount++;
            return element;
        }

        // 判断要移除的元素是否为 null，然后在遍历链表，找到钙元素第一次出现的位置，移除并返回 true
        public boolean remove(Object o) {
            if (o == null) {
                for (Node<E> x = first; x != null; x = x.next) {
                    if (x.item == null) {
                        unlink(x);
                        return true;
                    }
                }
            } else {
                for (Node<E> x = first; x != null; x = x.next) {
                    if (o.equals(x.item)) {
                        unlink(x);
                        return true;
                    }
                }
            }
            return false;
        }

        public ListIterator<E> listIterator(int index) {
            checkPositionIndex(index);
            return new ListItr(index);
        }

        private class ListItr implements ListIterator<E> {
            // lastReturned 表示已经返回的结点
            private Node<E> lastReturned;
            // 下一个要返回的结点
            private Node<E> next;
            // 下一个要返回的结点的下标
            private int nextIndex;
            private int expectedModCount = modCount;

            // 调用 LinkedList 中的 iterator 方法的调用链如下所示：
            // AbstractSequentialList#iterator() -> AbstractList#listIterator() -> LinkedList#listIterator(index)（这里的 index 为 0）
            ListItr(int index) {
                // 如果 index == size 条件为真，说明 size 为 0，LinkedList 为一个空链表
                // 所以 next 为 null
                next = (index == size) ? null : node(index);
                nextIndex = index;
            }

            public boolean hasNext() {
                return nextIndex < size;
            }

            public E next() {
                checkForComodification();
                if (!hasNext())
                    throw new NoSuchElementException();

                // 将 next 赋值给 lastReturned，同时将 next 和 nextIndex 均向后移动一位
                lastReturned = next;
                next = next.next;
                nextIndex++;
                return lastReturned.item;
            }

            public boolean hasPrevious() {
                return nextIndex > 0;
            }

            public E previous() {
                checkForComodification();
                if (!hasPrevious())
                    throw new NoSuchElementException();

                // 如果 next 为 null，next 指向的是链表最后一个结点后面的 null，因此其前面一个结点为 last
                lastReturned = next = (next == null) ? last : next.prev;
                nextIndex--;
                return lastReturned.item;
            }

            public int nextIndex() {
                return nextIndex;
            }

            public int previousIndex() {
                return nextIndex - 1;
            }
        
    
            public void remove() {
                checkForComodification();
                if (lastReturned == null)
                    throw new IllegalStateException();
    
                Node<E> lastNext = lastReturned.next;
                unlink(lastReturned);
                if (next == lastReturned)
                    next = lastNext;
                else
                    nextIndex--;
                lastReturned = null;
                expectedModCount++;
            }
    
            public void set(E e) {
                if (lastReturned == null)
                    throw new IllegalStateException();
                checkForComodification();
                lastReturned.item = e;
            }
    
            public void add(E e) {
                checkForComodification();
                lastReturned = null;
                if (next == null)
                    linkLast(e);
                else
                    linkBefore(e, next);
                nextIndex++;
                expectedModCount++;
            }

            final void checkForComodification() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

    }

}