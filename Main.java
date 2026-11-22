import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    // Interface que representa os comandos disponíveis na DLL da Elgin.
    // Não altere os nomes nem os parâmetros dos métodos.
    public interface ImpressoraDLL extends Library {
        ImpressoraDLL INSTANCE = Native.load(
                Paths.get("E1_Impressora01.dll").toAbsolutePath().toString(),
                ImpressoraDLL.class
        );

        int AbreConexaoImpressora(int tipo, String modelo, String conexao, int param);
        int FechaConexaoImpressora();
        int ImpressaoTexto(String dados, int posicao, int estilo, int tamanho);
        int Corte(int avanco);
        int ImpressaoQRCode(String dados, int tamanho, int nivelCorrecao);
        int ImpressaoCodigoBarras(int tipo, String dados, int altura, int largura, int HRI);
        int AvancaPapel(int linhas);
        int StatusImpressora(int param);
        int AbreGavetaElgin(int pino, int ti, int tf);
        int AbreGaveta(int pino, int ti, int tf);
        int SinalSonoro(int qtd, int tempoInicio, int tempoFim);
        int ModoPagina();
        int LimpaBufferModoPagina();
        int ImprimeModoPagina();
        int ModoPadrao();
        int PosicaoImpressaoHorizontal(int posicao);
        int PosicaoImpressaoVertical(int posicao);
        int ImprimeXMLSAT(String dados, int param);
        int ImprimeXMLCancelamentoSAT(String dados, String assQRCode, int param);
    }

    // Dados de acesso ao programa.
    private static final String USUARIO = "admin";
    private static final String SENHA = "1234";

    // Chave fornecida no arquivo "ASS_QRCODE Cancelamento Sat.txt".
    private static final String ASS_QRCODE =
            "Q5DLkpdRijIRGY6YSSNsTWK1TztHL1vD0V1Jc4spo/CEUqICEb9SFy82ym8EhBRZjbh3btsZhF+sjHqEMR159i4agru9x6KsepK/q0E2e5xlU5cv3m1woYfgHyOkWDNcSdMsS6bBh2Bpq6s89yJ9Q6qh/J8YHi306ce9Tqb/drKvN2XdE5noRSS32TAWuaQEVd7u+TrvXlOQsE3fHR1D5f1saUwQLPSdIv01NF6Ny7jZwjCwv1uNDgGZONJdlTJ6p0ccqnZvuE70aHOI09elpjEO6Cd+orI7XHHrFCwhFhAcbalc+ZfO5b/+vkyAHS6CYVFCDtYR9Hi5qgdk31v23w==";

    private static boolean conexaoAberta = false;
    private static int tipo;
    private static String modelo;
    private static String conexao;
    private static int parametro;

    private static final Scanner scanner = new Scanner(System.in);

    // Lê um texto digitado pelo usuário.
    private static String capturarEntrada(String mensagem) {
        System.out.print(mensagem);
        return scanner.nextLine();
    }

    // Lê um número inteiro. Se o usuário digitar letras, pergunta novamente.
    private static int lerInteiro(String mensagem) {
        while (true) {
            System.out.print(mensagem);
            String entrada = scanner.nextLine();

            try {
                return Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro válido.");
            }
        }
    }

    // Lê um arquivo de texto/XML. Retorna null se não conseguir encontrá-lo.
    private static String lerArquivo(String nome) {
        try {
            return Files.readString(Paths.get(nome), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    // Solicita os dados necessários para conectar à impressora.
    public static void configurarConexao() {
        if (conexaoAberta) {
            System.out.println("Feche a conexão atual antes de configurar outra.");
            return;
        }

        System.out.println("\n--- CONFIGURAÇÃO DA CONEXÃO ---");
        System.out.println("Tipo: 1 = USB | 2 = Serial | 3 = Rede (IP) | 4 = Bluetooth");

        tipo = lerInteiro("Tipo de conexão: ");
        modelo = capturarEntrada("Modelo da impressora (i8 ou i9): ").trim();
        conexao = capturarEntrada("Porta/conexão (para USB, digite USB): ").trim();
        parametro = lerInteiro("Parâmetro (para USB, digite 0): ");

        System.out.println("Configuração salva.");
    }

    // Abre a comunicação com a impressora.
    public static void abrirConexao() {
        if (conexaoAberta) {
            System.out.println("A conexão já está aberta.");
            return;
        }

        if (modelo == null || conexao == null) {
            System.out.println("Configure a conexão primeiro (opção 1).");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AbreConexaoImpressora(
                tipo, modelo, conexao, parametro
        );

        if (retorno == 0) {
            conexaoAberta = true;
            System.out.println("Conexão aberta com sucesso.");
        } else {
            System.out.println("Erro ao abrir conexão. Código: " + retorno);
        }
    }

    // Fecha a comunicação com a impressora.
    public static void fecharConexao() {
        if (!conexaoAberta) {
            System.out.println("Não existe conexão aberta.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.FechaConexaoImpressora();

        if (retorno == 0) {
            conexaoAberta = false;
            System.out.println("Conexão fechada com sucesso.");
        } else {
            System.out.println("Erro ao fechar conexão. Código: " + retorno);
        }
    }

    // Exemplo de impressão de texto.
    // Diferentemente dos outros comandos, retorno negativo indica erro.
    public static void imprimirTexto() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String texto = capturarEntrada("Texto a imprimir: ");
        int copias = lerInteiro("Quantas cópias? ");

        if (copias <= 0) {
            System.out.println("A quantidade de cópias deve ser maior que zero.");
            return;
        }

        for (int i = 1; i <= copias; i++) {
            int retorno = ImpressoraDLL.INSTANCE.ImpressaoTexto(texto, 1, 4, 0);

            if (retorno >= 0) {
                System.out.println("Cópia " + i + " enviada para impressão.");
            } else {
                System.out.println("Erro ao imprimir. Código: " + retorno);
            }
        }

        int retornoCorte = ImpressoraDLL.INSTANCE.Corte(5);
        if (retornoCorte != 0) {
            System.out.println("Erro ao cortar o papel. Código: " + retornoCorte);
        }
    }

    // Imprime um QR Code e corta o papel.
    public static void imprimirQRCode() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String dados = capturarEntrada("Conteúdo do QR Code: ");
        int retorno = ImpressoraDLL.INSTANCE.ImpressaoQRCode(dados, 6, 4);

        if (retorno == 0) {
            System.out.println("QR Code impresso com sucesso.");

            int retornoCorte = ImpressoraDLL.INSTANCE.Corte(5);
            if (retornoCorte != 0) {
                System.out.println("Erro ao cortar o papel. Código: " + retornoCorte);
            }
        } else {
            System.out.println("Erro ao imprimir QR Code. Código: " + retorno);
        }
    }

    // Imprime um código de barras no padrão CODE 128.
    public static void imprimirCodigoBarras() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String dados = capturarEntrada(
                "Dados do código de barras (exemplo: {A012345678912): "
        );

        int retorno = ImpressoraDLL.INSTANCE.ImpressaoCodigoBarras(
                8, dados, 100, 2, 3
        );

        if (retorno == 0) {
            System.out.println("Código de barras impresso com sucesso.");

            int retornoCorte = ImpressoraDLL.INSTANCE.Corte(5);
            if (retornoCorte != 0) {
                System.out.println("Erro ao cortar o papel. Código: " + retornoCorte);
            }
        } else {
            System.out.println("Erro ao imprimir código de barras. Código: " + retorno);
        }
    }

    // Lê e imprime um XML SAT.
    public static void imprimirXMLSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String nomeArquivo = capturarEntrada(
                "Nome do arquivo XML (exemplo: XMLSAT.xml): "
        ).trim();

        String xml = lerArquivo(nomeArquivo);

        if (xml ==  null) {
            System.out.println("Não foi possível ler o arquivo: " + nomeArquivo);
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLSAT(xml, 0);

        if (retorno == 0) {
            System.out.println("XML SAT enviado para impressão com sucesso.");
        } else {
            System.out.println("Erro ao imprimir XML SAT. Código: " + retorno);
        }
    }

    // Lê e imprime o XML de cancelamento SAT.
    public static void imprimirXMLCancelamentoSAT() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        String nomeArquivo = capturarEntrada(
                "Nome do arquivo de cancelamento (exemplo: CANC_SAT.xml): "
        ).trim();

        String xml = lerArquivo(nomeArquivo);

        if (xml == null) {
            System.out.println("Não foi possível ler o arquivo: " + nomeArquivo);
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.ImprimeXMLCancelamentoSAT(
                xml, ASS_QRCODE, 0
        );

        if (retorno == 0) {
            System.out.println("XML de cancelamento enviado com sucesso.");
        } else {
            System.out.println("Erro ao imprimir cancelamento SAT. Código: " + retorno);
        }
    }

    // Abre a gaveta de dinheiro.
    public static void abrirGaveta() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AbreGaveta(1, 5, 10);

        if (retorno == 0) {
            System.out.println("Comando para abrir a gaveta enviado com sucesso.");
        } else {
            System.out.println("Erro ao abrir a gaveta. Código: " + retorno);
        }
    }

    // Faz a impressora emitir sinais sonoros.
    public static void sinalSonoro() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int quantidade = lerInteiro("Quantos bips? (1 a 63): ");

        if (quantidade < 1 || quantidade > 63) {
            System.out.println("A quantidade deve estar entre 1 e 63.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.SinalSonoro(quantidade, 50, 5);

        if (retorno == 0) {
            System.out.println("Sinal sonoro executado com sucesso.");
        } else {
            System.out.println("Erro no sinal sonoro. Código: " + retorno);
        }
    }

    // Avança o papel pela quantidade de linhas informada.
    public static void avancarPapel() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        int linhas = lerInteiro("Quantas linhas deseja avançar? ");

        if (linhas <= 0) {
            System.out.println("A quantidade de linhas deve ser maior que zero.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.AvancaPapel(linhas);

        if (retorno == 0) {
            System.out.println("Papel avançado com sucesso.");
        } else {
            System.out.println("Erro ao avançar o papel. Código: " + retorno);
        }
    }

    // Consulta o status solicitado: 1 gaveta, 2 tampa, 3 papel ou 5 geral.
    // O retorno é exibido como valor de status, não como código de sucesso/erro.
    public static void consultarStatus() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes (opção 2).");
            return;
        }

        System.out.println("Status: 1 = gaveta | 2 = tampa | 3 = papel | 5 = geral");
        int parametroStatus = lerInteiro("Qual status deseja consultar? ");

        if (parametroStatus != 1 && parametroStatus != 2
                && parametroStatus != 3 && parametroStatus != 5) {
            System.out.println("Parâmetro de status inválido.");
            return;
        }

        int retorno = ImpressoraDLL.INSTANCE.StatusImpressora(parametroStatus);
        System.out.println("Retorno do status da impressora: " + retorno);
    }

    // Mostra as opções disponíveis.
    public static void exibirMenu() {
        System.out.println("\n========================================");
        System.out.println("           MENU IMPRESSORA");
        System.out.println("========================================");
        System.out.println("1  - Configurar conexão");
        System.out.println("2  - Abrir conexão");
        System.out.println("3  - Consultar status da impressora");
        System.out.println("4  - Imprimir texto");
        System.out.println("5  - Imprimir QR Code");
        System.out.println("6  - Imprimir código de barras");
        System.out.println("7  - Imprimir XML SAT");
        System.out.println("8  - Imprimir XML de cancelamento SAT");
        System.out.println("9  - Abrir gaveta");
        System.out.println("10 - Sinal sonoro");
        System.out.println("11 - Avançar papl");
        System.out.println("0  - Sair");
        System.out.println("=======================================");
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("       SISTEMA DE IMPRESSORA ELGIN");
        System.out.println("========================================");

        if (!fazerLogin()) {
            scanner.close();
            return;
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
                consultarStatus();
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
            } else if (opcao == 0) {
                fecharConexao();
                System.out.println("Programa encerrado.");
            } else {
                System.out.println("Opção inválida. Tente novamente.");
            }
        }

        scanner.close();
    }

    // Permite até três tentativas de login.
    public static boolean fazerLogin() {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            System.out.println("\n--- LOGIN ---");
            String usuario = capturarEntrada("Usuário: ");
            String senha = capturarEntrada("Senha: ");

            if (usuario.equals(USUARIO) && senha.equals(SENHA)) {
                System.out.println("Login realizado com sucesso.");
                return true;
            }

            int restantes = 3 - tentativa;
            if (restantes > 0) {
                System.out.println("Usuário ou senha incorretos. Tentativas restantes: "
                        + restantes);
            }
        }

        System.out.println("Número máximo de tentativas atingido. Programa encerrado.");
        return false;
    }
}
