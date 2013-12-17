package br.com.jcomputacao.cartaCorrecaoEmissor;

import br.com.jcomputacao.cartaCorrecao.assinatura.Assinador;
import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUFUtil;
import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUtil;
import br.com.jcomputacao.cartaCorrecao.util.XmlUtil;
import br.com.jcomputacao.cartaCorrecaoIntegracao.ServicoEventoCartaCorrecao;
import br.com.jcomputacao.cartaCorrecaoIntegracao.WsConnectionConfig;
import br.com.jcomputacao.exception.DbfDatabaseException;
import br.com.jcomputacao.exception.DbfException;
import br.com.jcomputacao.model.CartaCorrecaoModel;
import br.com.jcomputacao.model.LojaModel;
import br.com.jcomputacao.model.NFeStatus;
import br.com.jcomputacao.model.beans.LojaBean;
import br.com.jcomputacao.util.Ambiente;
import br.com.jcomputacao.util.StringUtil;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TEvento;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TEvento.InfEvento.DetEvento;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TProcEvento;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TRetEnvEvento;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TretEvento;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import org.apache.axis2.AxisFault;

/**
 *
 * @author Murilo.Lima
 */
public class IntegracaoCartaCorrecao extends Servico {

    private static final String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private String cnpj;

    public String converterEAssinar(CartaCorrecaoModel carta) throws DbfException {
        return assinar(converter(carta), carta);
    }

    public String converter(CartaCorrecaoModel carta) throws DbfException {
        String xml = "";
        try {
            xml = exportarString(carta);
            xml = xml.replaceAll("ns2:", "");
            xml = xml.replaceAll("xmlns:ns2=\".+#\"\\s", "").replaceAll("ns2:", "");
        } catch (JAXBException ex) {
            throw new DbfException("Erro de XML ao tentar tornar a Cartar de Correção um XML", ex);
        } catch (DbfDatabaseException ex) {
            throw new DbfException("Erro de banco de dados ao tentar tornar a Carta de Correção um XML", ex);
        } catch (ParseException ex) {
            throw new DbfException("Erro ao tentar converter a data do evento", ex);
        }
        List<String> pedacosXml = XmlUtil.getTagConteudo(xml, "evento", true);
        StringBuilder sb = new StringBuilder();
        for (String pedaco : pedacosXml) {
            sb.append(StringUtil.soEspacoSimples(pedaco).replace("\n", ""));
        }

        xml = sb.toString().replace("> <", "><");
        if (xml.contains("<evento>")) {
            xml = xml.replace("<evento>", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        }
        return xml;
    }

    public String assinar(String xml, CartaCorrecaoModel cartaModel) throws DbfException {
        try {
            if (cnpj == null) {
                cnpj = obtemCnpjEmitente(cartaModel);
            }
            Ambiente.debug(xml);
            xml = Assinador.assinar(xml, CartaCorrecaoUtil.getCaminhoCertificado(cnpj), CartaCorrecaoUtil.getSenhaCertificado(cnpj), cnpj);
        } catch (Exception ex) {
            throw new DbfException("Erro ao tentar assinar o XML da NFe", ex);
        }
        return xml;
    }

    public String exportarString(CartaCorrecaoModel cartaModel) throws JAXBException, DbfDatabaseException, DbfException, ParseException {
        return exportarString(exportarXml(cartaModel));
    }

    private String exportarString(TEvento carta) throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(carta, baos);
        return baos.toString();
    }

    private TEvento exportarXml(CartaCorrecaoModel cartaModel) throws DbfDatabaseException, DbfException, ParseException {
        String tipoEvento = "110110";
        String id = "ID" + tipoEvento + cartaModel.getNfeChaveAcesso() + "0" + cartaModel.getSequenciaString();
        TEvento evento = new TEvento();
        TEvento.InfEvento inf = new TEvento.InfEvento();
        LojaModel lojaModel = LojaBean.getLojaPorCodigo(cartaModel.getLoja());
        inf.setCOrgao(getCodigoUf(cartaModel));
        if (StringUtil.isNotNull(cartaModel.getNfeChaveAcesso())) {
            inf.setChNFe(cartaModel.getNfeChaveAcesso());
        } else {
            throw new DbfException("Chave de acesso da NFe, não informada na carta de correção");
        }
        inf.setDhEvento(converteData(cartaModel.getCadastroDbString()));
        inf.setNSeqEvento(cartaModel.getSequenciaString());
        inf.setTpAmb(Integer.toString(CartaCorrecaoUtil.getAmbiente()));
        inf.setTpEvento(tipoEvento);
        inf.setId(id);
        inf.setVerEvento("1.00");
        inf.setCNPJ(Long.toString(lojaModel.getCnpjLong()));
        inf.setDetEvento(criaDetalhes(cartaModel));
        evento.setInfEvento(inf);
        return evento;
    }

