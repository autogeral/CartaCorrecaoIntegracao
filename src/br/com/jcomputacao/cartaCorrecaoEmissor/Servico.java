package br.com.jcomputacao.cartaCorrecaoEmissor;

import br.com.jcomputacao.exception.DbfDatabaseException;
import br.com.jcomputacao.model.CartaCorrecaoModel;
import br.com.jcomputacao.model.LojaModel;
import br.com.jcomputacao.model.beans.LojaBean;
import br.com.jcomputacao.util.StringUtil;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Murilo.Lima
 */
public class Servico {

    protected static JAXBContext context = null;
    protected static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    static {
        try {
            context = JAXBContext.newInstance("br.inf.portalfiscal.cartaCorrecao.xml.np2011003.cartasCorrecoes");
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, "Erro ao criar o contexto do JAXB", ex);
        }
    }

    protected String obtemCnpjEmitente(CartaCorrecaoModel cartaModel) throws DbfDatabaseException {
        LojaModel lojaModel = LojaBean.getLojaPorCodigo(cartaModel.getLoja());
        String ocnpj = StringUtil.somenteNumeros(lojaModel.getCnpj());
        ocnpj = StringUtil.ajusta(ocnpj, 14, StringUtil.ALINHAMENTO_DIREITA, '0');
        return ocnpj;
    }

    protected String trataString(String str) {
        String r = StringUtil.somenteNumerosELetras(str, true);
        r = StringUtil.soEspacoSimples(r).trim();
        r = StringUtil.noDeadKeysToUpperCase(r);
        return r;
    }

    protected String getCondicaoUso() {
        return "A Carta de Correcao e disciplinada pelo paragrafo 1o-A do art. 7o do Convenio S/N, de 15 de dezembro de 1970 e pode ser "
                + "utilizada para regularizacao de erro ocorrido na emissao de documento fiscal, desde que o erro nao esteja relacionado com: "
                + "I - as variaveis que determinam o valor do imposto tais como: base de calculo, aliquota, diferenca de preco, quantidade, valor da operacao ou da prestacao; "
                + "II - a correcao de dados cadastrais que implique mudanca do remetente ou do destinatario; III - a data de emissao ou de saida.";
    }
    
    protected String converteData(String dataCadastro) throws ParseException {       
        Timestamp data = Timestamp.valueOf(dataCadastro);
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
}
