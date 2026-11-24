import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Trabalho de Lógica de Programação - Controlando uma Impressora Térmica Elgin (i8 / i9).
 * Todo o programa fica neste arquivo: login, menu e comandos da impressora.
 */
public class Main {

    // =====================================================================
    // PARTE PRONTA: ligação do Java com a DLL da Elgin (via JNA)
    // =====================================================================
    public interface ImpressoraDLL extends Library {
        // procura a DLL na pasta do projeto (Working directory)
        ImpressoraDLL INSTANCE = (ImpressoraDLL) Native.load(
                Paths.get("E1_Impressora01.dll").toAbsolutePath().toString(),
                ImpressoraDLL.class);

        int AbreConexaoImpressora(int tipo, String modelo, String conexao, int parametro);
        int FechaConexaoImpressora();
        int ImpressaoTexto(String dados, int posicao, int estilo, int tamanho);
        int Corte(int avanco);
        int ImpressaoQRCode(String dados, int tamanho, int nivelCorrecao);
        int ImpressaoCodigoBarras(int tipo, String dados, int altura, int largura, int HRI);
        int AvancaPapel(int linhas);
        int StatusImpressora(int param);
        int AbreGaveta(int pino, int ti, int tf);
        int SinalSonoro(int qtd, int tempoInicio, int tempoFim);
        int ImprimeXMLSAT(String dados, int param);
        int ImprimeXMLCancelamentoSAT(String dados, String assQRCode, int param);
    }

    // =====================================================================
    // VARIÁVEIS GLOBAIS
    // =====================================================================
    private static final String USUARIO = "admin";
    private static final String SENHA = "1234";

    // IMPORTANTE: troque pelo valor original que veio no Main.java do professor.
    private static final String ASS_QRCODE = "COLE_AQUI_A_CHAVE_ASS_QRCODE_DO_PROFESSOR";

    // Dados da conexão com a impressora (valores de teste: USB + i9)
    private static boolean conexaoAberta = false;
    private static int tipo = 1;
    private static String modelo = "i9";
    private static String conexao = "USB";
    private static int parametro = 0;

    private static final Scanner teclado = new Scanner(System.in);

    // =====================================================================
    // MÉTODOS AUXILIARES (prontos)
    // =====================================================================

    /** Mostra a mensagem e devolve o texto digitado. */
    public static String capturarEntrada(String mensagem) {
        System.out.print(mensagem);
        return teclado.nextLine();
    }

