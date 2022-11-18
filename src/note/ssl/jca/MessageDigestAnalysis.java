public class MessageDigestAnalysis {

    public static abstract class MessageDigest extends MessageDigestSpi {

        /**
         * Returns a MessageDigest object that implements the specified digest algorithm.
         *
         * This method traverses the list of registered security Providers, starting with the most preferred Provider.
         * A new MessageDigest object encapsulating the MessageDigestSpi implementation from the first Provider that supports 
         * the specified algorithm is returned.
         *
         * Note that the list of registered providers may be retrieved via the {@link Security#getProviders() Security.getProviders()} method.
         *
         * @param algorithm the name of the algorithm requested.
         * @return a Message Digest object that implements the specified algorithm.
         * @exception NoSuchAlgorithmException if no Provider supports a MessageDigestSpi implementation for the specified algorithm.
         */
        public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
            try {
                MessageDigest md;
                // getImpl 方法返回 MessageDigest.algorithm（比如 MessageDigest.MD5）的实现类，以及这个算法实现类的提供者
                // objs = {impl, provider}，一般来说，这个算法实现类 impl 为 MessageDigestSpi 的子类
                Object[] objs = Security.getImpl(algorithm, "MessageDigest", (String) null);
                if (objs[0] instanceof MessageDigest) {
                    md = (MessageDigest) objs[0];
                } else {
                    // 将 impl 传入到 Delegate 中，而 Delegate 继承了 MessageDigest，md 是一个 MessageDigest 类实例
                    // 因此，调用 md#digest() 方法，最终会调用 md 内部 digestSpi（算法的真正实现类）的 engineDigest 方法
                    md = new Delegate((MessageDigestSpi) objs[0], algorithm);
                }
                md.provider = (Provider) objs[1];

                if (!skipDebug && pdebug != null) {
                    pdebug.println("MessageDigest." + algorithm +" algorithm from: " + md.provider.getName());
                }

                return md;

            } catch (NoSuchProviderException e) {
                throw new NoSuchAlgorithmException(algorithm + " not found");
            }
        }

    }

    /*
     * The following class allows providers to extend from MessageDigestSpi rather than from MessageDigest. It represents a MessageDigest 
     * with an encapsulated, provider-supplied SPI object (of type MessageDigestSpi). If the provider implementation is an instance of 
     * MessageDigestSpi, the getInstance() methods above return an instance of this class, with the SPI object encapsulated.
     */
    static class Delegate extends MessageDigest implements MessageDigestSpi2 {

        // The provider implementation (delegate)
        private MessageDigestSpi digestSpi;

        // constructor
        public Delegate(MessageDigestSpi digestSpi, String algorithm) {
            super(algorithm);
            // 这里的 digestSpi 是真正的算法实现类
            this.digestSpi = digestSpi;
        }

        protected int engineGetDigestLength() {
            return digestSpi.engineGetDigestLength();
        }

        protected void engineUpdate(byte input) {
            digestSpi.engineUpdate(input);
        }

        protected void engineUpdate(byte[] input, int offset, int len) {
            digestSpi.engineUpdate(input, offset, len);
        }

        protected void engineUpdate(ByteBuffer input) {
            digestSpi.engineUpdate(input);
        }

        public void engineUpdate(SecretKey key) throws InvalidKeyException {
            if (digestSpi instanceof MessageDigestSpi2) {
                ((MessageDigestSpi2)digestSpi).engineUpdate(key);
            } else {
                throw new UnsupportedOperationException("Digest does not support update of SecretKey object");
            }
        }
        protected byte[] engineDigest() {
            return digestSpi.engineDigest();
        }

        protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
                return digestSpi.engineDigest(buf, offset, len);
        }

        protected void engineReset() {
            digestSpi.engineReset();
        }
    }

}