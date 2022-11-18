public class CipherAnalysis {

    public static final Cipher getInstance(String transformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
        List transforms = getTransforms(transformation);
        List cipherServices = new ArrayList(transforms.size());
        
        for (Iterator t = transforms.iterator(); t.hasNext(); ) {
            Transform transform = (Transform)t.next();
            cipherServices.add(new ServiceId("Cipher", transform.transform));
        }

        List services = GetInstance.getServices(cipherServices);
        // make sure there is at least one service from a signed provider
        // and that it can use the specified mode and padding
        Iterator t = services.iterator();
        Exception failure = null;
        while (t.hasNext()) {
            Service s = (Service)t.next();
            if (JceSecurity.canUseProvider(s.getProvider()) == false) {
                continue;
            }
            Transform tr = getTransform(s, transforms);
            if (tr == null) {
                // should never happen
                continue;
            }
            int canuse = tr.supportsModePadding(s);
            if (canuse == S_NO) {
                // does not support mode or padding we need, ignore
                continue;
            }
            if (canuse == S_YES) {
                return new Cipher(null, s, t, transformation, transforms);
            } else { // S_MAYBE, try out if it works
                try {
                    CipherSpi spi = (CipherSpi)s.newInstance(null);
                    tr.setModePadding(spi);
                    return new Cipher(spi, s, t, transformation, transforms);
                } catch (Exception e) {
                    failure = e;
                }
            }
        }
        throw new NoSuchAlgorithmException("Cannot find any provider supporting " + transformation, failure);
    }

    private static List getTransforms(String transformation) throws NoSuchAlgorithmException {
        String[] parts = tokenizeTransformation(transformation);

        String alg = parts[0];
        String mode = parts[1];
        String pad = parts[2];
        if ((mode != null) && (mode.length() == 0)) {
            mode = null;
        }
        if ((pad != null) && (pad.length() == 0)) {
            pad = null;
        }

        if ((mode == null) && (pad == null)) {
            // DES
            Transform tr = new Transform(alg, "", null, null);
            return Collections.singletonList(tr);
        } else { // if ((mode != null) && (pad != null)) {
            // DES/CBC/PKCS5Padding
            List list = new ArrayList(4);
            list.add(new Transform(alg, "/" + mode + "/" + pad, null, null));
            list.add(new Transform(alg, "/" + mode, null, pad));
            list.add(new Transform(alg, "//" + pad, mode, null));
            list.add(new Transform(alg, "", mode, pad));
            return list;
        }
    }

    private static String[] tokenizeTransformation(String transformation) throws NoSuchAlgorithmException {
        if (transformation == null) {
            throw new NoSuchAlgorithmException("No transformation given");
        }
        /*
         * array containing the components of a Cipher transformation:
         *
         * index 0: algorithm component (e.g., DES)
         * index 1: feedback component (e.g., CFB)
         * index 2: padding component (e.g., PKCS5Padding)
         */
        String[] parts = new String[3];
        int count = 0;
        StringTokenizer parser = new StringTokenizer(transformation, "/");
        try {
            while (parser.hasMoreTokens() && count < 3) {
                parts[count++] = parser.nextToken().trim();
            }
            if (count == 0 || count == 2 || parser.hasMoreTokens()) {
                throw new NoSuchAlgorithmException("Invalid transformation" + " format:" + transformation);
            }
        } catch (NoSuchElementException e) {
            throw new NoSuchAlgorithmException("Invalid transformation " + "format:" + transformation);
        }
        if ((parts[0] == null) || (parts[0].length() == 0)) {
            throw new NoSuchAlgorithmException("Invalid transformation:" + "algorithm not specified-"  + transformation);
        }
        return parts;
    }

    private static class Transform {

        // transform string to lookup in the provider
        final String transform;

        // the mode/padding suffix in upper case. for example, if the algorithm
        // to lookup is "DES/CBC/PKCS5Padding" suffix is "/CBC/PKCS5PADDING"
        // if loopup is "DES", suffix is the empty string
        // needed because aliases prevent straight transform.equals()
        final String suffix;

        // value to pass to setMode() or null if no such call required
        final String mode;

        // value to pass to setPadding() or null if no such call required
        final String pad;

        Transform(String alg, String suffix, String mode, String pad) {
            this.transform = alg + suffix;
            this.suffix = suffix.toUpperCase(Locale.ENGLISH);
            this.mode = mode;
            this.pad = pad;
        }

        // set mode and padding for the given SPI
        void setModePadding(CipherSpi spi) throws NoSuchAlgorithmException,
                NoSuchPaddingException {
            if (mode != null) {
                spi.engineSetMode(mode);
            }
            if (pad != null) {
                spi.engineSetPadding(pad);
            }
        }
        // check whether the given services supports the mode and
        // padding described by this Transform
        int supportsModePadding(Service s) {
            int smode = supportsMode(s);
            if (smode == S_NO) {
                return smode;
            }
            int spad = supportsPadding(s);
            // our constants are defined so that Math.min() is a tri-valued AND
            return Math.min(smode, spad);
        }

        // separate methods for mode and padding
        // called directly by Cipher only to throw the correct exception
        int supportsMode(Service s) {
            return supports(s, ATTR_MODE, mode);
        }
        int supportsPadding(Service s) {
            return supports(s, ATTR_PAD, pad);
        }

        private static int supports(Service s, String attrName, String value) {
            if (value == null) {
                return S_YES;
            }
            String regexp = s.getAttribute(attrName);
            if (regexp == null) {
                return S_MAYBE;
            }
            return matches(regexp, value) ? S_YES : S_NO;
        }

        // ConcurrentMap<String,Pattern> for previously compiled patterns
        private final static ConcurrentMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();

        private static boolean matches(String regexp, String str) {
            Pattern pattern = (Pattern)patternCache.get(regexp);
            if (pattern == null) {
                pattern = Pattern.compile(regexp);
                patternCache.putIfAbsent(regexp, pattern);
            }
            return pattern.matcher(str.toUpperCase(Locale.ENGLISH)).matches();
        }

    }

}