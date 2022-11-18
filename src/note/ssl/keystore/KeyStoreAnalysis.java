public class KeyStoreAnalysis {

    /**
     * Key Management:
     * 
     * keystore：keystore 有两个概念，第一个是 .keystore 文件，这个文件保存 keytool 工具创建好的密钥和证书；同时在 jdk 中还有一个 KeyStore 类。
     * 一般来说，.keystore 文件保存在 C:/WINDOWS 文件夹下。当然用户可以通过 keytool 工具自己设置 .keystore 文件保存的位置。
     * 
     * alias：keystore 中的每一个密钥 key 都属于一个实体 entity，用户可以为这个 entity 指定一个别名（alias）来获取 entity 所对应的密钥和证书
     * 
     * DN（distinguished name）：证书颁发机构（certificate authorities）给服务器实体颁发证书，用来证明这个服务器实体的公钥确实属于这个服务器
     * 实体。在这个证书中，可以提供一个 distinguished name，表明这个服务器实体的身份。当服务器 A 给客户端 B 发送一份公钥时，这个公钥对应的证书中
     * 就有 DN 字符串。DN 一般符合 X.509 标准，举例如下：
     * 
     * CN = Scott Oaks, OU = JSD, O = Sun Microsystems, L = New York, S = NY, C = US
     * 
     * CN 一般表示服务器实体组织的域名，并且 SSL 使用 DN 中的 CN 来检验和服务器的域名是否相同。
     * 
     * key entries：在 keystore 中保存了两种类型的实体（entry），第一种叫做 key entry，key entry 分为两种：
     * 
     * 1).保存了一个非对称加密的键值对（私钥和公钥），对于公钥，还有和这个公钥对应的证书链（从服务器实体证书到中间证书再到根证书）
     * 2).保存了对称加密的对称密钥
     * 
     * certificate entries：在 keystore 中保存了两种类型的实体（entry），第二种叫做 certificate entry，其中保存了根证书
     * 
     * JKS, JCEKS 以及 PKCS12：KeyStore 是 JCA 体系中的一个 Engine 类，Sun 里面不同的 security provider 提供了三种不同的 keystore 实现，
     * 其中默认的是 JKS，但是 JKS 对于 key entry 只能存储非对称加密的键值对，而不能保存对称密钥。但是 JCEKS 两种类型的 key entry 可以提供，
     * JCEKS 由 SunJCE 这个 provider 提供。并且 JKS 和 JCEKS 使用的私钥都被加密过，但是 JCEKS 的加密算法更强。
     * 
     * java key management api 和 keytool 都可以指定 keystore 的类型，如果想更改 keystore 的类型，可以编辑 $JREHOME/lib/security/java.security
     * 文件中的如下属性：
     * 
     * keystore.type = JCEKS
     * 
     * trusted certificate authorities：$JREHOME/lib/security/cacerts 文件本身是一个 keystore 文件，这个 keystore 保存了 10 个证书实体，其中每一个都是
     * CA 机构的根证书。这个 keystore 文件应该被设置为 read-only，不应该被更改。
     * 
     * ************************************************************* 💫🔔  *************************************************************
     * 
     * Keys/Certificate：
     * 
     * 
     * 
     * 
     * 
     */


}