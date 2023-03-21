package edu.riccardomori.wordle.client.frontend.GUI.panels;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import edu.riccardomori.wordle.client.backend.ClientBackend;
import edu.riccardomori.wordle.client.backend.exceptions.GenericError;
import edu.riccardomori.wordle.client.backend.exceptions.ServerError;
import edu.riccardomori.wordle.client.backend.exceptions.UserTakenException;
import java.awt.Font;
import java.awt.GridBagConstraints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class RegistrationPanel extends JPanel {
    private JLabel errMsg;
    private JTextField usernameField;
    private JPasswordField passwordField;

    private ClientBackend backend;

    public RegistrationPanel(ClientBackend backend) {
        super();

        this.backend = backend;

        this.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Add the title
        JLabel title = new JLabel("Registration");
        title.setFont(new Font("NYTKarnakCondensed", Font.BOLD, 26));
        this.addComponent(title, gbc, 0, 0, 0, 0, GridBagConstraints.PAGE_START, 0, 0);

        // Add error message
        this.errMsg = new JLabel("Error");
        this.errMsg.setFont(new Font("Dialog", Font.PLAIN, 16));
        this.errMsg.setForeground(Color.RED);
        this.errMsg.setVisible(false);
        this.addComponent(this.errMsg, gbc, 0, 1, 0, 0, GridBagConstraints.CENTER, 0, 0);

        // Add the username label
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Ariel", Font.PLAIN, 18));
        this.addComponent(usernameLabel, gbc, 0, 2, 0, 0, GridBagConstraints.LINE_START, 0, 0,
                new Insets(30, 0, 0, 0));

        // Add the username field
        this.usernameField = new JTextField(25);
        this.addComponent(this.usernameField, gbc, 0, 3, 0, 1, GridBagConstraints.PAGE_START, 0, 10,
                new Insets(0, 0, 0, 0));

        // Add the password label
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Ariel", Font.PLAIN, 18));
        this.addComponent(passwordLabel, gbc, 0, 4, 0, 0, GridBagConstraints.LINE_START, 0, 0,
                new Insets(5, 0, 0, 0));

        // Add the password text input field
        this.passwordField = new JPasswordField(25);
        this.addComponent(this.passwordField, gbc, 0, 5, 0, 1, GridBagConstraints.PAGE_START, 0, 10,
                new Insets(0, 0, 0, 0));

        // Event listener
        this.passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    RegistrationPanel.this.register();
                }
            }
        });
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;

        this.add(component, gbc);
    }

    private void addComponent(JComponent component, GridBagConstraints gbc, int gridx, int gridy,
            double weightx, double weighty, int anchor, int ipadx, int ipady, Insets insets) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        gbc.anchor = anchor;
        gbc.insets = insets;

        this.add(component, gbc);
    }

    private void error(String message) {
        this.usernameField.setBorder(BorderFactory.createLineBorder(Color.RED));
        this.passwordField.setBorder(BorderFactory.createLineBorder(Color.RED));

        this.errMsg.setText(message);
        this.errMsg.setVisible(true);

        JDialog dialog = (JDialog) SwingUtilities.windowForComponent(this);
        dialog.pack();
    }

    public void register() {
        String username = this.usernameField.getText().trim();
        String password = new String(this.passwordField.getPassword()).trim();
        if (username.length() > 255 || password.length() > 255) {
            this.error("Wrong username or password.");
            return;
        }

        // Call the backend
        try {
            this.backend.register(username, password);

            JDialog dialog = (JDialog) SwingUtilities.windowForComponent(this);
            dialog.dispose();

        } catch (UserTakenException e) {
            this.error("Username already taken.");
        } catch (ServerError e) {
            this.error("The server is not responding. It might be offline");
        } catch (GenericError e) {
            this.error("An error happened while registering.");
        }
    }

    public static void popup(ClientBackend backend) {
        RegistrationPanel panel = new RegistrationPanel(backend);
        JComponent[] options = new JComponent[2];
        JButton registerButton = new JButton("Register");
        options[0] = registerButton;
        JButton cancelButton = new JButton("Cancel");
        options[1] = cancelButton;

        // Create popup
        JOptionPane popup = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, panel);
        final JDialog dialog = popup.createDialog("Registration");
        dialog.setAlwaysOnTop(true);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Event listeners
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.register();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }
}
