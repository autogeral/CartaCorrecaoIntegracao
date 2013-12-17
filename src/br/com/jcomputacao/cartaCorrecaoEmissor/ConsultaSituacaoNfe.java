package br.com.jcomputacao.cartaCorrecaoEmissor;

import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUtil;
import br.com.jcomputacao.cartaCorrecaoIntegracao.ServicoConsultaSituacaoNfe;
import br.com.jcomputacao.cartaCorrecaoIntegracao.WsConnectionConfig;
import br.com.jcomputacao.exception.DbfException;
import br.com.jcomputacao.model.CartaCorrecaoModel;
import br.com.jcomputacao.util.StringUtil;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TConsSitNFe;
import br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes.TRetConsSitNFe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
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
public class ConsultaSituacaoNfe extends Servico {
    
    public TRetConsSitNFe consultarNfe(CartaCorrecaoModel cartaModel) throws DbfException {
        String cnpj = obtemCnpjEmitente(cartaModel);
        WsConnectionConfig.setProperties(cnpj);
        
        if(StringUtil.isNull(cartaModel.getNfeChaveAcesso())) {
            throw new DbfException("Nao e possivel consultar uma NFe sem chave de acesso");
        }
        TConsSitNFe consSitNFe = new TConsSitNFe();
        consSitNFe.setChNFe(cartaModel.getNfeChaveAcesso());
        consSitNFe.setTpAmb(Integer.toString(CartaCorrecaoUtil.getAmbiente())); //producao
        consSitNFe.setVersao("2.01");
        consSitNFe.setXServ("CONSULTAR");
        String xml = null;        
        try {            
            Marshaller marshaller = context.createMarshaller();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(consSitNFe, baos);
            xml = baos.toString();
            xml = xml.replaceAll("xmlns:ns2=\".+#\"\\s", "").replaceAll("ns2:", "");
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new DbfException("Erro ao tentar converter a consulta em XML", ex);
        }
        validar(xml);
        String fileName = cartaModel.getNfeChaveAcesso()+"-ped-sit.xml";
        try {
            escreve(fileName, xml);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar gravar o arquivo "+fileName, ex);
        }
        ServicoConsultaSituacaoNfe servico = new ServicoConsultaSituacaoNfe();
        try {
            xml = servico.executar(xml);
        } catch (AxisFault ex) {
            throw new DbfException("Erro ao tentar acessar acessar o servico da SEFAZ", ex);
        } catch (RemoteException ex) {
            throw new DbfException("Erro ao tentar acessar acessar o servico da SEFAZ", ex);
        } catch (XMLStreamException ex) {
            throw new DbfException("Erro de XML ao tentar acessar acessar o servico da SEFAZ", ex);
        }
        
        fileName = cartaModel.getNfeChaveAcesso()+"-sit.xml";
        try {
            escreve(fileName, xml);
        } catch (IOException ex) {
            throw new DbfException("Erro ao tentar gravar o arquivo "+fileName, ex);
        }
        
        TRetConsSitNFe rcr = null;
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            rcr = unmarshaller.unmarshal(new StreamSource(bais), TRetConsSitNFe.class).getValue();
            bais.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new DbfException("Erro ao tentar conveter o XML de retorno em resposta", ex);        
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new DbfException("Erro ao tentar conveter o XML de retorno em resposta", ex);
        }
        return rcr;
    }
    
    private void validar(String xml) throws DbfException {
        String urlXsd = System.getProperty("nfe.validacao.localizacao.consulta");
        validar(xml, urlXsd);
    }
}
