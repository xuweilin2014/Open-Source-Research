public class SecurityAnalysis {

    /**
     * java 安全
     * 
     * Java 语言拥有三大特征：平台无关性、网络移动性和安全性，而 Java 安全体系结构对这三大特征提供了强大的支持和保证，Java 安全体系结构总共分为4个部分：
     * 
     * （1）JCA（Java Cryptography Architecture， Java 加密体系结构）：JCA 提供基本的加密框架， 如证书、 数字签名、消息摘要和密钥对产生器。
     * （2）JCE（Java Cryptography Extension， Java 加密扩展包）：JCE 在 JCA 的基础上作了扩展， 提供了各种加密算法、 消息摘要算法和密钥管理等功能。
     *      JCE 的实现主要在javax.crypto包（及其子包） 中，JCE 可以看成是 JCA 的一部分
     * （3）JSSE（Java Secure Sockets Extension， Java 安全套接字扩展包）：JSSE 提供了基于 SSL（Secure Sockets Layer，安全套接字层）的加密功能。 在网络的传输过程中， 
     *     信息会经过多个主机（很有可能其中一台就被窃听），最终传送给接收者，这是不安全的。这种确保网络通信安全的服务就是由JSSE来提供的。
     * （4）JAAS（Java Authentication and Authentication Service， Java 鉴别与安全服务）：JAAS 提供了在 Java 平台上进行用户身份鉴别的功能。
     * 
     * JCA 和 JCE 是 Java 平台提供的用于安全和加密服务的两组 API。它们并不执行任何算法，它们只是连接应用和实际算法实现程序的一组接口。软件开发商可以根据 JCE 接口
     * （又称安全提供者接口，Secure Provider Interface，即 SPI） 将各种算法实现后，打包成一个 Provider（安全提供者），动态地加载到 Java 运行环境中。根据美国出口限制规定，
     * JCA 可出口，但 JCE 对部分国家是限制出口的。因此，要实现一个完整的安全结构，就需要一个或多个第三方厂商提供的 JCE 产品，称为安全提供者。
     * 
     * Prior to JDK 1.4, the JCE was an unbundled product, and as such, the JCA and JCE were regularly referred to as separate, distinct components. As JCE is now
     * bundled in the JDK, the distinction is becoming less apparent. Since the JCE uses the same architecture as the JCA, the JCE should be more properly thought of as
     * a part of the JCA.
     */

    /**
     * JCA 体系结构
     * 
     * 这里先介绍 Java Cryptography Architecture （JCA）体系结构，这个 JCA 结构主要包括 Engine、Security、Provider、Algorithm、Spi 五个，下面依次介绍。
     * 
     * 1.Engine 并不是某一个类，而是一系列类的总称，具体为：SecureRandom、MessageDigest、Signature、Cipher、MAC、KeyFactory、SecretKeyFactory、
     * KeyPairGenerator、KeyGenerator、KeyAgreement、KeyStore、CertStore、CertPathValidator、CertPathBuilder、CertificateFactory、AlgorithmParameters、
     * AlgorithmParameterGenerator，这些 Engine 类提供了加密服务的一系列接口供用户调用，这些 API 接口独立于于具体的算法实现和 provider 提供者。
     * 
     * 以 MessageDigest 为例，此 Engine 类并不实现任何具体的类，它提供多个 getInstance 工厂类方法，通过此方法可以获取到
     * 一个 MessageDigest 实例，此实例实际上为 MessageDigest 内部的 Deletgate 类对象（Delegate 类为 MessageDigest 的子类）。Delegate 类中包含一个 MessageDigestSpi 类
     * 对象（此对象真正实现了消息摘要算法）。然后调用此 MessageDigest 实例的相关方法，会被代理到 Delegate 中的 spi 类（SPI 的子类实现了具体的消息摘要算法）对应方法来真正完成
     * 消息摘要计算。
     * 
     * 2.Security 类是一个 final 类，不可以被继承，并且几乎所有的方法都是 static 静态的（和 Math、System 类比较类似）。之前介绍过 Engine 类的 getInstance 方法，比如进行如下调用：
     * MessageDigest.getInstance("MD5")，它的具体流程就是 Security 遍历注册好的 Provider 列表，对于列表中的每一个 Provider 都查找是否提供 MD5 算法的实现（这个实现一定
     * 继承了 SPI 接口，这里就是 MessageDigestSpi 类接口，否则会抛出异常），如果找到了就此 SPI 类包装到 MessageDigest#Delegate 类中，然后返回此 Delegate 类。不止 
     * MessageDigest 类，所有的 Engine 类都必须通过 Security 类来查找 Provider 中是否存在对应的算法实现类。另外，Security 的 static 静态块在初始化的时候，会从 java.security
     * 文件中读取 key/value 键值对，并且保存到 Security 的 props 属性中。
     * 
     * 3.Provider 类继承了 Properties 类，Provider 类实际上类似于一个 Map，保存了 Engine.Alg -> Class 的映射（比如 MessageDigest.MD5 -> sun.security.provider.MD5）。
     * Security 在遍历 Provider 列表中的每个 Provider 对象时，就会尝试到这个类似于 Map 结构中找 Engine.Alg（比如 MessageDigest.MD5）是否存在，如果存在就把对应的 Class
     * 通过反射初始化返回。Provider 类本身也并没有任何 JCE/JSSE 算法的具体实现，而只是保存了算法到实现类的映射，方便 Security 进行查找。如果一个公司或者组织自己实现了
     * JCE/JSSE 中的部分或者全部算法，然后就可以自己定义一个 Provider 类，将自定义的 Engine.Alg -> Class 映射保存到 Provider 类中（其中 Class 就是自己实现的算法类的全类名）。
     * 最后将 Provider 类的信息写入到 java.security 文件中，也可以调用 Security#addProvider 方法手动将自定义的 Provider 实例添加到 Security 中。
     * 
     * 4.Algorithm 类和 SPI 类放到一起进行讲解，Algorithm 就是 Engine 代表的算法的具体实现，比如 MessageDigest 这个 Engine 类，而 sun.security.provider.MD5 这个 Algorithm
     * 类就是 MessageDigest 代表的消息摘要算法的某一个具体实现，其它的还有 SHA 等。并且所有的 Algorithm 都必须继承 Spi 类，每一个 Engine 类都有对应的 Spi 抽象类，比如 MessageDigest
     * 对应的 MessageDigestSpi，Signature 对应的 SignatureSpi 等等。
     * 
     * 总的来说，JCA 体系结构就是起到应用程序和真正的算法实现类之间的解耦，当用户调用 Engine 类的 getInstance 方法获取某一算法的具体实现类时，Security 就会遍历列表中的所有 Provider，
     * 对于每一个 Provider，由于其内部保存了算法名称到实现类名之间的映射，即 Engine.Alg -> Class，因此可以在 Provider 中查找是否包含用户所求算法，如果包含，则通过反射将 Class 
     * 类初始化，生成一个实例，然后返回给用户。用户不需要知道获取到的是具体哪一个算法类，这样当算法类本身代码发生了变更，甚至 Provider 发生了变化时，都不会影响用户代码，实现了解耦。
     */

    /**
     * Provider 体系结构
     * 
     * java.security.Provider 类是所有提供者的父类。类 Sun，SunJCE，SunJSSE 都实现了 Provider 类，在这些提供者类（后面称为 provider 类）的构造函数中，提供了此 provider 类的类名称，
     * 版本以及相关信息，同时还要把 Engine.Algorithm -> Class 的映射保存起来（这里的映射字符串可以是 String -> String，也可以是 ServiceKey -> Service）。当用户请求某一个特定算法的
     * 具体实现，比如 MD5，JCA 结构就会遍历 provider 列表中的每一个 provider，查看此 provider 是否包含了该算法实现类的信息。
     * 
     * 每一个 provider 类（比如 SunJCE）提供了一个包（或者好几个包）的具体算法实现。在每一个版本的 JDK 中都默认配置好了一个或者多个 Provider 类，并且 provider 类可以通过静态配置也可以
     * 通过手动配置，静态配置就是指手动把 provider 类的信息写入到 java.security 文件中，手动就是指通过程序（比如 Security.addProvider）添加 provider。在 java.security 文件中，provider
     * 类的信息由配置项 security.provider.N = provider classname 决定（N 代表 provider 类的优先级，数字越大优先级越低，1 代表最高优先级）。因此用户用户可以调整各个 provider 对应的 N 值
     * 改变 provider 查找的优先级。查找优先级指的是当用户请求某一个特定算法实现类并且没有指明具体的 provider 类时，就会按照优先级从高到低的顺序来查找 provider，找到某一个 provider 拥有算法
     * 的具体实现时，就不会再往后进行查找。
     * 
     * JCA 的使用很简单，用户通过 md = MessageDigest.getInstance("SHA-256"); 调用，就可以从某一个 provider 中获取到 SHA-256 消息摘要算法的实现。除此之外，用户也可以指明某一个具体的 provider，
     * 比如 md = MessageDigest.getInstance("SHA-256", "ProviderC");，这行代码直接从 ProviderC 中查找 SHA-256 的具体实现。
     * 
     * 在图 2-1 中，有三个 provider 类实现了不同的消息摘要算法，它们的优先从左到右分别为 1-3。图 2-1 中，一个程序要求 SHA-256 算法，并且没有指明 provider 类的名称。JCA 按照 provider 的优先级
     * 依次检查 provider 是否有 SHA-256 的实现，最后 ProviderB 对象被返回。在图 2-2 中，由于程序指定了 provider 类的名称（尽管 ProviderB 也有 SHA-256 算法）。JDK 中的算法实现分布在多个不同的
     * provider 中（Sun，SunJSSE，SunJCE，SunRsaSign），这主要是因为历史遗留的原因，而不是什么功能设计上的考虑。
     * 
     * 在 JCA 体系中，provider 类对象（比如 SunJCE，Sun 等）并不提供具体的实现，而只是保存了算法到实现类的映射。具体的算法实现类都必须实现对应 Engine 的 Spi 类，比如消息摘要的 MD5 算法必须继承
     * MessageDigest 这个 Engine 的 MessageDigestSpi 类，每一个 Engine 类都有对应的 Spi 类，这些 Spi 类就是在 Engine 类的类名后面加上 Spi 字符串，比如 Signature 对应的 Spi 类为 SignatureSpi。
     * 当用户调用 getInstance 方法时，回返回一个 Engine 类实例，在这个实例内部封装了 Engine 对应的 Spi 类。当用户调用 Engine 类实例的 API 接口时，最终会被代理到内部封装的 Spi 类的方法完成用户所
     * 需的功能。举例如下：
     * 
     * import javax.crypto.*;
     * 
     * Cipher c = Cipher.getInstance("AES");
     * c.init(ENCRYPT_MODE, key);
     * 
     * 这里，应用程序想要 javax.crypto.Cipher 中特定的 AES 算法实例，并且不关心由哪一个 provider 提供。应用程序调用 Cipher 类的 getInstance 工厂方法，然后此方法要求 JCA 框架遍历 Provider 列表
     * 去找到 AES 算法的具体实现，比如找到了 com.foo.AESCipher，这个 AESCipher 类继承了 CipherSpi 类。然后 JCA 会创建一个 Cipher 类实例（在这个 Cipher 实例中封装好了一个 AESCipher 类对象），
     * 最后这个 Cipher 实例对象被返回给用户。当应用程序调用 Cipher 实例的 init 方法时，最终调用的是 com.foo.AESCipher 的 engineInit 方法。
     */

    /**
     * 分析 MessageDigest.getInstance("MD5") 源码流程。
     * 
     * 如果是第一次调用，首先会加载 Security 类，并且会从 java.security 文件中读取属性，保存到 Security 内部的 props 属性。这些属性中就包括了 security.provider.Number 属性。
     * 接下来将这些 provider 属性组装成 ProviderList 并保存到 Providers 的 providerList 属性中。然后就是遍历 ProviderList，通过反射创建 Provider 实例（如果之前没有创建好的
     * 话），然后调用 Provider.getService 方法检查当前 Provider 中是否包含对应的算法实现类，如果存在，就返回一个 Service 对象（Service 为 Provider 的内部类）。
     * 
     * 在 Provider 的子类比如 Sun、SunJCE、SunJSSE 类，在这些子类中需要保存 Engine.Alg -> Class 的映射，这种映射有两种保存方式，
     *  
     * 第一种，也就是之前遗留的一种方式（legacy），就是直接保存字符串，比如 "MessageDigest.MD5" -> "sun.security.provider.MD5"，最后这对
     * key -> value 字符串会被保存到 legacyStrings 中。另外必须说明的是，legacyStrings 中的所有 key/value 字符串最后都会被转换成 ServiceKey -> Service
     * 键值对，保存到 legacyMap 中
     * 
     * 第二种，直接调用 Provider#putService 方法保存 Service 对象。比如下面这种：
     * 
     * public final class MyProvider extends Provider {
     *      public MyProvider() {
     *          super("MyProvider", "1.0", "Some info about my provider and which algorithms it supports");
     *              putService(new Provider.Service(this, "Cipher", "MyCipher", "p.MyCipher", null, null));
     *      }
     * }
     *
     * 最后会根据 Service 对象中的属性值生成 ServiceKey，并且把 ServiceKey -> Service 保存到 serviceMap 中。
     * 
     * 因此 Provider.getService(type, algorithm) 的流程也很明确，首先根据 type 和 algorithm 生成 ServiceKey，然后如果 serviceMap 不为 null，就到 serviceMap 中
     * 根据 key 查找对应的 Service；如果 serviceMap 为 null，就到 legacyMap 中根据 key 查找 Service（如果 legacyMap 也为 null，那么就解析 legacyStrings 中的
     * 字符串键值对，转换成 ServiceKey/Service，保存到 legacyMap 中）。最后调用 Service 的 newInstance 方法，通过反射调用构造函数初始化算法实现类（Service 中有
     * 算法实现类的类名 className）。
     */

    public static final class Security {

        static {
            // doPrivileged here because there are multiple things in initialize that might require privs.
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    initialize();
                    return null;
                }
            });
        }

        // 从 java.security 文件中读取属性到 Security 的 props 属性中
        private static void initialize() {
            props = new Properties();
            boolean loadedProps = false;
            boolean overrideAll = false;

            // first load the system properties file to determine the value of security.overridePropertiesFile
            // 获取到 java.home/lib/security/java.security 文件
            File propFile = securityPropFile("java.security");
            if (propFile.exists()) {
                InputStream is = null;
                try {
                    // FileInputStream 表示从上述 java.security 文件中读取数据
                    FileInputStream fis = new FileInputStream(propFile);
                    is = new BufferedInputStream(fis);
                    // 将 java.security 这个文件中的各个属性保存到 props 对象中，其中就包括 security.provider.Number 为属性值的 provider 类型
                    // 比如 security.provider.3 -> sun.security.ec.SunEC
                    props.load(is);
                    loadedProps = true;

                    if (sdebug != null) {
                        sdebug.println("reading security properties file: " + propFile);
                    }
                } catch (IOException e) {
                    // 省略代码....
                } finally {
                    // 省略代码....
                }
            }

            // 用户可以在 security 文件中指定 java.security.properties 属性，来指明自己定义 security properties 属性的文件
            // 当然，前提是 security.overridePropertiesFile 被设置为 true
            if ("true".equalsIgnoreCase(props.getProperty("security.overridePropertiesFile"))) {
                //.....
            }

            // 如果之前从 properties 文件中读取数据失败时，那么调用 initializeStatic，静态添加系统指定的 provider，比如 Sun、SunJCE、SunProvider
            if (!loadedProps) {
                initializeStatic();
                if (sdebug != null) {
                    sdebug.println("unable to load security properties " + "-- using defaults");
                }
            }
        }

        /*
        * Initialize to default values, if <java.home>/lib/java.security is not found.
        */
        private static void initializeStatic() {
            props.put("security.provider.1", "sun.security.provider.Sun");
            props.put("security.provider.2", "sun.security.rsa.SunRsaSign");
            props.put("security.provider.3", "com.sun.net.ssl.internal.ssl.Provider");
            props.put("security.provider.4", "com.sun.crypto.provider.SunJCE");
            props.put("security.provider.5", "sun.security.jgss.SunProvider");
            props.put("security.provider.6", "com.sun.security.sasl.Provider");
        }

        /**
        * Gets a security property value.
        *
        * <p>First, if there is a security manager, its checkPermission method is called with a java.security.SecurityPermission("getProperty."+key)
        * permission to see if it's ok to retrieve the specified security property value..
        */
        public static String getProperty(String key) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new SecurityPermission("getProperty."+ key));
            }
            String name = props.getProperty(key);
            if (name != null)
                name = name.trim(); // could be a class name with trailing ws
            return name;
        }

       /*
        * Returns an array of objects: the first object in the array is an instance of an implementation of the requested algorithm
        * and type, and the second object in the array identifies the provider of that implementation.
        */
        static Object[] getImpl(String algorithm, String type, Provider provider) throws NoSuchAlgorithmException {
            return GetInstance.getInstance(type, getSpiClass(type), algorithm, provider).toArray();
        }

        /**
         * Return the Class object for the given engine type (e.g. "MessageDigest"). Works for Spis in the java.security package only.
         * 这里传入的 type 就是 Engine type，这里到 java.security 包中查找 type + "Spi" 类，然后把它保存到 spiMap 中
         */
        private static Class<?> getSpiClass(String type) {
            Class<?> clazz = spiMap.get(type);
            if (clazz != null) {
                return clazz;
            }
            try {
                clazz = Class.forName("java.security." + type + "Spi");
                spiMap.put(type, clazz);
                return clazz;
            } catch (ClassNotFoundException e) {
                throw new AssertionError("Spi class not found", e);
            }
        }

    }

}