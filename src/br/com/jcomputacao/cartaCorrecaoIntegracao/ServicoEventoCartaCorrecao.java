/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.jcomputacao.cartaCorrecaoIntegracao;

import br.com.jcomputacao.cartaCorrecao.ws.recepcaoEvento4.NFeRecepcaoEvento4Stub;
import java.rmi.RemoteException;
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axis2.AxisFault;

/**
 *
 * @author Murilo.Lima
 */
public class ServicoEventoCartaCorrecao {

    public String executar(String xml) throws AxisFault, XMLStreamException, RemoteException {
        NFeRecepcaoEvento4Stub stub = new NFeRecepcaoEvento4Stub();        
        OMElement element = AXIOMUtil.stringToOM(xml);
        NFeRecepcaoEvento4Stub.NfeDadosMsg mensagem = new NFeRecepcaoEvento4Stub.NfeDadosMsg();        
        mensagem.setExtraElement(element);        

        NFeRecepcaoEvento4Stub.NfeResultMsg resultado = stub.nfeRecepcaoEvento(mensagem);
        element = resultado.getExtraElement();
        return element.toString();
    }
}