    private DetEvento criaDetalhes(CartaCorrecaoModel cartaModel) {
        DetEvento det = new DetEvento();
        det.setDescEvento("Carta de Correcao");
        det.setVersao("1.00");
        det.setXCondUso(getCondicaoUso());
        det.setXCorrecao(cartaModel.getCorrecao());
        return det;
    }

    public int enviar(CartaCorrecaoModel cartaModel) throws DbfException {
        cnpj = obtemCnpjEmitente(cartaModel);
        WsConnectionConfig.setProperties(cnpj);

        String xml = null;
        String chave = null;
        String tipoEvento = "110110";
        Marshaller marshaller = null;
        TEvento eveCart = new TEvento();

        try {
            eveCart.setVersao("1.00");
            TEvento.InfEvento eveInfCart = new TEvento.InfEvento();
            chave = cartaModel.getNfeChaveAcesso();
            String id = "ID" + tipoEvento + chave + "0" + cartaModel.getSequenciaString();
            eveInfCart.setId(id);
            eveInfCart.setChNFe(chave);
            eveInfCart.setCOrgao(getCodigoUf(cartaModel));
            eveInfCart.setCNPJ(StringUtil.somenteNumeros(LojaBean.getLojaAtual().getCpfCnpj()));
            eveInfCart.setNSeqEvento(cartaModel.getSequenciaString());
            eveInfCart.setTpAmb(Integer.toString(CartaCorrecaoUtil.getAmbiente()));
            eveInfCart.setVerEvento("1.00");
            eveInfCart.setTpEvento(tipoEvento);
            eveInfCart.setDhEvento(buscaDataAtual());
            eveInfCart.setDetEvento(criaDetalhes(cartaModel));
            eveCart.setInfEvento(eveInfCart);
            marshaller = context.createMarshaller();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(eveCart, baos);
            xml = baos.toString();
            xml = xml.replaceAll("xmlns:ns2=\".+#\"\\s", "").replaceAll("ns2:", "");

        } catch (JAXBException ex) {
            throw new DbfException("Erro ao montar os dados para a assinatura", ex);
        }

        System.out.print(xml);
        
        try {
            xml = Assinador.assinar(xml, CartaCorrecaoUtil.getCaminhoCertificado(cnpj), CartaCorrecaoUtil.getSenhaCertificado(cnpj), cnpj);
        } catch (Exception ex) {
            throw new DbfException("Erro ao tentar assinar o cancelamento", ex);
        }

        validar(xml);

        String fileName = chave + "-ped-event-carta.xml";
        try {
            escreve(fileName, xml);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar gravar o arquivo " + fileName, ex);
        }

        cartaModel.setCartaXml(xml);
        long timeInMillis = Calendar.getInstance().getTimeInMillis();
        String idLote = Long.toString(timeInMillis);

        logger.log(Level.FINE, "ID Lote : {0}", idLote);
        String lote = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><envEvento xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"1.00\">"
                + "<idLote>" + idLote + "</idLote>"
                + xml.replace(prolog, "")
                + "</envEvento>";
        logger.log(Level.FINE, lote);
        cartaModel.setCartaLote(idLote);

        try {
            fileName = "loteCartaCorrecao" + idLote + ".xml";
            escreve(fileName, lote);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar escrever o arquivo " + lote, ex);
        }

        ServicoEventoCartaCorrecao servico = new ServicoEventoCartaCorrecao();
        fileName = chave + "-carta.xml";
        String resultado = null;
        try {
            resultado = servico.executar(lote);
            logger.log(Level.FINER, "Arquivo escrito {0}", fileName);
        } catch (AxisFault ex) {
            throw new DbfException("Erro ao tentar acessar o servico da SEFAZ", ex);
        } catch (RemoteException ex) {
            throw new DbfException("Erro ao tentar acessar o servico da SEFAZ", ex);
        } catch (XMLStreamException ex) {
            throw new DbfException("Erro de XML ao tentar acessar o servico da SEFAZ", ex);
        }

        try {
            escreve(fileName, resultado);
        } catch (IOException ex) {
            throw new DbfException("Erro ao criar o arquivo " + fileName, ex);
        }
        
        Ambiente.debug("Colocando o numero do lote na carta de correcao " + cartaModel.update());        

        Unmarshaller unmarshaller = null;
        TRetEnvEvento retEnvEvento = null;
        try {
            unmarshaller = context.createUnmarshaller();
            ByteArrayInputStream bais = new ByteArrayInputStream(resultado.getBytes("UTF-8"));
            retEnvEvento = unmarshaller.unmarshal(new StreamSource(bais), TRetEnvEvento.class).getValue();
            bais.close();
        } catch (JAXBException ex) {
            throw new DbfException("Erro ao de XML tentar converter o XML do retorno em objeto", ex);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar converter o XML do retorno em objeto", ex);
        }

        TretEvento retEvento = null;
        if (retEnvEvento.getRetEvento() != null && !retEnvEvento.getRetEvento().isEmpty()) {
            for (TretEvento retEv : retEnvEvento.getRetEvento()) {
                retEvento = retEv;
            }
        }

        xml = colocaProtocoloNoXmlDaCartaCorrecao(cartaModel, retEvento);                
        if (xml.contains("<evento>")) {
            xml = xml.replace("<evento>", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        }
        try {
            fileName = chave + "-procEventoCarta.xml";
            escreve(fileName, xml);
        } catch (IOException ex) {
            throw new DbfException("Erro ao criar o arquivo " + fileName, ex);
        }
        if (retEvento != null && retEvento.getInfEvento() != null && retEvento.getInfEvento().getCStat() != null) {
            if (Integer.parseInt(retEvento.getInfEvento().getCStat()) == 135) {
                cartaModel.setCartaXml(xml);
                cartaModel.setNumeroProtocolo(retEvento.getInfEvento().getNProt());    
                cartaModel.setEventoDataHora(retEvento.getInfEvento().getDhRegEvento().toString());
                cartaModel.setCartaStatus(NFeStatus.AUTORIZADA);
                cartaModel.setProtocoloStatus(retEvento.getInfEvento().getCStat());                
                cartaModel.update();
            } else {
                cartaModel.setProtocoloStatus(retEvento.getInfEvento().getCStat());
                cartaModel.update();
                String msg = retEvento.getInfEvento().getCStat() + " "
                        + retEvento.getInfEvento().getXMotivo();
                throw new DbfException(msg);
            }
        } else if(retEnvEvento != null) {
            String msg = retEnvEvento.getCStat() + retEnvEvento.getXMotivo();
            throw new DbfException(msg);
        } else {
            throw new DbfException("Resultado desconhecido vide arquivo " + fileName);
        }
        int status = 0;
        if(retEvento.getInfEvento().getCStat() != null) {
            status = Integer.parseInt(retEvento.getInfEvento().getCStat());
        }
        return status;
    }

    private void validar(String xml) throws DbfException {
        String urlXsd = System.getProperty("cartaCorrecao.validacao.localizacao.cartaCorrecao");
        validar(xml, urlXsd);
    }

    private String getCodigoUf(CartaCorrecaoModel cartaModel) throws DbfDatabaseException {
        LojaModel lojaModel = LojaBean.getLojaPorCodigo(cartaModel.getLoja());
        CartaCorrecaoUFUtil uf = CartaCorrecaoUFUtil.porSigla(lojaModel.getCidade().getEstado().getSigla());
        return uf.getCodigo();
    }

    private String buscaDataAtual() {
        String dataString = "";
        try {
            java.sql.Statement stmt = Ambiente.getConnectionForSelect().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT NOW()");

            if (rs.next()) {
                java.sql.Timestamp data = rs.getTimestamp(1);
                //PEGANDO A DATA EM MILIGEGUNDOS
                try {
                    dataString = converteData(data);
                } catch (ParseException ex) {
                    Logger.getLogger(IntegracaoCartaCorrecao.class.getName()).log(Level.SEVERE, "Erro ao tentar converter data", ex);
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            new Exception("N\u00e3O CONSEGUIU OBTER A DATA DO SERVIDOR", e);
            System.exit(0);
        }
        return dataString;
    }

    private String converteData(Timestamp data) throws ParseException {
        String dataString = "";
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sszzz");
        //Time in GMT
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT-03:00"));
        dataString = dateFormatGmt.format(data).toString().replaceAll("GMT", "");
        if (dataString.length() > 24) {
            int i;
            String pedacoString1;
            String pedacoString2;
            if (dataString.contains(".")) {
                i = dataString.indexOf(".");
                pedacoString1 = dataString.substring(0, i);
                i = dataString.indexOf("-03:00");
                pedacoString2 = dataString.substring(i, dataString.length());
                dataString = pedacoString1 + pedacoString2;
            }
        }
        return dataString;
    }

    public void trataSucesso(CartaCorrecaoModel model, TProcEvento procEvento) throws DbfException, IOException {
        TretEvento.InfEvento infEvento = procEvento.getRetEvento().getInfEvento();
        model.setCartaStatus(NFeStatus.AUTORIZADA);
        logger.log(Level.FINE, "Status    : {0}", infEvento.getCStat());
        model.setProtocoloStatus(infEvento.getCStat());
        logger.log(Level.FINE, "Data/Hora : {0}", infEvento.getDhRegEvento().toString());
        model.setEventoDataHora((infEvento.getDhRegEvento().toString()));
        String xml = colocaProtocoloNoXmlDaCartaCorrecao(model, procEvento.getRetEvento());
        logger.log(Level.FINE, "N# Prot.  : {0}", infEvento.getNProt());
        model.setNumeroProtocolo(infEvento.getNProt());        
        String fileName = model.getNfeChaveAcesso() + "-procNFe.xml";
        escreve(fileName, xml);
        model.setCartaXml(xml);        
    }

    private String colocaProtocoloNoXmlDaCartaCorrecao(CartaCorrecaoModel cartaModel, TretEvento retEvento) throws DbfException {
        Unmarshaller unmarshaller = null;
        TProcEvento procEventoCarta = new TProcEvento();
        try {
            unmarshaller = context.createUnmarshaller();
            ByteArrayInputStream bais = new ByteArrayInputStream((cartaModel.getCartaXml()).getBytes("UTF-8"));
            TEvento evento = unmarshaller.unmarshal(new StreamSource(bais), TEvento.class).getValue();
            bais.close();
            procEventoCarta.setVersao("1.00");
            procEventoCarta.setEvento(evento);
            procEventoCarta.setRetEvento(retEvento);
        } catch (JAXBException ex) {
            throw new DbfException("Erro ao de XML tentar converter o XML do retorno em objeto", ex);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar converter o XML do retorno em objeto", ex);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Marshaller marshaller = null;
        try {
            marshaller = context.createMarshaller();
            marshaller.marshal(procEventoCarta, baos);
            String xml = baos.toString();
            xml = xml.replaceAll("xmlns:ns2=\".+#\"\\s", "").replaceAll("ns2:", "").replace("<Signature>", "<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">");;
            if (xml.contains("<evento>")) {
                xml = xml.replace("<evento>", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
            }
            if (retEvento != null && retEvento.getInfEvento() != null && retEvento.getInfEvento().getCStat() != null) {
                if (Integer.parseInt(retEvento.getInfEvento().getCStat()) == 135) {
                    List<String> digest = XmlUtil.getTagConteudo(xml, "DigestValue", false);
                    if (digest != null && digest.size() > 0) {
                        cartaModel.setDigestValue(digest.get(0));
                    }
                }
            }

            return xml;
        } catch (JAXBException ex) {
            throw new DbfException("Erro ao tentar montar o resultado do processamento para armazenar", ex);
        }
    }
}
