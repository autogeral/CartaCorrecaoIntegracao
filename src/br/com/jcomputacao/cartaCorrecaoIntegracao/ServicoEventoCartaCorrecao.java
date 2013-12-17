/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.jcomputacao.cartaCorrecaoIntegracao;

import br.com.jcomputacao.cartaCorrecao.ws.recepcaoEvento.RecepcaoEventoStub;
import br.com.jcomputacao.cartaCorrecao.ws.recepcaoEvento.RecepcaoEventoStub.NfeRecepcaoEventoResult;
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
        RecepcaoEventoStub stub = new RecepcaoEventoStub();
        RecepcaoEventoStub.NfeCabecMsg7 cabecalho = new RecepcaoEventoStub.NfeCabecMsg7();
        RecepcaoEventoStub.NfeCabecMsg cabec = new RecepcaoEventoStub.NfeCabecMsg();

        cabec.setCUF("35");
        cabec.setVersaoDados("1.00");
        cabecalho.setNfeCabecMsg(cabec);

        RecepcaoEventoStub.NfeDadosMsg mensagem = new RecepcaoEventoStub.NfeDadosMsg();
        OMElement element = AXIOMUtil.stringToOM(xml);
        mensagem.setExtraElement(element);
        
        NfeRecepcaoEventoResult resultado = stub.nfeRecepcaoEvento(mensagem, cabecalho);
        element = resultado.getExtraElement();
        return element.toString();
    }
}
