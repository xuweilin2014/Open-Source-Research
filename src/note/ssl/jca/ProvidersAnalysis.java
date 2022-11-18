public class ProvidersAnalysis{

    public static abstract class Provider extends Properties {

        // built in knowledge of the engine types shipped as part of the JDK
        private static final Map<String,EngineDescription> knownEngines;

        /**
         * 在 Provider 的子类比如 Sun、SunJCE、SunJSSE 类，在这些子类中需要保存 Engine.Alg -> Class 的映射，这种映射有两种保存方式，
         * 
         * 第一种，也就是之前遗留的一种方式（legacy），就是直接保存字符串，比如 "MessageDigest.MD5" -> "sun.security.provider.MD5"，最后这对
         * key -> value 字符串会被保存到 legacyStrings 中。另外必须说明的是，legacyStrings 中的所有 key/value 字符串最后都会被转换成 ServiceKey -> Service
         * 键值对，保存到 legacyMap 中
         * 
         * 第二种，直接调用 Provider#putService 方法保存 Service 对象。比如下面这种：
         * 
         * public final class MyProvider extends Provider {
         * 
         *      public MyProvider() {
         *          super("MyProvider", "1.0", "Some info about my provider and which algorithms it supports");
         *          putService(new Provider.Service(this, "Cipher", "MyCipher", "p.MyCipher", null, null));
         *      }
         * }
         * 
         * 最后会根据 Service 对象中的属性值生成 ServiceKey，并且把 ServiceKey -> Service 保存到 serviceMap 中
         */

        // Map<String,String>
        private transient Map<String, String> legacyStrings;

        // Map<ServiceKey,Service>
        // used for services added via putService(), initialized on demand
        private transient Map<ServiceKey, Service> serviceMap;

        // Map<ServiceKey,Service>
        // used for services added via legacy methods, init on demand
        private transient Map<ServiceKey, Service> legacyMap;

        // ServiceKey from previous getService() call by re-using it if possible we avoid allocating a new object
        // and the toUpperCase() call. re-use will occur e.g. as the framework traverses the provider
        // list and queries each provider with the same values until it finds a matching service
        // previousKey 是 static 静态类型的，所有的 Provider 类对象中只包含一个。在 Provider 列表中查找算法对应的 Service 类时，
        // 要么是通过 serviceMap.get 方法获取到，要么是通过 legacyMap.get 方法，而 get 方法的参数就是 ServiceKey。在 Provider 列表中查找
        // 时，是通过同一个 ServiceKey（其中的 type 和 algorithm 属性相同），因此复用同一个 previousKey 就避免了每次调用 Provider#getService 
        // 方法都要创建一个 ServiceKey 对象
        private static volatile ServiceKey previousKey = new ServiceKey("", "", false);

        private final static String ALIAS_PREFIX = "Alg.Alias.";

        private final static String ALIAS_PREFIX_LOWER = "alg.alias.";

        private final static int ALIAS_LENGTH = ALIAS_PREFIX.length();

        /**
         * Provider 类在初始化的时候，会把系统中默认的 Engine 类信息封装到类 EngineDescription 中，并保存到 knownEngines 中。其中 name
         * 表示 Engine 类的名称，而 supportsParameter 表示此 Engine 类的构造函数是否有参数，paramName 表示构造函数参数的类名
         */
        static {
            knownEngines = new HashMap<String,EngineDescription>();
            // JCA
            addEngine("AlgorithmParameterGenerator",        false, null);
            addEngine("AlgorithmParameters",                false, null);
            addEngine("KeyFactory",                         false, null);
            addEngine("KeyPairGenerator",                   false, null);
            addEngine("KeyStore",                           false, null);
            addEngine("MessageDigest",                      false, null);
            addEngine("SecureRandom",                       false, null);
            addEngine("Signature",                          true,  null);
            addEngine("CertificateFactory",                 false, null);
            addEngine("CertPathBuilder",                    false, null);
            addEngine("CertPathValidator",                  false, null);
            addEngine("CertStore",                          false, "java.security.cert.CertStoreParameters");
            // JCE
            addEngine("Cipher",                             true,  null);
            addEngine("ExemptionMechanism",                 false, null);
            addEngine("Mac",                                true,  null);
            addEngine("KeyAgreement",                       true,  null);
            addEngine("KeyGenerator",                       false, null);
            addEngine("SecretKeyFactory",                   false, null);
            // JSSE
            addEngine("KeyManagerFactory",                  false, null);
            addEngine("SSLContext",                         false, null);
            addEngine("TrustManagerFactory",                false, null);
            // JGSS
            addEngine("GssApiMechanism",                    false, null);
            
            // 省略代码....
        }

        /**
         * Get the service describing this Provider's implementation of the specified type of this algorithm or alias. If no such
         * implementation exists, this method returns null. If there are two matching services, one added to this provider using
         * {@link #putService putService()} and one added via {@link #put put()}, the service added via {@link #putService putService()} is returned.
         *
         * @param type      the type of {@link Service service} requested (for example, {@code MessageDigest})
         * @param algorithm the case insensitive algorithm name (or alternate alias) of the service requested (for example, {@code SHA-1})
         * @return the service describing this Provider's matching service or null if no such service exists
         * @throws NullPointerException if type or algorithm is null
         */
        public synchronized Service getService(String type, String algorithm) {
            checkInitialized();
            // avoid allocating a new key object if possible
            ServiceKey key = previousKey;
            // previousKey 初始的 type 和 algorithm 都为空字符串，因此第一次调用 getService 时，key.matches 肯定为 false，
            // 接下来就需要创建一个新的 ServiceKey 对象
            if (key.matches(type, algorithm) == false) {
                key = new ServiceKey(type, algorithm, false);
                previousKey = key;
            }
            // 一般情况下，Provider 都是添加字符串键值对（保存到了 legacyStrings 中），serviceMap 为 null
            if (serviceMap != null) {
                Service service = serviceMap.get(key);
                if (service != null) {
                    return service;
                }
            }
            // 现在需要将 legacyStrings 中的键值对解析成 ServiceKey 和 Service，然后保存到 legacyMap 中
            ensureLegacyParsed();
            return (legacyMap != null) ? legacyMap.get(key) : null;
        }

        private static void addEngine(String name, boolean sp, String paramName) {
            EngineDescription ed = new EngineDescription(name, sp, paramName);
            // also index by canonical name to avoid toLowerCase() for some lookups
            knownEngines.put(name.toLowerCase(ENGLISH), ed);
            knownEngines.put(name, ed);
        }

        /**
         * Ensure all the legacy String properties are fully parsed into service objects.
         */
        private void ensureLegacyParsed() {
            if ((legacyChanged == false) || (legacyStrings == null)) {
                return;
            }
            serviceSet = null;
            if (legacyMap == null) {
                legacyMap = new LinkedHashMap<ServiceKey, Service>();
            } else {
                legacyMap.clear();
            }
            for (Map.Entry<String, String> entry : legacyStrings.entrySet()) {
                parseLegacyPut(entry.getKey(), entry.getValue());
            }
            removeInvalidServices(legacyMap);
            legacyChanged = false;
        }

        private void parseLegacyPut(String name, String value) {
            // SunJCE.this.put("KeyGenerator.AES", "com.sun.crypto.provider.AESKeyGenerator");
            // SunJCE.this.put("Alg.Alias.KeyGenerator.Rijndael", "AES");
            // 上面两行表示 KeyGenerator 这个 Engine 类中 AES 算法的具体实现类为 com.sun.crypto.provider.AESKeyGenerator
            // 同时，同时这个 AES 算法还有一个别名，就是 Rijndael。如果用户调用 KeyGenerator.getInstance(name)，其中的
            // name 如果是算法原始名称 AES 或者别名 Rijndael，都会返回 com.sun.crypto.provider.AESKeyGenerator
            
            // name 表示 Engine.Alg，比如Alg.Alias.KeyGenerator.Rijndael，如果这里 name 以 "alg.alias" 字符串开头
            if (name.toLowerCase(ENGLISH).startsWith(ALIAS_PREFIX_LOWER)) {
                // e.g. SunJCE.this.put("Alg.Alias.KeyGenerator.Rijndael", "AES");
                // stdAlg 为 AES
                String stdAlg = value;
                // aliasKey ~ KeyGenerator.Rijndael
                String aliasKey = name.substring(ALIAS_LENGTH);
                String[] typeAndAlg = getTypeAndAlgorithm(aliasKey);
                if (typeAndAlg == null) {
                    return;
                }
                
                // type 为 KeyGenerator
                String type = getEngineName(typeAndAlg[0]);
                // aliasAlg 为 Rijndael
                String aliasAlg = typeAndAlg[1].intern();

                // 使用原始的算法名称（比如 AES）生成一个 ServiceKey，然后从 legacyMap 中获取到 Service
                //（如果没有，则创建一个 Service，并且将其保存到 legacyMap 中）
                ServiceKey key = new ServiceKey(type, stdAlg, true);
                Service s = legacyMap.get(key);
                if (s == null) {
                    s = new Service(this);
                    s.type = type;
                    s.algorithm = stdAlg;
                    legacyMap.put(key, s);
                }

                // 使用算法的别名也创建一个 ServiceKey，同时把它和 Service 的映射保存到 legacyMap 中
                legacyMap.put(new ServiceKey(type, aliasAlg, true), s);
                s.addAlias(aliasAlg);
            } else {
                // 从 name 中获取到 Engine 的 type 和使用的算法 alg
                String[] typeAndAlg = getTypeAndAlgorithm(name);
                if (typeAndAlg == null) {
                    return;
                }

                // 在 name 中可以带有属性，属性和 Engine.Alg 之间以空格相隔
                // 比如 Signature.SHA1withDSA SupportedKeyClasses -> java.security.interfaces.DSAPublicKey|java.security.interfaces.DSAPrivateKey
                int i = typeAndAlg[1].indexOf(' ');
                if (i == -1) {
                    // e.g. put("MessageDigest.SHA-1", "sun.security.provider.SHA");
                    // 将 MessageDigest.SHA-1 拆分成 MessageDigest 和 SHA-1，封装到 ServiceKey 和 Service 中，然后把 sun.security.provider.SHA 保存到 Service 中
                    // 最后将 ServiceKey 和 Service 保存到 legacyMap 中
                    String type = getEngineName(typeAndAlg[0]);
                    String stdAlg = typeAndAlg[1].intern();
                    String className = value;
                    ServiceKey key = new ServiceKey(type, stdAlg, true);
                    Service s = legacyMap.get(key);
                    if (s == null) {
                        s = new Service(this);
                        s.type = type;
                        s.algorithm = stdAlg;
                        legacyMap.put(key, s);
                    }
                    s.className = className;
                } else { // attribute
                    // e.g. put("MessageDigest.SHA-1 ImplementedIn", "Software");
                    String attributeValue = value;
                    // type 为 MessageDigest
                    String type = getEngineName(typeAndAlg[0]);
                    // attributeString 为 SHA-1 ImplementedIn
                    String attributeString = typeAndAlg[1];

                    // stdAlg 为 SHA-1
                    String stdAlg = attributeString.substring(0, i).intern();
                    // attributeName 为 ImplementedIn
                    String attributeName = attributeString.substring(i + 1);
                    // kill additional spaces
                    while (attributeName.startsWith(" ")) {
                        attributeName = attributeName.substring(1);
                    }
                    attributeName = attributeName.intern();

                    // 同样根据 type 和 alg 生成 ServiceKey 和 Service，并保存到 legacyMap 中，并且将 attribute 保存到 Service 中
                    ServiceKey key = new ServiceKey(type, stdAlg, true);
                    Service s = legacyMap.get(key);
                    if (s == null) {
                        s = new Service(this);
                        s.type = type;
                        s.algorithm = stdAlg;
                        legacyMap.put(key, s);
                    }
                    s.addAttribute(attributeName, attributeValue);
                }
            }
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            check("putProviderProperty." + name);
            if (debug != null) {
                debug.println("Set " + name + " provider property [" + key + "/" + value + "]");
            }
            return implPut(key, value);
        }

        private Object implPut(Object key, Object value) {
            if ((key instanceof String) && (value instanceof String)) {
                if (!checkLegacy(key)) {
                    return null;
                }
                legacyStrings.put((String)key, (String)value);
            }
            return super.put(key, value);
        }

        public static class Service {

            private String type, algorithm, className;
            private final Provider provider;
            private List<String> aliases;
            private Map<UString,String> attributes;

            /**
             * Return a new instance of the implementation described by this
             * service. The security provider framework uses this method to
             * construct implementations. Applications will typically not need
             * to call it.
             *
             *
             * @param constructorParameter the value to pass to the constructor, or null if this type of service does not use a
             *                             constructorParameter.
             *
             * @return a new implementation of this service
             *
             * @throws InvalidParameterException if the value of constructorParameter is invalid for this type of service.
             * @throws NoSuchAlgorithmException  if instantiation failed for any other reason.
             */
            public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
                if (registered == false) {
                    if (provider.getService(type, algorithm) != this) {
                        throw new NoSuchAlgorithmException("Service not registered with Provider " + provider.getName() + ": " + this);
                    }
                    registered = true;
                }
                try {
                    EngineDescription cap = knownEngines.get(type);
                    if (cap == null) {
                        // unknown engine type, use generic code
                        // this is the code path future for non-core
                        // optional packages
                        return newInstanceGeneric(constructorParameter);
                    }

                    // 如果传入 EngineDescription 的构造函数参数类名为 null，说明算法实现类为无参构造函数，通过反射调用无参构造函数来初始化实例
                    if (cap.constructorParameterClassName == null) {
                        if (constructorParameter != null) {
                            throw new InvalidParameterException("constructorParameter not used with " + type + " engines");
                        }
                        // 通过 Service 类中 string 类型的 className，返回对应的 Class<?> 对象
                        Class<?> clazz = getImplClass();
                        Class<?>[] empty = {};
                        // 通过反射获取 clazz 的实例
                        Constructor<?> con = clazz.getConstructor(empty);
                        return con.newInstance();
                    // 如果传入 EngineDescription 的构造函数参数类名不为 null，那么通过反射调用对应参数的构造函数来初始化实例 
                    } else {
                        Class<?> paramClass = cap.getConstructorParameterClass();
                        if (constructorParameter != null) {
                            Class<?> argClass = constructorParameter.getClass();
                            if (paramClass.isAssignableFrom(argClass) == false) {
                                throw new InvalidParameterException("constructorParameter must be instanceof " + cap.constructorParameterClassName.replace('$', '.') + " for engine type " + type);
                            }
                        }
                        Class<?> clazz = getImplClass();
                        Constructor<?> cons = clazz.getConstructor(paramClass);
                        return cons.newInstance(constructorParameter);
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw e;
                } catch (InvocationTargetException e) {
                    throw new NoSuchAlgorithmException("Error constructing implementation (algorithm: ");
                } catch (Exception e) {
                    throw new NoSuchAlgorithmException("Error constructing implementation (algorithm: ");
                }
            }

        }

        // describe relevant properties of a type of engine
        private static class EngineDescription {
            final String name;
            final boolean supportsParameter;
            final String constructorParameterClassName;
            private volatile Class<?> constructorParameterClass;

            EngineDescription(String name, boolean sp, String paramName) {
                this.name = name;
                this.supportsParameter = sp;
                this.constructorParameterClassName = paramName;
            }

            Class<?> getConstructorParameterClass() throws ClassNotFoundException {
                Class<?> clazz = constructorParameterClass;
                if (clazz == null) {
                    clazz = Class.forName(constructorParameterClassName);
                    constructorParameterClass = clazz;
                }
                return clazz;
            }
        }
    }

    public static class Providers {

        // current system-wide provider list
        // Note volatile immutable object, so no synchronization needed.
        // providerList 是 static 静态类型的，所有的 providers 对象中只包含一份
        private static volatile ProviderList providerList;

        static {
            // set providerList to empty list first in case initialization somehow
            // triggers a getInstance() call (although that should not happen)
            // 当 Providers 中的静态属性或者静态方法（构造方法也是静态方法）第一次被调用时，Java 解释器就会查找并定位 Providers.class 文件，随后载入
            // 此文件，这时有关静态初始化的所有动作都会被执行。因此触发执行 Providers 的 static 块来对 providerList 进行初始化。
            // 具体就是通过 fromSecurityProperties 方法，调用 ProviderList 的无参构造方法，从 java.security 中读取到所有的 provider 属性，并且依次
            // 将其封装到 ProviderConfig 对象中，并保存到 ProviderList 类的 configs 属性
            providerList = ProviderList.EMPTY;
            providerList = ProviderList.fromSecurityProperties();
        }

        private Providers() {
            // empty
        }

        /**
         * Return the current ProviderList. If the thread-local list is set, it is returned. Otherwise, the system wide list is returned.
         */
        public static ProviderList getProviderList() {
            ProviderList list = getThreadProviderList();
            if (list == null) {
                list = getSystemProviderList();
            }
            return list;
        }

        public static ProviderList getThreadProviderList() {
            // avoid accessing the threadlocal if none are currently in use
            // (first use of ThreadLocal.get() for a Thread allocates a Map)
            if (threadListsUsed == 0) {
                return null;
            }
            return threadLists.get();
        }

        private static ProviderList getSystemProviderList() {
            return providerList;
        }

    }

    public final class SunJCE extends Provider {
        public SunJCE() {
            super("SunJCE", 1.8D, "SunJCE Provider (implements RSA, DES, Triple DES, AES, Blowfish, ARCFOUR, RC2, PBE, Diffie-Hellman, HMAC)");
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    // "Cipher.RSA" -> "com.sun.crypto.provider.RSACipher"，这一键值对字符串会被保存到 legacyStrings，
                    // 在后期，legacyStrings （Map<String, String> 类型）中的字符串键值对会分别转变为 ServiceKey 和 Service 保存到
                    // legacyMap 中
                    SunJCE.this.put("Cipher.RSA", "com.sun.crypto.provider.RSACipher");
                    SunJCE.this.put("Cipher.RSA SupportedModes", "ECB");
                    SunJCE.this.put("Cipher.RSA SupportedPaddings", "NOPADDING|PKCS1PADDING|OAEPPADDING|OAEPWITHMD5ANDMGF1PADDING|OAEPWITHSHA1ANDMGF1PADDING|OAEPWITHSHA-1ANDMGF1PADDING|OAEPWITHSHA-224ANDMGF1PADDING|OAEPWITHSHA-256ANDMGF1PADDING|OAEPWITHSHA-384ANDMGF1PADDING|OAEPWITHSHA-512ANDMGF1PADDING");
                    SunJCE.this.put("Cipher.RSA SupportedKeyClasses", "java.security.interfaces.RSAPublicKey|java.security.interfaces.RSAPrivateKey");
                    SunJCE.this.put("Cipher.DES", "com.sun.crypto.provider.DESCipher");
                    SunJCE.this.put("Cipher.DES SupportedModes", "ECB|CBC|PCBC|CTR|CTS|CFB|OFB|CFB8|CFB16|CFB24|CFB32|CFB40|CFB48|CFB56|CFB64|OFB8|OFB16|OFB24|OFB32|OFB40|OFB48|OFB56|OFB64");
                    SunJCE.this.put("Cipher.DES SupportedPaddings", "NOPADDING|PKCS5PADDING|ISO10126PADDING");
                    SunJCE.this.put("Cipher.DES SupportedKeyFormats", "RAW");
                    SunJCE.this.put("Cipher.DESede", "com.sun.crypto.provider.DESedeCipher");
                    SunJCE.this.put("Alg.Alias.Cipher.TripleDES", "DESede");
                    // 省略代码.....
       
                    return null;
                }
            });
            if (instance == null) {
                instance = this;
            }
    
        }
    }

}