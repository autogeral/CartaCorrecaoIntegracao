package br.com.jcomputacao.cartaCorrecaoEmissor;

import br.com.jcomputacao.cartaCorrecao.assinatura.Assinador;
import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUFUtil;
import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUtil;
import br.com.jcomputacao.cartaCorrecao.util.XmlUtil;
import br.com.jcomputacao.exception.DbfDatabaseException;
import br.com.jcomputacao.exception.DbfException;
import br.com.jcomputacao.model.CartaCorrecaoModel;
import br.com.jcomputacao.model.LojaModel;
import br.com.jcomputacao.model.beans.LojaBean;
import br.com.jcomputacao.util.Ambiente;
import br.com.jcomputacao.util.StringUtil;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TEvento;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TEvento.InfEvento.DetEvento;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 *
 * @author Murilo.Lima
 */
public class IntegracaoCartaCorrecao extends Servico{
    
    private static final String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";    
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
        if(xml.contains("<evento>")) {
            xml = xml.replace("<evento>", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        }
        return xml;
    }
    
    public String assinar(String xml, CartaCorrecaoModel cartaModel) throws DbfException {
        try {
            if(cnpj == null) {
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
        CartaCorrecaoUFUtil uf = CartaCorrecaoUFUtil.porSigla(lojaModel.getCidade().getEstado().getSigla());
        inf.setCOrgao(uf.getCodigo());
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
    
}
