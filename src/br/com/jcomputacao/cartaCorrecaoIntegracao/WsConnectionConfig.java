/*
 * Copyright (C) 2010  J. Computacao LTDA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If it is your itention to get support for a commercial version of this
 * application get in touch with J. Computacao LTDA at http://www.jcomputacao.com.br
 *
 * Este programa Ã© software livre: voce pode redistribui-lo e/ou modifica-lo
 * sob os termos da Licenca Pulica Generica (GNU GPL) como publicado pela
 * Free Software Foundation, na versao 3 da licenca ou alguma versao superior.
 *
 * This programa e distribuido na esperanca que seja util,
 * mas SEM QUALQUER GARANTIA; sem sequer a garantia implicita
 * de COMERCIALIZACAO or APTIDAO PARA UMA FINALIDADE PARTICULAR.
 * Leia a GNU GPL para mais detalhes.
 *
 * Voce deveria receber uma copia da GNU GPL junto com este programa
 * se nao o recebeu leia em <http://www.gnu.org/licenses/>.
 *
 * Se for sua intencao obter suporte para uma versao comercial desta
 * aplicacao entre em contato com J. Computacao LTDA em http://www.jcomputacao.com.br
 *
 */
package br.com.jcomputacao.cartaCorrecaoIntegracao;

import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.axis2.client.Options;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * 31/03/2010 18:14:02
 * @author Murilo
 */
public class WsConnectionConfig {

