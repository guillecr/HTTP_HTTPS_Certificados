package com.guillermo.elmanitas.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.web.multipart.MultipartFile;


/**
 * Clase para realizar peticiones HTTP y HTTPS y el uso de cartera de certificados de confianza personalizado
 * @author Guillermo Casas Reche
 * @author g.casas.r94@gmail.com
 * @version 0.1
 */
public class PeticionesHTTP {

    /**
     * Metodo para realizar peticiones HTTP o HTTPS y obtener su respuesta.
     * Si se emplea protocolo HTTPS, requiere que el servicio posea un certificado validado verificado por una CA reconocida.
     * @param urlParaVisitar URL de petición
     * @return Texto plano de la respuesta
     * @throws Exception Si se produce algún error durante la petición
     */
    public static String peticionHttpGet(String urlParaVisitar) throws Exception {
        // Esto es lo que vamos a devolver
        StringBuilder resultado = new StringBuilder();
        // Crear un objeto de tipo URL
        URL url = new URL(urlParaVisitar);

        // Abrir la conexión e indicar que será de tipo GET
        HttpsURLConnection conexion = (HttpsURLConnection) url.openConnection();
        conexion.setRequestMethod("GET");

        // Búferes para leer
        BufferedReader rd = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        String linea;
        // Mientras el BufferedReader se pueda leer, agregar contenido a resultado
        while ((linea = rd.readLine()) != null) {
            resultado.append(linea);
        }
        // Cerrar el BufferedReader
        rd.close();
        // Regresar resultado, pero como cadena, no como StringBuilder
        return resultado.toString();
    }

    /**
     * Metodo para enviar por protocolo HTTP o HTTPS ficheros y recuperar el codigo de respuesta del servidor.
     * El fichero se enviará bajo el nombre de parametro "file"
     * Si se emplea protocolo HTTPS, requiere que el servicio posea un certificado validado verificado por una CA reconocida.
     * @param url URL de servicio
     * @param multiPartfile Fichero multiparte al que enviar
     * @param name Nombre con el que enviar el fichero
     * 
     * @return OK: Todo bien, sinó devuelve una excepión
     * @throws Exception Si se produce algún error durante la petición
     */
    public static String peticionHttpPost(String url, MultipartFile multiPartfile, String name) throws Exception {
        String charset = "UTF-8";
        String param = name;
        
        File file = File.createTempFile( multiPartfile.getOriginalFilename(), "");
        multiPartfile.transferTo(file);
        
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (
            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
        ) {
            // Send normal param.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"idImg\"").append(CRLF); // Nombre del parametro
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF).append(param).append(CRLF).flush(); // El valor del parametro
        
            // Send binary file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\""+ name +"\"").append(CRLF); // Nombre del parametro
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(multiPartfile.getOriginalFilename())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();

            Files.copy(file.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
        
            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();
        }
        
        // Request is lazily fired whenever you need to obtain information about response.
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        System.out.println(responseCode); // Should be 200
        return "OK";
    }

    /**
     * Metodo indicado para realizar peticiones HTTPS a servidores sin un certificado reconocido por una CA valida.
     * En ella hay que indicar los certificados en los que se confiarán durante la petición
     * @param urlParaVisitar URL de petición. HTTPS://...
     * @param Certificados Cartera de certificados de confianza
     * @param HOST Indicar el nombre del servicio para su verificación
     * @return Texto plano de la respuesta
     * @throws Exception Si se produce algún error durante la petición
     */
    public static String peticionHttpsGet(String urlParaVisitar,TrustManagerFactory Certificados, String HOST) throws Exception{
        StringBuilder resultado = new StringBuilder();
        URL url = new URL(urlParaVisitar);
        SSLContext context2 = SSLContext.getInstance("TLS");
        context2.init(null, Certificados.getTrustManagers(), null);

        // Deshabilitar la verificación del Host. Esto no es seguro, no es producción
        //HttpsURLConnection.setDefaultHostnameVerifier( new HostnameVerifier(){ public boolean verify(String string,SSLSession ssls) { return true; } });

        // Verificamos que es nuestra IP (HOST)
        HostnameVerifier hostnameVerifierOtro = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return hostname.equals(HOST);
            }
        };

        HttpsURLConnection conexion = (HttpsURLConnection) url.openConnection();
        conexion.setSSLSocketFactory(context2.getSocketFactory());
        conexion.setHostnameVerifier(hostnameVerifierOtro);
        conexion.setRequestMethod("GET");

        // Búferes para leer
        BufferedReader rd = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        String linea;
        // Mientras el BufferedReader se pueda leer, agregar contenido a resultado
        while ((linea = rd.readLine()) != null) {
            resultado.append(linea);
        }
        // Cerrar el BufferedReader
        rd.close();
        // Regresar resultado, pero como cadena, no como StringBuilder
        return resultado.toString();
    }
}