package br.com.jcomputacao.cartaCorrecaoIntegracao;

import br.com.jcomputacao.cartaCorrecao.util.CartaCorrecaoUFUtil;
import br.com.jcomputacao.nfe.ws.consultaProtocolo.NfeConsulta2Stub;
import br.com.jcomputacao.nfe.ws.consultaProtocolo.NfeConsulta2Stub.NfeConsultaNF2Result;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axis2.AxisFault;

/**
 *
 * @author Murilo.Lima
 */
public class ServicoConsultaSituacaoNfe {

    public String executar(String xml) throws AxisFault, XMLStreamException, RemoteException {
        return executar(xml, CartaCorrecaoUFUtil.SAO_PAULO);
    }
    
    public String executar(String xml, CartaCorrecaoUFUtil uf) throws AxisFault, XMLStreamException, RemoteException{
//        WsConnectionConfig.setProperties();
        NfeConsulta2Stub stub = new NfeConsulta2Stub();
        NfeConsulta2Stub.NfeDadosMsg dados = new NfeConsulta2Stub.NfeDadosMsg();
        NfeConsulta2Stub.NfeCabecMsg2 cabec = new NfeConsulta2Stub.NfeCabecMsg2();
        NfeConsulta2Stub.NfeCabecMsg param = new NfeConsulta2Stub.NfeCabecMsg();
        param.setVersaoDados("2.01");
        param.setCUF(uf.getCodigo());
        cabec.setNfeCabecMsg(param);
        
        OMElement el = AXIOMUtil.stringToOM(xml);
        dados.setExtraElement(el);
        
        NfeConsultaNF2Result resultado = stub.nfeConsultaNF2(dados, cabec);
        String s = resultado.getExtraElement().toString();
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINER, s);
        return s;
    }
}
