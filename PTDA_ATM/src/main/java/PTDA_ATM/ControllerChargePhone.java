package PTDA_ATM;

import SQL.Query;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Controlador para a funcionalidade de carregamento de saldo de telefone.
 */
public class ControllerChargePhone {

    /**
     * Botão para realizar o carregamento do telefone.
     */
    @FXML
    private Button buttonCharge;

    /**
     * Botão para voltar ao menu principal.
     */
    @FXML
    private Button buttonGoBack;

    /**
     * Rótulo para exibir mensagens de validação ou sucesso.
     */
    @FXML
    private Label labelValidacao;

    /**
     * Campo de texto para inserir o valor a ser carregado.
     */
    @FXML
    private TextField amount;

    /**
     * Campo de texto para inserir o número de telefone.
     */
    @FXML
    private TextField phoneNumber;

    /**
     * Número da conta do cliente.
     */
    private String clientAccountNumber;

    /**
     * Objeto para executar consultas no banco de dados.
     */
    private Query query = new Query();

    /**
     * Objeto para criar sombra para os botões
     */
    DropShadow shadow = new DropShadow();


    /**
     * Define o número da conta do cliente.
     *
     * @param clientAccountNumber Número da conta do cliente.
     */
    public void setClientAccountNumber(String clientAccountNumber) {
        this.clientAccountNumber = clientAccountNumber;
        initialize();
    }

    /**
     * Inicializa o controlador.
     */
    public void initialize() {
        phoneNumber.setOnKeyTyped(event -> clearValidationStyles());
        amount.setOnKeyTyped(event -> clearValidationStyles());

        buttonGoBack.setOnMouseEntered(e -> {
            buttonGoBack.setCursor(Cursor.HAND);
            buttonGoBack.setTranslateY(2);
            buttonGoBack.setEffect(shadow);
            buttonGoBack.setStyle("-fx-background-color: #761215; -fx-background-radius: 8;");
        });
        buttonGoBack.setOnMouseExited(e -> {
            buttonGoBack.setCursor(Cursor.DEFAULT);
            buttonGoBack.setTranslateY(0);
            buttonGoBack.setEffect(null);
            buttonGoBack.setStyle("-fx-background-color: #B01B1F; -fx-background-radius: 8;");
        });

        Reflection reflection = new Reflection();
        reflection.setFraction(0.5); // Ajusta o efeito de reflexão

        buttonCharge.setOnMouseEntered(e -> {
            buttonCharge.setCursor(Cursor.HAND);
            buttonCharge.setTranslateY(2); // Altere o valor conforme necessário
            buttonCharge.setEffect(reflection); // Adiciona reflexo ao passar o mouse
        });
        buttonCharge.setOnMouseExited(e -> {
            buttonCharge.setCursor(Cursor.DEFAULT);
            buttonCharge.setTranslateY(0); // Retorna à posição original
            buttonCharge.setEffect(null); // Remove o efeito de sombra ao passar o mouse
        });

    }