    /** Mostra a mensagem e devolve um número inteiro. Se digitar letras, pergunta de novo. */
    public static int lerInteiro(String mensagem) {
        while (true) {
            String texto = capturarEntrada(mensagem).trim();
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido. Digite apenas números.");
            }
        }
    }

    /** Lê um arquivo XML e devolve o texto dele (ou null se não achar). */
    public static String lerArquivo(String nome) {
        try {
            return new String(Files.readAllBytes(Paths.get(nome)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    // =====================================================================
    // LOGIN E MENU
    // =====================================================================

    /** Pede usuário e senha (3 tentativas). Devolve true se acertar e false se errar as 3. */
    public static boolean fazerLogin() {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            System.out.println("\n=== LOGIN (tentativa " + tentativa + " de 3) ===");
            String usuario = capturarEntrada("Usuário: ");
            String senha = capturarEntrada("Senha: ");

            if (usuario.equals(USUARIO) && senha.equals(SENHA)) {
                System.out.println("Login realizado com sucesso!");
                return true;
            }

            int restantes = 3 - tentativa;
            if (restantes > 0) {
                System.out.println("Usuário ou senha incorretos. Tentativas restantes: " + restantes);
            }
        }
        // chegou aqui: errou as 3 vezes
        System.out.println("Número máximo de tentativas atingido. Programa encerrado.");
        return false;
    }

    /** Mostra o menu principal. */
    public static void exibirMenu() {
        System.out.println("\n*************************************");
        System.out.println("*     IMPRESSORA ELGIN - MENU       *");
        System.out.println("*************************************");
        System.out.println("1  - Configurar conexão");
        System.out.println("2  - Abrir conexão");
        System.out.println("3  - Fechar conexão");
        System.out.println("4  - Imprimir texto");
        System.out.println("5  - Imprimir QR Code");
        System.out.println("6  - Imprimir código de barras");
        System.out.println("7  - Imprimir XML SAT");
        System.out.println("8  - Imprimir XML de cancelamento SAT");
        System.out.println("9  - Abrir gaveta");
        System.out.println("10 - Sinal sonoro");
        System.out.println("11 - Avançar papel");
        System.out.println("12 - Status da impressora");
        System.out.println("0  - Sair");
        System.out.println("*************************************");
    }

    // =====================================================================
    // CONEXÃO
    // =====================================================================

    /** Pergunta os dados da conexão e guarda nas variáveis globais. */
    public static void configurarConexao() {
        if (conexaoAberta) {
            System.out.println("Atenção: a conexão já está aberta. "
                    + "Os novos dados só valem na próxima vez que você abrir a conexão.");
        }
        tipo = lerInteiro("Tipo (1 = USB, 2 = Serial, 3 = Rede, 4 = Bluetooth): ");
        modelo = capturarEntrada("Modelo (i8 ou i9): ");
        conexao = capturarEntrada("Conexão/porta (ex.: USB): ");
        parametro = lerInteiro("Parâmetro (0 para USB): ");
        System.out.println("Configuração salva: tipo=" + tipo + ", modelo=" + modelo
                + ", conexão=" + conexao + ", parâmetro=" + parametro);
    }

    /** Abre a conexão com a impressora (exemplo 1). */
    public static void abrirConexao() {
        if (conexaoAberta) {
            System.out.println("A conexão já está aberta.");
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.AbreConexaoImpressora(tipo, modelo, conexao, parametro);
        if (retorno == 0) {
            conexaoAberta = true;
            System.out.println("Conexão aberta com sucesso!");
        } else {
            System.out.println("Erro ao abrir a conexão. Código: " + retorno);
        }
    }

    /** Fecha a conexão com a impressora. */
    public static void fecharConexao() {
        if (!conexaoAberta) {
            System.out.println("A conexão já está fechada.");
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.FechaConexaoImpressora();
        if (retorno == 0) {
            System.out.println("Conexão fechada com sucesso!");
        } else {
            System.out.println("Erro ao fechar a conexão. Código: " + retorno);
        }
        conexaoAberta = false;
    }

    // =====================================================================
    // IMPRESSÃO E COMANDOS (todos seguem a mesma receita)
    // =====================================================================

    /** Imprime um texto (exemplo 2, já vinha pronto). */
    public static void imprimirTexto() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        String texto = capturarEntrada("Texto a imprimir: ");
        int copias = lerInteiro("Quantas cópias? ");
        for (int i = 1; i <= copias; i++) {
            int retorno = ImpressoraDLL.INSTANCE.ImpressaoTexto(texto, 1, 4, 0);
            if (retorno >= 0) { // ImpressaoTexto: só é erro se for negativo
                System.out.println("Cópia " + i + " impressa!");
            } else {
                System.out.println("Erro ao imprimir. Código: " + retorno);
            }
        }
        ImpressoraDLL.INSTANCE.Corte(5);
    }

    /** Pergunta o conteúdo e imprime um QR Code. */
    public static void imprimirQRCode() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        String dados = capturarEntrada("Conteúdo do QR Code: ");
        int retorno = ImpressoraDLL.INSTANCE.ImpressaoQRCode(dados, 6, 4);
        if (retorno == 0) {
            System.out.println("QR Code impresso!");
        } else {
            System.out.println("Erro ao imprimir o QR Code. Código: " + retorno);
        }
        ImpressoraDLL.INSTANCE.Corte(5);
    }

    /** Imprime um código de barras CODE 128 de exemplo. */
    public static void imprimirCodigoBarras() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        // tipo 8 = CODE 128 (o texto começa com {A), altura 100, largura 2, HRI 3 = número acima e abaixo
        int retorno = ImpressoraDLL.INSTANCE.ImpressaoCodigoBarras(8, "{A012345678912", 100, 2, 3);
        if (retorno == 0) {
            System.out.println("Código de barras impresso!");
        } else {
            System.out.println("Erro ao imprimir o código de barras. Código: " + retorno);
        }
        ImpressoraDLL.INSTANCE.Corte(5);
    }

    /** Pergunta o nome do arquivo XML, lê e imprime o cupom SAT. */
    public static void imprimirXMLSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        String nomeArquivo = capturarEntrada("Nome do arquivo XML (ex.: XMLSAT.xml): ");
        String xml = lerArquivo(nomeArquivo);
        if (xml == null) {
            System.out.println("Não foi possível ler o arquivo: " + nomeArquivo);
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLSAT(xml, 0);
        if (retorno == 0) {
            System.out.println("Cupom SAT impresso!");
        } else {
            System.out.println("Erro ao imprimir o XML SAT. Código: " + retorno);
        }
        ImpressoraDLL.INSTANCE.Corte(5);
    }

    /** Igual ao anterior, mas para o cancelamento do SAT. */
    public static void imprimirXMLCancelamentoSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        String nomeArquivo = capturarEntrada("Nome do arquivo XML (ex.: CANC_SAT.xml): ");
        String xml = lerArquivo(nomeArquivo);
        if (xml == null) {
            System.out.println("Não foi possível ler o arquivo: " + nomeArquivo);
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLCancelamentoSAT(xml, ASS_QRCODE, 0);
        if (retorno == 0) {
            System.out.println("Cancelamento SAT impresso!");
        } else {
            System.out.println("Erro ao imprimir o cancelamento SAT. Código: " + retorno);
        }
        ImpressoraDLL.INSTANCE.Corte(5);
    }

    /** Abre a gaveta de dinheiro. */
    public static void abrirGaveta() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.AbreGaveta(1, 5, 10);
        if (retorno == 0) {
            System.out.println("Gaveta aberta!");
        } else {
            System.out.println("Erro ao abrir a gaveta. Código: " + retorno);
        }
    }

    /** Pergunta quantos bips e faz a impressora apitar. */
    public static void sinalSonoro() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        int quantidade = lerInteiro("Quantos bips (1 a 63)? ");
        int retorno = ImpressoraDLL.INSTANCE.SinalSonoro(quantidade, 50, 5);
        if (retorno == 0) {
            System.out.println("Bip!");
        } else {
            System.out.println("Erro no sinal sonoro. Código: " + retorno);
        }
    }

    /** (Extra) Pergunta quantas linhas e avança o papel. */
    public static void avancarPapel() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        int linhas = lerInteiro("Quantas linhas avançar? ");
        int retorno = ImpressoraDLL.INSTANCE.AvancaPapel(linhas);
        if (retorno == 0) {
            System.out.println("Papel avançado!");
        } else {
            System.out.println("Erro ao avançar o papel. Código: " + retorno);
        }
    }

    /** (Extra) Consulta o status da impressora. */
    public static void statusImpressora() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }
        int consulta = lerInteiro("Status de qual item? (1 = gaveta, 2 = tampa, 3 = papel, 5 = geral): ");
        if (consulta != 1 && consulta != 2 && consulta != 3 && consulta != 5) {
            System.out.println("Opção de status inválida.");
            return;
        }
        int retorno = ImpressoraDLL.INSTANCE.StatusImpressora(consulta);
        if (retorno >= 0) {
            // aqui o número devolvido é o próprio status (veja a documentação da Elgin)
            System.out.println("Status consultado com sucesso. Valor retornado: " + retorno);
        } else {
            System.out.println("Erro ao consultar o status. Código: " + retorno);
        }
    }

    // =====================================================================
    // PROGRAMA PRINCIPAL
    // =====================================================================
    public static void main(String[] args) {
        if (!fazerLogin()) {
            return; // errou 3 vezes: encerra o programa
        }

        int opcao = -1;
        while (opcao != 0) {
            exibirMenu();
            opcao = lerInteiro("Digite a opção desejada: ");

            if (opcao == 1) {
                configurarConexao();
            } else if (opcao == 2) {
                abrirConexao();
            } else if (opcao == 3) {
                fecharConexao();
            } else if (opcao == 4) {
                imprimirTexto();
            } else if (opcao == 5) {
                imprimirQRCode();
            } else if (opcao == 6) {
                imprimirCodigoBarras();
            } else if (opcao == 7) {
                imprimirXMLSAT();
            } else if (opcao == 8) {
                imprimirXMLCancelamentoSAT();
            } else if (opcao == 9) {
                abrirGaveta();
            } else if (opcao == 10) {
                sinalSonoro();
            } else if (opcao == 11) {
                avancarPapel();
            } else if (opcao == 12) {
                statusImpressora();
            } else if (opcao == 0) {
                fecharConexao();
                System.out.println("Programa encerrado. Até logo!");
            } else {
                System.out.println("Opção inválida.");
            }
        }
    }
}
