import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Clase para instanciar TrustManagerFactory de un fichero .cer
 * @author Guillermo Casas Reche
 * @author g.casas.r94@gmail.com
 * @version 0.1
 */
public class Certificado {
    public static TrustManagerFactory certificados;
    public static void setCertificado(){
        try {
            // Leer el certificado X.509
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream is = new FileInputStream(new File("<Path_cert>"));
            InputStream caInput = new BufferedInputStream(is);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
            } finally {
                caInput.close();
            }

            // Crear el almacén de claves (KeyStore) e insertar
            // la clave pública del certificado que acabamos de leer
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            // Cada certificado se identifica con un string (podemos añadir varios)
            keyStore.setCertificateEntry("ca", ca);

            // Crear el TrustManager que confia en las CA's incluidas
            // en el KeyStore creado.
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            Certificado.certificados = tmf;
        } catch (NoSuchAlgorithmException | KeyStoreException
                | CertificateException
                | IOException e)
        {
            e.printStackTrace();
        }
    }
}