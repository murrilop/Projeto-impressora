import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    // Interface da DLL Elgin. Os nomes e parâmetros seguem a documentação do projeto.
    public interface ImpressoraDLL extends Library {
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
        int ModoPagina();
        int LimpaBufferModoPagina();
        int ImprimeModoPagina();
        int ModoPadrao();
        int PosicaoImpressaoHorizontal(int posicao);
        int PosicaoImpressaoVertical(int posicao);
        int ImprimeXMLSAT(String dados, int param);
        int ImprimeXMLCancelamentoSAT(String dados, String assQRCode, int param);
    }

    private static final String USUARIO = "admin";
    private static final String SENHA = "1234";

    // Chave do arquivo ASS_QRCODE Cancelamento Sat.txt fornecido no projeto.
    private static final String ASS_QRCODE =
            "Q5DLkpdRijIRGY6YSSNsTWK1TztHL1vD0V1Jc4spo/CEUqICEb9SFy82ym8EhBRZjbh3btsZhF+sjHqEMR159i4agru9x6KsepK/q0E2e5xlU5cv3m1woYfgHyOkWDNcSdMsS6bBh2Bpq6s89yJ9Q6qh/J8YHi306ce9Tqb/drKvN2XdE5noRSS32TAWuaQEVd7u+TrvXlOQsE3fHR1D5f1saUwQLPSdIv01NF6Ny7jZwjCwv1uNDgGZONJdlTJ6p0ccqnZvuE70aHOI09elpjEO6Cd+orI7XHHrFCwhFhAcbalc+ZfO5b/+vkyAHS6CYVFCDtYR9Hi5qgdk31v23w==";

    private static ImpressoraDLL dll;
    private static boolean conexaoAberta = false;
    private static int tipo;
    private static String modelo;
    private static String conexao;
    private static int parametro;

    private static final Scanner scanner = new Scanner(System.in);

    private static ImpressoraDLL impressora() {
        if (dll == null) {
            Path caminhoDll = Paths.get("E1_Impressora01.dll").toAbsolutePath();
            dll = Native.load(caminhoDll.toString(), ImpressoraDLL.class);
        }
        return dll;
    }

    private static String capturarEntrada(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                return scanner.nextLine().trim();
            } catch (Exception e) {
                System.out.println("Não foi possível ler a entrada. Tente novamente.");
            }
        }
    }

    private static int lerInteiro(String mensagem) {
        while (true) {
            try {
                System.out.print(mensagem);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Digite um número inteiro.");
            } catch (Exception e) {
                System.out.println("Não foi possível ler a entrada. Tente novamente.");
            }
        }
    }

    private static int lerFaixa(String mensagem, int minimo, int maximo) {
        while (true) {
            int valor = lerInteiro(mensagem);
            if (valor >= minimo && valor <= maximo) {
                return valor;
            }
            System.out.println("Digite um valor entre " + minimo + " e " + maximo + ".");
        }
    }

    private static boolean exigirConexao() {
        if (!conexaoAberta) {
            System.out.println("Conexão fechada. Abra a conexão antes de continuar (opção 2).");
            return false;
        }
        return true;
    }

    private static void informarRetorno(String operacao, int retorno) {
        if (retorno == 0) {
            System.out.println(operacao + " realizado(a) com sucesso.");
        } else {
            System.out.println("Erro ao " + operacao.toLowerCase() + ". Código: " + retorno);
        }
    }

    // Lê XML/texto usando UTF-8. O caminho relativo é considerado a pasta do projeto.
    private static String lerArquivo(String nome) {
        try {
            Path arquivo = Paths.get(nome);
            if (!Files.exists(arquivo) || !Files.isRegularFile(arquivo)) {
                System.out.println("Arquivo não encontrado: " + arquivo.toAbsolutePath());
                return null;
            }
            return new String(Files.readAllBytes(arquivo), StandardCharsets.UTF_8);
        } catch (IOException | SecurityException e) {
            System.out.println("Não foi possível ler o arquivo: " + e.getMessage());
            return null;
        }
    }

    public static void configurarConexao() {
        try {
            if (conexaoAberta) {
                System.out.println("Feche a conexão atual antes de configurar outra.");
                return;
            }

            System.out.println("\n--- CONFIGURAÇÃO DA CONEXÃO ---");
            System.out.println("1 = USB | 2 = Serial | 3 = Rede (IP) | 4 = Bluetooth");
            int novoTipo = lerFaixa("Tipo de conexão: ", 1, 4);
            String novoModelo = capturarEntrada("Modelo da impressora (i8 ou i9): ").toLowerCase();

            if (!novoModelo.equals("i8") && !novoModelo.equals("i9")) {
                System.out.println("Modelo inválido. Digite i8 ou i9.");
                return;
            }

            // Os quatro campos são solicitados explicitamente, inclusive em USB.
            String novaConexao = capturarEntrada(
                    "Conexão (USB, porta COM, endereço IP ou Bluetooth): ");
            if (novaConexao.isEmpty()) {
                System.out.println("O campo conexão não pode ficar vazio.");
                return;
            }

            int novoParametro = lerInteiro("Parâmetro da conexão (USB: 0; rede: porta TCP): ");

            if (novoTipo == 1 && (!novaConexao.equalsIgnoreCase("USB") || novoParametro != 0)) {
                System.out.println("Para USB, informe conexão USB e parâmetro 0.");
                return;
            }
            if (novoTipo == 3 && (novoParametro < 1 || novoParametro > 65535)) {
                System.out.println("A porta TCP deve estar entre 1 e 65535.");
                return;
            }
            if (novoTipo == 2 && novoParametro <= 0) {
                System.out.println("O parâmetro serial (baudrate) deve ser maior que zero.");
                return;
            }

            tipo = novoTipo;
            modelo = novoModelo;
            conexao = novaConexao;
            parametro = novoParametro;

            System.out.println("\nConfiguração salva:");
            System.out.println("Tipo: " + tipo);
            System.out.println("Modelo: " + modelo);
            System.out.println("Conexão: " + conexao);
            System.out.println("Parâmetro: " + parametro);
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao configurar a conexão: " + e.getMessage());
        }
    }

    public static void abrirConexao() {
        try {
            if (conexaoAberta) {
                System.out.println("A conexão já está aberta.");
                return;
            }
            if (modelo == null || conexao == null) {
                System.out.println("Configure a conexão primeiro (opção 1).");
                return;
            }

            int retorno = impressora().AbreConexaoImpressora(tipo, modelo, conexao, parametro);
            if (retorno == 0) {
                conexaoAberta = true;
                System.out.println("Conexão aberta com sucesso.");
            } else {
                System.out.println("Erro ao abrir conexão. Código: " + retorno);
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Não foi possível abrir a conexão: " + e.getMessage());
        }
    }

    public static void fecharConexao() {
        try {
            if (!conexaoAberta) {
                System.out.println("Não existe conexão aberta.");
                return;
            }
            int retorno = impressora().FechaConexaoImpressora();
            if (retorno == 0) {
                conexaoAberta = false;
                System.out.println("Conexão fechada com sucesso.");
            } else {
                System.out.println("Erro ao fechar conexão. Código: " + retorno);
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao fechar a conexão: " + e.getMessage());
        }
    }

    public static void imprimirTexto() {
        try {
            if (!exigirConexao()) return;

            String texto = capturarEntrada("Texto a imprimir: ");
            if (texto.isEmpty()) {
                System.out.println("O texto não pode ficar vazio.");
                return;
            }
            int copias = lerFaixa("Quantas cópias? (1 a 20): ", 1, 20);

            for (int i = 1; i <= copias; i++) {
                // Posição 0 = esquerda; estilo 8 = negrito; tamanho 2 = ampliado.
                // ImpressaoTexto retorna a quantidade de dados enviados; erro é retorno negativo.
                int retorno = impressora().ImpressaoTexto(texto + "\n", 0, 8, 2);
                if (retorno < 0) {
                    System.out.println("Erro na cópia " + i + ". Código: " + retorno);
                    return;
                }
                System.out.println("Cópia " + i + " enviada (" + retorno + " bytes/dados).");
            }

            int corte = impressora().Corte(5);
            if (corte == 0) {
                System.out.println("Papel avançado e cortado.");
            } else {
                System.out.println("Texto enviado, mas houve erro no corte. Código: " + corte);
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha na impressão de texto: " + e.getMessage());
        }
    }

    public static void imprimirQRCode() {
        try {
            if (!exigirConexao()) return;
            String dados = capturarEntrada("Conteúdo do QR Code: ");
            if (dados.isEmpty()) {
                System.out.println("O conteúdo do QR Code não pode ficar vazio.");
                return;
            }
            int retorno = impressora().ImpressaoQRCode(dados, 6, 4);
            if (retorno == 0) {
                System.out.println("QR Code impresso com sucesso.");
                int corte = impressora().Corte(5);
                if (corte != 0) System.out.println("Erro ao cortar o papel. Código: " + corte);
            } else {
                System.out.println("Erro ao imprimir QR Code. Código: " + retorno);
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha no QR Code: " + e.getMessage());
        }
    }

    public static void imprimirCodigoBarras() {
        try {
            if (!exigirConexao()) return;
            String dados = capturarEntrada(
                    "Dados do código de barras (CODE 128; exemplo: {A012345678912): ");
            if (dados.isEmpty()) {
                System.out.println("Os dados do código de barras não podem ficar vazios.");
                return;
            }
            int retorno = impressora().ImpressaoCodigoBarras(8, dados, 100, 2, 3);
            if (retorno == 0) {
                System.out.println("Código de barras impresso com sucesso.");
                int corte = impressora().Corte(5);
                if (corte != 0) System.out.println("Erro ao cortar o papel. Código: " + corte);
            } else {
                System.out.println("Erro ao imprimir código de barras. Código: " + retorno);
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha no código de barras: " + e.getMessage());
        }
    }

    public static void imprimirXMLSAT() {
        try {
            if (!exigirConexao()) return;
            String nome = capturarEntrada("Nome do XML SAT (exemplo: XMLSAT.xml): ");
            String xml = lerArquivo(nome);
            if (xml == null) return;

            int retorno = impressora().ImprimeXMLSAT(xml, 0);
            informarRetorno("Imprimir XML SAT", retorno);
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao imprimir XML SAT: " + e.getMessage());
        }
    }

    public static void imprimirXMLCancelamentoSAT() {
        try {
            if (!exigirConexao()) return;
            String nome = capturarEntrada(
                    "Nome do XML de cancelamento (exemplo: CANC_SAT.xml): ");
            String xml = lerArquivo(nome);
            if (xml == null) return;

            int retorno = impressora().ImprimeXMLCancelamentoSAT(xml, ASS_QRCODE, 0);
            informarRetorno("Imprimir XML de cancelamento", retorno);
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao imprimir XML de cancelamento: " + e.getMessage());
        }
    }

    public static void abrirGaveta() {
        try {
            if (!exigirConexao()) return;

            System.out.println("A gaveta pode usar o pino 0 ou 1.");
            int pino = lerFaixa("Pino da gaveta (0 ou 1): ", 0, 1);
            int ti = lerFaixa("Tempo de acionamento ti (1 a 255): ", 1, 255);
            int tf = lerFaixa("Tempo entre acionamentos tf (1 a 255): ", 1, 255);

            int retorno = impressora().AbreGaveta(pino, ti, tf);
            if (retorno == 0) {
                System.out.println("Comando de abertura enviado. Se não abrir, confira o cabo da gaveta e teste o outro pino.");
            } else {
                System.out.println("Erro ao abrir a gaveta. Código: " + retorno);
                if (retorno == -121) System.out.println("Pino inválido: use 0 ou 1.");
                if (retorno == -122) System.out.println("Tempo inválido: use valores de 1 a 255.");
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao acionar a gaveta: " + e.getMessage());
        }
    }

    public static void sinalSonoro() {
        try {
            if (!exigirConexao()) return;
            int quantidade = lerFaixa("Quantos bips? (1 a 10): ", 1, 10);

            // Envia um bip por chamada, com pausa, para que a sequência seja perceptível.
            for (int i = 1; i <= quantidade; i++) {
                int retorno = impressora().SinalSonoro(1, 50, 5);
                if (retorno != 0) {
                    System.out.println("Erro no bip " + i + ". Código: " + retorno);
                    return;
                }
                System.out.println("Bip " + i + " de " + quantidade + ".");
                if (i < quantidade) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Sequência de bips interrompida.");
                        return;
                    }
                }
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha no sinal sonoro: " + e.getMessage());
        }
    }

    public static void avancarPapel() {
        try {
            if (!exigirConexao()) return;
            int linhas = lerFaixa("Quantas linhas deseja avançar? (1 a 100): ", 1, 100);
            int retorno = impressora().AvancaPapel(linhas);
            informarRetorno("Avançar papel", retorno);
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao avançar o papel: " + e.getMessage());
        }
    }

    public static void consultarStatus() {
        try {
            if (!exigirConexao()) return;

            System.out.println("\n1 = Gaveta | 2 = Tampa | 3 = Papel | 5 = Geral");
            int status = lerInteiro("Qual status deseja consultar? ");
            if (status != 1 && status != 2 && status != 3 && status != 5) {
                System.out.println("Opção inválida. Escolha 1, 2, 3 ou 5.");
                return;
            }

            int retorno = impressora().StatusImpressora(status);
            System.out.println("Retorno da consulta de status (" + status + "): " + retorno);

            if (retorno < 0) {
                System.out.println("A DLL retornou um código de erro. Verifique o modelo e a documentação.");
            } else {
                // O material do trabalho informa os parâmetros de consulta, mas não define
                // os valores numéricos retornados para cada estado. Por isso, exibimos o
                // retorno bruto em vez de atribuir um significado que pode estar incorreto.
                System.out.println("Consulta executada. O valor acima é o retorno bruto da DLL.");
            }
        } catch (Exception | LinkageError e) {
            System.out.println("Falha ao consultar status: " + e.getMessage());
        }
    }

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
        System.out.println("11 - Avançar papel");
        System.out.println("0  - Sair");
        System.out.println("========================================");
    }

    public static boolean fazerLogin() {
        try {
            for (int tentativa = 1; tentativa <= 3; tentativa++) {
                System.out.println("\n--- LOGIN ---");
                String usuario = capturarEntrada("Usuário: ");
                String senha = capturarEntrada("Senha: ");

                if (USUARIO.equals(usuario) && SENHA.equals(senha)) {
                    System.out.println("Login realizado com sucesso.");
                    return true;
                }

                int restantes = 3 - tentativa;
                if (restantes > 0) {
                    System.out.println("Usuário ou senha incorretos. Tentativas restantes: " + restantes);
                }
            }
            System.out.println("Número máximo de tentativas atingido. Programa encerrado.");
            return false;
        } catch (Exception e) {
            System.out.println("Não foi possível concluir o login: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("       SISTEMA DE IMPRESSORA ELGIN");
        System.out.println("========================================");

        if (!fazerLogin()) {
            scanner.close();
            return;
        }

        boolean executando = true;
        while (executando) {
            try {
                exibirMenu();
                int opcao = lerInteiro("Digite a opção desejada: ");

                switch (opcao) {
                    case 1: configurarConexao(); break;
                    case 2: abrirConexao(); break;
                    case 3: consultarStatus(); break;
                    case 4: imprimirTexto(); break;
                    case 5: imprimirQRCode(); break;
                    case 6: imprimirCodigoBarras(); break;
                    case 7: imprimirXMLSAT(); break;
                    case 8: imprimirXMLCancelamentoSAT(); break;
                    case 9: abrirGaveta(); break;
                    case 10: sinalSonoro(); break;
                    case 11: avancarPapel(); break;
                    case 0:
                        fecharConexao();
                        System.out.println("Programa encerrado.");
                        executando = false;
                        break;
                    default:
                        System.out.println("Opção inválida. Escolha uma opção do menu.");
                }
            } catch (Exception | LinkageError e) {
                System.out.println("A operação apresentou uma falha, mas o programa continuará.");
                System.out.println("Detalhes: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