    /**
     * Realiza o carregamento do telefone.
     *
     * @param event O evento associado à ação.
     * @throws SQLException Exceção SQL.
     */
    public void chargePhone(ActionEvent event) throws SQLException {
        String phoneNumberInput = phoneNumber.getText();
        String chargeAmountInput = amount.getText();

        if (!validateInput(phoneNumberInput, chargeAmountInput)) {
            labelValidacao.setText("Invalid input. Check and try again.");
            applyValidationStyle();
        } else {
            float chargeAmount = Float.parseFloat(chargeAmountInput);

            float availableBalance = query.getAvailableBalance(clientAccountNumber);

            if (chargeAmount > 10000) {
                labelValidacao.setText("Charge amount exceeds the limit of 10000€");
                applyValidationStyle();
            } else if (chargeAmount > availableBalance) {
                labelValidacao.setText("Insufficient funds");
                applyValidationStyle();
            } else {
                // Restante da lógica permanece igual
                boolean success = performPhoneCharge(clientAccountNumber, phoneNumberInput, chargeAmount);

                if (success) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();

                    labelValidacao.setText(chargeAmountInput + "€ has been charged to " + phoneNumberInput + "!");
                    labelValidacao.setTextFill(Color.GREEN);

                    String recipientEmail = query.getClientEmail(clientAccountNumber);
                    String subject = "Transfer";
                    String message = "Subject: Phone Credit Notification\n" +
                            "Dear " + query.getClientName(clientAccountNumber) + ",\n" +
                            "We are pleased to inform you that a credit of " + chargeAmountInput + "€ has been successfully loaded onto the phone: " + phoneNumberInput + ". This credit was processed on " + formatter.format(now) + ".\n" +
                            "Should you have any questions or need further clarification, please do not hesitate to reach out to us. We are here to assist you.\n" +
                            "Best regards,\n" +
                            "ByteBank";
                    sendEmail(recipientEmail, subject, message);

                    PauseTransition pause = new PauseTransition(Duration.seconds(3));
                    pause.setOnFinished(events -> {
                        try {
                            switchToMenu(event);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    pause.play();
                } else {
                    showError("Phone Charge unsuccessful. Check the phone number and try again.");
                }
            }
        }
    }


    /**
     * Executa a lógica de carregamento do telefone.
     *
     * @param clientAccountNumber Número da conta do cliente.
     * @param phoneNumber      Número de telefone.
     * @param amount           Valor a ser carregado.
     * @return True se o carregamento for bem-sucedido, False caso contrário.
     * @throws SQLException Exceção SQL.
     */
    private boolean performPhoneCharge(String clientAccountNumber, String phoneNumber, float amount) throws SQLException {
        // Verifica se a conta tem fundos suficientes
        float sourceBalance = query.getAvailableBalance(clientAccountNumber);
        if (sourceBalance < amount) {
            return false; // Fundos insuficientes
        }

        // Logica para fazer o carregamento
        boolean debitSuccess = query.movementPhone(phoneNumber, clientAccountNumber, "Debit", amount, "Phone Charge");

        return debitSuccess;
    }

    /**
     * Alterna para a tela de menu principal.
     *
     * @param event O evento associado à ação.
     * @throws IOException Exceção de entrada/saída.
     */
    public void switchToMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Menu.fxml"));
        Parent root = loader.load();
        ControllerMenu menuController = loader.getController();
        String clientName = query.getClientName(clientAccountNumber);
        menuController.setClientName(clientName);
        menuController.setClientAccountNumber(clientAccountNumber);
        Stage stage = (Stage) buttonGoBack.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Valida a entrada do usuário.
     *
     * @param phoneNumber Número de telefone.
     * @param amount      Valor a ser carregado.
     * @return True se a entrada for válida, False caso contrário.
     */
    protected boolean validateInput(String phoneNumber, String amount) {
        // Validate if the phone number exists and the amount is a valid float
        if (!phoneNumber.matches("^\\d{1,9}$")) {
            return false; // Phone number should have up to 9 digits
        }

        if (!amount.matches("^\\d+(\\.\\d+)?$")) {
            return false; // Amount should be a valid float
        }
        return true;
    }

    /**
     * Envia um email.
     *
     * @param recipientEmail Endereço de email do destinatário.
     * @param subject        Assunto do email.
     * @param text           Corpo do email.
     */
    private void sendEmail(String recipientEmail, String subject, String text) {
        final String username = "projetoptda@gmail.com";
        final String password = "gcue jaff wcib cklg";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(text);

            Transport.send(message);

            System.out.println("Email enviado com sucesso!");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Exibe uma mensagem de erro usando um Alert.
     *
     * @param message Mensagem de erro a ser exibida.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Mostrar o Alert
        alert.showAndWait();
    }

    /**
     * Aplica o estilo de validação com borda vermelha.
     */
    private void applyValidationStyle() {
        labelValidacao.setTextFill(Color.RED);
        Border border = new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(6), BorderWidths.DEFAULT));
        phoneNumber.setBorder(border);
        amount.setBorder(border);
    }

    /**
     * Limpa os estilos de validação.
     */
    private void clearValidationStyles() {
        labelValidacao.setText("");
        phoneNumber.setBorder(null);
        amount.setBorder(null);
    }
}
