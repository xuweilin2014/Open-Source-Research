public class GetInstanceAnalysis{

    public static class GetInstance {
        /*
         * type 字符串表示 Engine 的类型，比如 MessageDigest；clazz 表示 Spi 类，比如 MessageDigestSpi 类；algorithm 表示算法的类型，比如 MD5
         * 
         * For all the getInstance() methods below:
         * 
         * @param type the type of engine (e.g. MessageDigest)
         * @param clazz the Spi class that the implementation must subclass (e.g. MessageDigestSpi.class) or null if no superclass check is required
         * @param algorithm the name of the algorithm (or alias), e.g. MD5
         * @param provider the provider (String or Provider object)
         * @param param the parameter to pass to the Spi constructor (for CertStores)
         *
         * There are overloaded methods for all the permutations.
         */
        public static Instance getInstance(String type, Class<?> clazz, String algorithm) throws NoSuchAlgorithmException {
            // in the almost all cases, the first service will work avoid taking long path if so
            // 调用 getProviderList 方法从 Providers 中获取到 ProviderList。这时需要注意，如果是第一次调用 Providers 的方法（从 Providers 这个后缀为 s 的
            // 名称看出，这是一个工具类，方法大多数都为 static 静态方法），那么就要进入 Providers 类的静态初始化块，调用 ProviderList 构造方法，从 java.security 
            // 文件中获取到提供者 Provider 的类名，并且封装生成一个 ProviderConfig，添加到 ProviderList#configs 数组中，最后 ProviderList 构造方法执行结束，
            // 返回一个 ProviderList 对象保存到 Providers#providerList 属性
            ProviderList list = Providers.getProviderList();
            // 遍历每一个 ProviderList，从 Provider 中根据 type 和 algorithm 找到的第一个对应的 Service
            Service firstService = list.getService(type, algorithm);
            if (firstService == null) {
                throw new NoSuchAlgorithmException(algorithm + " " + type + " not available");
            }

            NoSuchAlgorithmException failure;
            try {
                return getInstance(firstService, clazz);
            } catch (NoSuchAlgorithmException e) {
                failure = e;
            }
            // if we cannot get the service from the preferred provider, fail over to the next
            for (Service s : list.getServices(type, algorithm)) {
                if (s == firstService) {
                    // do not retry initial failed service
                    continue;
                }
                try {
                    return getInstance(s, clazz);
                } catch (NoSuchAlgorithmException e) {
                    failure = e;
                }
            }
            throw failure;
        }

        /*
         * The two getInstance() methods below take a service. They are intended for classes that cannot use the standard methods, e.g.
         * because they implement delayed provider selection like the Signature class.
         */
        public static Instance getInstance(Service s, Class<?> clazz) throws NoSuchAlgorithmException {
            // 将 Service 中对应的算法实现类（Service 中保存了类名 className，比如 sun.security.provider.MD5），通过反射调用构造函数初始化得到实例
            Object instance = s.newInstance(null);
            // 检查算法类实例是否继承了对应 Engine 的 Spi 类，比如 sun.security.provider.MD5 是否继承了 MessageDigestSpi
            checkSuperClass(s, instance.getClass(), clazz);
            // Instance 类对象包含两个属性，算法的提供者 provider 和算法的实现类 instance
            return new Instance(s.getProvider(), instance);
        }

        /**
         * Check is subClass is a subclass of superClass. If not, throw a NoSuchAlgorithmException.
         */
        public static void checkSuperClass(Service s, Class<?> subClass, Class<?> superClass) throws NoSuchAlgorithmException {
            if (superClass == null) {
                return;
            }
            if (superClass.isAssignableFrom(subClass) == false) {
                throw new NoSuchAlgorithmException("class configured for " + s.getType() + ": " + s.getClassName() + " not a " + s.getType());
            }
        }

        

        /**
         * Return a List of all the available Services that implement any of
         * the specified algorithms. See getServices(String, String) for detals.
         */
        public static List<Service> getServices(List<ServiceId> ids) {
            ProviderList list = Providers.getProviderList();
            return list.getServices(ids);
        }

        /**
         * Static inner class representing a newly created instance.
         */
        public static final class Instance {
            // public final fields, access directly without accessors
            public final Provider provider;
            public final Object impl;

            private Instance(Provider provider, Object impl) {
                this.provider = provider;
                this.impl = impl;
            }

            // Return Provider and implementation as an array as used in the
            // old Security.getImpl() methods.
            public Object[] toArray() {
                return new Object[] { impl, provider };
            }
        }
    }

}