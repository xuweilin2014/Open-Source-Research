public class ProviderListAnalysis {

    public static final class ProviderList {

        private final static ProviderConfig[] PC0 = new ProviderConfig[0];

        private final static Provider[] P0 = new Provider[0];

        // constant for an ProviderList with no elements
        static final ProviderList EMPTY = new ProviderList(PC0, true);

        // configuration of the providers
        private final ProviderConfig[] configs;

        /**
         * Create a new ProviderList from an array of configs
         */
        private ProviderList(ProviderConfig[] configs, boolean allLoaded) {
            this.configs = configs;
            this.allLoaded = allLoaded;
        }

        // construct a ProviderList from the security properties (static provider configuration in the java.security file)
        static ProviderList fromSecurityProperties() {
            // doPrivileged() because of Security.getProperty()
            return AccessController.doPrivileged(
                        new PrivilegedAction<ProviderList>() {
                            public ProviderList run() {
                                return new ProviderList();
                            }
                        }
                    );
        }

        /**
         * Return a new ProviderList parsed from the java.security Properties.
         */
        private ProviderList() {
            List<ProviderConfig> configList = new ArrayList<>();
            for (int i = 1; true; i++) {
                // 获取 java.security 文件中属性 security.provider.N 的值，比如 sun.security.provider.Sun
                String entry = Security.getProperty("security.provider." + i);
                if (entry == null) {
                    break;
                }
                entry = entry.trim();
                if (entry.length() == 0) {
                    System.err.println("invalid entry for " + "security.provider." + i);
                    break;
                }

                // 如果 provider 类的构造函数包括了其它的参数，那么类名和参数之间使用空白符进行分隔
                int k = entry.indexOf(' ');
                ProviderConfig config;
                // k = -1 说明没有参数，provider 使用无参的默认构造函数
                if (k == -1) {
                    // 将属性 security.provider.N 的值封装到 config 中
                    config = new ProviderConfig(entry);
                // 否则需要获取到参数和类名，并且封装到 ProviderConfig 类中，ProviderConfig 表示 Provider 的配置信息    
                } else {
                    String className = entry.substring(0, k);
                    String argument = entry.substring(k + 1).trim();
                    config = new ProviderConfig(className, argument);
                }

                // Get rid of duplicate providers.
                // 然后将 config 添加到 configList 中
                if (configList.contains(config) == false) {
                    configList.add(config);
                }
            }
            // 将 List<ProviderConfig> 转化为 ProviderConfig[]，并保存到 configs 属性中
            configs = configList.toArray(PC0);
            if (debug != null) {
                debug.println("provider configuration: " + configList);
            }
        }

        /**
         * Return a Service describing an implementation of the specified algorithm from the Provider with the highest precedence that
         * supports that algorithm. Return null if no Provider supports this algorithm.
         */
        // class: ProviderList
        public Service getService(String type, String name) {
            for (int i = 0; i < configs.length; i++) {
                // 通过反射创建 Provider 类实例
                Provider p = getProvider(i);
                Service s = p.getService(type, name);
                if (s != null) {
                    return s;
                }
            }
            return null;
        }

        Provider getProvider(int index) {
            Provider p = configs[index].getProvider();
            return (p != null) ? p : EMPTY_PROVIDER;
        }
    }

    final class ProviderConfig {

        // name of the provider class
        // Provider 对应的类名称
        private final String className;

        // argument to the provider constructor, empty string indicates no-arg constructor
        // Provider 构造函数的参数，如果没有参数，则 argument 为空字符串
        private final String argument;

        private volatile Provider provider;

        ProviderConfig(String className, String argument) {
            if (className.equals(P11_SOL_NAME) && argument.equals(P11_SOL_ARG)) {
                checkSunPKCS11Solaris();
            }
            this.className = className;
            this.argument = expand(argument);
        }
    
        ProviderConfig(String className) {
            this(className, "");
        }

        /**
         * Get the provider object. Loads the provider if it is not already loaded.
         * 如果 ProviderConfig 的 provider 属性不为 null，那么说明 provider 已经生成直接返回；否则通过反射生成 provider 实例并返回
         */
        synchronized Provider getProvider() {
            // volatile variable load
            Provider p = provider;
        
            if (p != null) {
                return p;
            }
            // 省略代码.....
            try {
                isLoading = true;
                tries++;
                p = doLoadProvider();
            } finally {
                isLoading = false;
            }
            provider = p;
            return p;
        }

        private Provider doLoadProvider() {
            return AccessController.doPrivileged(new PrivilegedAction<Provider>() {
                public Provider run() {
                    if (debug != null) {
                        debug.println("Loading provider: " + ProviderConfig.this);
                    }
                    try {
                        // 所有的 Provider 类都是通过系统类加载器来进行加载
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        Class<?> provClass;
                        if (cl != null) {
                            provClass = cl.loadClass(className);
                        } else {
                            provClass = Class.forName(className);
                        }
                        Object obj;
                        // 通过反射调用 newInstance 方法来获取到 Provider 实例对象（考虑构造函数有参数和没有参数两种情况）
                        if (hasArgument() == false) {
                            obj = provClass.newInstance();
                        } else {
                            Constructor<?> cons = provClass.getConstructor(CL_STRING);
                            obj = cons.newInstance(argument);
                        }
                        // 如果反射生成的实例对象是 Provider 类型的话，就直接返回
                        if (obj instanceof Provider) {
                            if (debug != null) {
                                debug.println("Loaded provider " + obj);
                            }
                            return (Provider)obj;
                        } else {
                            if (debug != null) {
                                debug.println(className + " is not a provider");
                            }
                            disableLoad();
                            return null;
                        }
                    } catch (Exception e) {
                        // 省略代码.....
                    } catch (ExceptionInInitializerError err) {
                        // 省略代码.....
                    }
                }
            });
        }

    }

}