    public static void setProperties(String cnpj) {
        boolean useProxy = Boolean.parseBoolean(System.getProperty("useProxy", "false"));
        if (useProxy) {
            configuraProxy();
        }

        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        if (CartaCorrecaoUtil.getTipoCertificado(cnpj) != null && !"A1".equals(CartaCorrecaoUtil.getTipoCertificado(cnpj))) {
            configuraA3(cnpj);
        } else {
            configuraA1(cnpj);
        }

        String certpath = CartaCorrecaoUtil.getSefazCaminhoCertificado(cnpj);
        File file = null;

        if(certpath.startsWith("file://")) {
            String urlFile =null;
            try {
                urlFile = new URL(certpath).getFile();
            } catch (MalformedURLException ex) {
                Logger.getLogger(WsConnectionConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
            file = new File(urlFile);
        } else {
            file = new File(certpath);
        }

        if(!file.exists()) {
            try {
                String [] hosts;
//                String [] hosts = new String[] {"www.portalfiscal.inf.br"};
//                InstallCert.installHostCertificate(false, file, hosts);
//                hosts = new String[] {"www.sefazvirtual.fazenda.gov.br"};
//                InstallCert.installHostCertificate(false, file, hosts);
                hosts = new String[] {"homologacao.nfe.fazenda.sp.gov.br"};
                InstallCert.installHostCertificate(false, file, hosts);
//                hosts = new String[] {"www.portalfiscal.inf.br"};
//                InstallCert.installHostCertificate(false, file, hosts);
                hosts = new String[] {"homologacao.nfe.fazenda.sp.gov.br"};
                InstallCert.installHostCertificate(false, file, hosts);
                hosts = new String[] {"nfe.fazenda.sp.gov.br"};
                InstallCert.installHostCertificate(false, file, hosts);
                hosts = new String[] {"nfe.fazenda.sp.gov.br"};
                InstallCert.installHostCertificate(false, file, hosts);
            } catch (Exception ex) {
                Logger.getLogger(WsConnectionConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", certpath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

    }

    private static void configuraA3(String cnpj) {
        /*
         * http://www.guj.com.br/java/119191-nota-fiscal-eletronica---certificado-digital
         * http://www.guj.com.br/java/110442-certificado-a3---nfe
         * ***    arquivo token.cfg     ***
         * name=Safesig
         * library =  C:\WINDOWS\system32\aetpkss1.dll
         *
         * Funciona para SERASA
         * name = SmartCard
         * library = c:/windows/system32/aetpkss1.dll
         *
         *http://www.javac.com.br/jc/posts/list/2516-erro-comunicacao-webservice-certificado-oberthur-technologies.page
         * Funciona para Certsign (Certificado Oberthur-technologies = novo da Geralcar)
         * name = SmartCard
         * library = c:/windows/system32/OcsCryptoki.dll
         *
         *Além disso é preciso colocar novas properties 
         */
        

        
        KeyStore ks;
        try {
            String tokenCfg = System.getProperty("nfe.certificado.token.cfg", "C:\\DBF\\dist\\token.cfg");
            String className = "sun.security.pkcs11.SunPKCS11";
            Class<?> providerClass = Class.forName(className);
            if (providerClass == null) {
                throw new Exception("Nao encontrou a classe " + className + "\nPara conseguir assinar o documento!");
            }
            Constructor<?> constructor = providerClass.getConstructor(String.class);
            Provider p = (Provider) constructor.newInstance(tokenCfg);
            Security.addProvider(p);
            ks = KeyStore.getInstance("PKCS11");
            System.setProperty("javax.net.ssl.keyStore", "NONE");
            System.setProperty("javax.net.ssl.keyStoreProvider", "SunPKCS11-SmartCard");
            String senha = CartaCorrecaoUtil.getSenhaCertificado(cnpj);
            ks.load(null, senha.toCharArray());
            System.setProperty("javax.net.ssl.keyStoreType", "PKCS11");
            
            //precisa adicionar essas propriedades se o certificado A3 for Certsign
            //e seu administrador de Token foi AWP Identity Manager da Oberthur
            //Technologies e estiver Usando Java 8, senão este erro pode ocorrer:
            //InvalidKeyException: RSA key must be at most 0 bits
            //FONTE: http://www.javac.com.br/jc/posts/list/2516-erro-comunicacao-webservice-certificado-oberthur-technologies.page
            boolean certificadoTipoOberthurTechnologiesComJava8 = Boolean.parseBoolean(System.getProperty("certicado.OberthurTechnologies", "false"));
            if (certificadoTipoOberthurTechnologiesComJava8) {
                System.setProperty("https.protocols", "SSLv3,TLSv1");
                System.setProperty("jdk.tls.client.protocols", "TLSv1");
            }
            
            boolean multiplosCertificados = CartaCorrecaoUtil.getMultiploCertificado(cnpj);
            if(multiplosCertificados) {
                multiplosCertificados(ks, senha, cnpj);
            }
        } catch (KeyStoreException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (CertificateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (Exception ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        }

        boolean printProviders = Boolean.parseBoolean(System.getProperty("nfe.wsConnectionConfig.printProviders", "false"));
        if (printProviders) {
            Provider[] providers = Security.getProviders();
            for (Provider provider : providers) {
                provider.list(System.out);
            }
        }
    }
    

    private static void configuraProxy() {
        Options options = new Options();
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        String aux = System.getProperty("http.proxyUser");
        auth.setUsername(aux);
        aux = System.getProperty("http.proxyPassword");
        auth.setPassword(aux);
        // set if realm or domain is known
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
        HttpTransportProperties.ProxyProperties proxyProperties = new HttpTransportProperties.ProxyProperties();
        aux = System.getProperty("http.proxyHost");
        proxyProperties.setProxyName(aux);
        int porta = Integer.parseInt(System.getProperty("http.proxyPort", "3128"));
        proxyProperties.setProxyPort(porta);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.PROXY, proxyProperties);
        options.setProperty(org.apache.axis2.context.MessageContextConstants.HTTP_PROTOCOL_VERSION, org.apache.axis2.transport.http.HTTPConstants.HEADER_PROTOCOL_11);
    }

    private static void configuraA1(String cnpj) {
        //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
//        Security.addProvider(new com.sun.security.sasl.Provider());
        String certpath = CartaCorrecaoUtil.getCaminhoCertificado(cnpj);
        File file = new File(certpath);

        String senha = CartaCorrecaoUtil.getSenhaCertificado(cnpj);
        try {
            if (!file.exists()) {
                file = new File(file.getName());
                certpath = file.getAbsolutePath();

                if (!file.exists()) {
                    if (CartaCorrecaoUtil.getCaminhoCertificado(cnpj).startsWith("http")) {

                        URL url = new URL(CartaCorrecaoUtil.getCaminhoCertificado(cnpj));
                        InputStream is = url.openStream();
                        FileOutputStream fos = new FileOutputStream(file);
                        int obyte = -1;
                        while ((obyte = is.read()) != -1) {
                            fos.write(obyte);
                        }
                        fos.flush();
                        fos.close();
                        is.close();

                    }
                }
            }

            boolean multiplosCertificados = CartaCorrecaoUtil.getMultiploCertificado(cnpj);
            if (multiplosCertificados) {
                InputStream entrada = new FileInputStream(file);
                KeyStore ks = KeyStore.getInstance("PKCS12");
                ks.load(entrada, senha.toCharArray());
                multiplosCertificados(ks, senha, cnpj);
            }
        } catch (KeyStoreException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (CertificateException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Erro ao obter o cerficiado", ex);
        }

        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
        System.setProperty("javax.net.ssl.keyStore", certpath);
        System.setProperty("javax.net.ssl.keyStorePassword", senha);
    }

    private static void multiplosCertificados(KeyStore ks, String senha, String cnpj) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        /**
         * Resolve o problema do 403.7 Forbidden para Certificados A3 e A1 e
         * elimina o uso das cofigurações: -
         * System.setProperty("javax.net.ssl.keyStore", "NONE"); -
         * System.setProperty("javax.net.ssl.keyStoreType", "PKCS11"); -
         * System.setProperty("javax.net.ssl.keyStoreProvider",
         * "SunPKCS11-SmartCard"); -
         * System.setProperty("javax.net.ssl.trustStoreType", "JKS"); -
         * System.setProperty("javax.net.ssl.trustStore",
         * arquivoCacertsGeradoTodosOsEstados);
         */
        String alias = "";
        Enumeration<String> aliasesEnum = ks.aliases();
        while (aliasesEnum.hasMoreElements()) {
            alias = (String) aliasesEnum.nextElement();
            if (ks.isKeyEntry(alias)) {
                break;
            }
        }
        X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, senha.toCharArray());
        SocketFactoryDinamico socketFactoryDinamico = new SocketFactoryDinamico(certificate, privateKey);
        String certpath = CartaCorrecaoUtil.getSefazCaminhoCertificado(cnpj);
        socketFactoryDinamico.setFileCacerts(certpath);

        Protocol protocol = new Protocol("https", socketFactoryDinamico, 443);
        Protocol.registerProtocol("https", protocol);
    }
}

